package navrelis.glintsmith.modelfix;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.ItemModelGenerator;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.Util;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceMetadata;
import net.minecraft.world.inventory.InventoryMenu;
import org.joml.Vector3f;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Generates the side elements of a small stair-step sprite with vanilla's real {@link ItemModelGenerator} (our mixin
 * applied) and bakes them with vanilla's {@link FaceBakery}, then compares the quads against plain vanilla.
 */
class SideElementBakeTest {
	/** Opaque pixels, row by row from the top: a two pixel tall left edge and a diagonal staircase. */
	private static final String[] SPRITE = {
		"....#",
		"...##",
		"..##.",
		"###..",
		"##...",
	};
	private static final int SIZE = SPRITE.length;
	private static final ModelState IDENTITY = new ModelState() {
	};
	/** Block units; a bake goes through float maths at magnitude 1, so exact equality is not possible. */
	private static final float POS_EPS = 2.0E-7F;

	private static NativeImage image;
	private static SpriteContents contents;
	private static TextureAtlasSprite sprite;
	private static Method createSideElements;

	private final GapFix.Settings saved = GapFix.current();

	@BeforeAll
	static void setUp() throws Exception {
		image = new NativeImage(SIZE, SIZE, true);
		for (int y = 0; y < SIZE; y++) {
			for (int x = 0; x < SIZE; x++) {
				image.setPixelRGBA(x, y, SPRITE[y].charAt(x) == '#' ? 0xFF3050A0 : 0);
			}
		}
		contents = new SpriteContents(ResourceLocation.fromNamespaceAndPath("glintsmith", "test_stairs"), new FrameSize(SIZE, SIZE), image,
			ResourceMetadata.EMPTY);
		sprite = new TextureAtlasSprite(InventoryMenu.BLOCK_ATLAS, contents, 64, 64, 16, 8) {
		};
		createSideElements = ItemModelGenerator.class.getDeclaredMethod("createSideElements", SpriteContents.class, String.class, int.class);
		createSideElements.setAccessible(true);
	}

	@AfterAll
	static void tearDown() {
		image.close();
	}

	@AfterEach
	void restore() {
		GapFix.apply(saved);
		assertFalse(GapFix.hasFailed(), "a gap fix handler threw");
	}

	@Test
	void vanillaBakesLeftAndRightSpansOutwards() throws Exception {
		List<BlockElement> elements = generate(GapFix.vanilla());
		boolean sawEast = false;
		boolean sawWest = false;
		for (BlockElement element : elements) {
			Direction key = face(element);
			Direction baked = bake(element).getDirection();
			switch (key) {
				case EAST -> {
					// SpanFacing.LEFT: the left edge of a pixel column, stored with from.y > to.y, so it faces west.
					sawEast = true;
					assertTrue(element.from.y > element.to.y, "LEFT spans are stored upside down");
					assertEquals(Direction.WEST, baked);
				}
				case WEST -> {
					sawWest = true;
					assertTrue(element.from.y > element.to.y, "RIGHT spans are stored upside down");
					assertEquals(Direction.EAST, baked);
				}
				default -> assertEquals(key, baked);
			}
		}
		assertTrue(sawEast && sawWest, "sprite must produce left and right spans");
	}

	@Test
	void noOpSettingsGiveVanillaQuads() throws Exception {
		List<BakedQuad> vanilla = bakeAll(generate(GapFix.vanilla()));
		GapFix.Settings[] noOps = {
			new GapFix.Settings(true, 1.0F, 0.0F, 0.0F, false),
			new GapFix.Settings(false, 1.0F, 0.05F, 0.05F, false),
		};
		for (GapFix.Settings settings : noOps) {
			List<BakedQuad> quads = bakeAll(generate(settings));
			assertEquals(vanilla.size(), quads.size());
			for (int i = 0; i < vanilla.size(); i++) {
				assertSameQuad(vanilla.get(i), quads.get(i), settings + " quad " + i);
			}
		}
	}

	@Test
	void zeroAdjustmentKeepsVanillaElementsBitIdentical() throws Exception {
		for (BlockElement element : generate(GapFix.vanilla())) {
			Vector3f from = new Vector3f(element.from);
			Vector3f to = new Vector3f(element.to);
			GapFix.adjustSideElement(face(element), from, to, 0.0F, 0.0F);
			assertBits(element.from, from);
			assertBits(element.to, to);
		}
	}

	@Test
	void indentAndExpansionOnlyMoveCorners() throws Exception {
		assertOnlyCornersMove(0.05F, 0.025F);
		assertOnlyCornersMove(0.0F, 0.04F);
		assertOnlyCornersMove(0.03F, 0.0F);
		assertOnlyCornersMove(-0.02F, 0.01F);
	}

	@Test
	void autoGrowsSideQuadsAndIndentsWithoutFlippingCorners() throws Exception {
		GapFix.Settings auto = GapFix.autoSettings();
		if (Util.getPlatform() != Util.OS.OSX) {
			assertEquals(0.005F, auto.sideExpansion());
		}
		// Per quad: same facing and winding as vanilla (no corner flip), same vertex order and UVs, exactly indent along
		// the normal and exactly expansion away from the centre on both in-plane axes; nothing else changes.
		assertOnlyCornersMove(auto.sideExpansion(), auto.sideIndent());
	}

	/**
	 * Each adjusted quad must face the same way, keep vertex order and per-vertex UVs, move {@code indent} against its
	 * baked normal and grow {@code expansion} away from its centre on both in-plane axes. Values are in model units.
	 */
	private void assertOnlyCornersMove(float expansion, float indent) throws Exception {
		List<BlockElement> vanillaElements = generate(GapFix.vanilla());
		List<BakedQuad> vanilla = bakeAll(vanillaElements);
		List<BakedQuad> adjusted = bakeAll(generate(new GapFix.Settings(true, 1.0F, expansion, indent, false)));
		assertEquals(vanilla.size(), adjusted.size());
		for (int q = 0; q < vanilla.size(); q++) {
			BakedQuad a = vanilla.get(q);
			BakedQuad b = adjusted.get(q);
			String what = "e=" + expansion + " i=" + indent + " " + face(vanillaElements.get(q)) + " quad " + q;
			assertEquals(a.getDirection(), b.getDirection(), what + " facing");
			assertEquals(a.getDirection(), geometricFacing(b), what + " winding");
			int[] va = a.getVertices();
			int[] vb = b.getVertices();
			int normalAxis = a.getDirection().getAxis().ordinal();
			int normalSign = a.getDirection().getAxisDirection().getStep();
			float[] centre = new float[3];
			for (int v = 0; v < 4; v++) {
				for (int axis = 0; axis < 3; axis++) {
					centre[axis] += Float.intBitsToFloat(va[v * 8 + axis]) / 4.0F;
				}
			}
			for (int v = 0; v < 4; v++) {
				int o = v * 8;
				assertEquals(va[o + 4], vb[o + 4], what + " vertex " + v + " u");
				assertEquals(va[o + 5], vb[o + 5], what + " vertex " + v + " v");
				for (int axis = 0; axis < 3; axis++) {
					float before = Float.intBitsToFloat(va[o + axis]);
					float after = Float.intBitsToFloat(vb[o + axis]);
					float expected;
					if (axis == normalAxis) {
						expected = before - normalSign * indent / 16.0F;
					} else {
						expected = before + Math.signum(before - centre[axis]) * expansion / 16.0F;
					}
					assertEquals(expected, after, POS_EPS, what + " vertex " + v + " axis " + axis);
				}
			}
		}
	}

	private static List<BlockElement> generate(GapFix.Settings settings) throws Exception {
		GapFix.apply(settings);
		@SuppressWarnings("unchecked")
		List<BlockElement> elements = (List<BlockElement>) createSideElements.invoke(new ItemModelGenerator(), contents, "layer0", 0);
		return elements;
	}

	private static List<BakedQuad> bakeAll(List<BlockElement> elements) {
		List<BakedQuad> quads = new ArrayList<>();
		for (BlockElement element : elements) {
			quads.add(bake(element));
		}
		return quads;
	}

	private static BakedQuad bake(BlockElement element) {
		Map.Entry<Direction, BlockElementFace> face = element.faces.entrySet().iterator().next();
		return new FaceBakery().bakeQuad(element.from, element.to, face.getValue(), sprite, face.getKey(), IDENTITY, element.rotation, element.shade);
	}

	private static Direction face(BlockElement element) {
		assertEquals(1, element.faces.size());
		return element.faces.keySet().iterator().next();
	}

	/** The facing of the quad's actual winding, the way {@code FaceBakery.calculateFacing} sees it. */
	private static Direction geometricFacing(BakedQuad quad) {
		return FaceBakery.calculateFacing(quad.getVertices());
	}

	private static void assertSameQuad(BakedQuad expected, BakedQuad actual, String what) {
		assertEquals(expected.getDirection(), actual.getDirection(), what + " facing");
		assertEquals(expected.getTintIndex(), actual.getTintIndex(), what + " tint");
		assertArrayEquals(expected.getVertices(), actual.getVertices(), what + " vertices");
	}

	private static void assertBits(Vector3f expected, Vector3f actual) {
		assertEquals(Float.floatToRawIntBits(expected.x), Float.floatToRawIntBits(actual.x), "x of " + expected);
		assertEquals(Float.floatToRawIntBits(expected.y), Float.floatToRawIntBits(actual.y), "y of " + expected);
		assertEquals(Float.floatToRawIntBits(expected.z), Float.floatToRawIntBits(actual.z), "z of " + expected);
	}
}
