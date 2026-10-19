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
 * Generates the side elements and the front and back faces of a small stair-step sprite with vanilla's real
 * {@link ItemModelGenerator} (our mixin applied) and bakes them with vanilla's {@link FaceBakery}, then compares the quads
 * against plain vanilla.
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
	private static Method processFrames;

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
		processFrames = ItemModelGenerator.class.getDeclaredMethod("processFrames", int.class, String.class, SpriteContents.class);
		processFrames.setAccessible(true);
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
	void disabledSettingsGiveVanillaQuads() throws Exception {
		List<BakedQuad> vanilla = bakeAll(generate(GapFix.vanilla()));
		GapFix.Settings disabled = new GapFix.Settings(false, 1.0F, 0.05F, 0.05F, false);
		List<BakedQuad> quads = bakeAll(generate(disabled));
		assertEquals(vanilla.size(), quads.size());
		for (int i = 0; i < vanilla.size(); i++) {
			assertSameQuad(vanilla.get(i), quads.get(i), disabled + " quad " + i);
		}
		BlockElement layer = frames(disabled).get(0);
		BlockElement vanillaLayer = frames(GapFix.vanilla()).get(0);
		assertBits(vanillaLayer.from, layer.from);
		assertBits(vanillaLayer.to, layer.to);
	}

	@Test
	void zeroAdjustmentOnlyAppliesTheEdgeGuard() throws Exception {
		assertOnlyCornersMove(0.0F, 0.0F);
	}

	@Test
	void sideElementsOnTheSpriteBorderMeetTheGuardedFrontFace() throws Exception {
		float guard = GapFix.EDGE_GUARD_TEXELS * 16.0F / SIZE;
		int border = 0;
		for (BlockElement element : generate(new GapFix.Settings(true, 0.0F, 0.0F, 0.0F, false))) {
			Direction face = face(element);
			float plane = face == Direction.UP || face == Direction.DOWN ? element.from.y : element.from.x;
			if (Math.abs(plane - guard) < 1.0E-5F || Math.abs(plane - (16.0F - guard)) < 1.0E-5F) {
				border++;
			} else {
				assertTrue(plane > guard && plane < 16.0F - guard, face + " plane " + plane + " lies between guard and border");
			}
		}
		// The test sprite touches all four borders: left column (EAST), top right (WEST and UP), bottom row (DOWN).
		assertTrue(border >= 4, "border side elements: " + border);
	}

	@Test
	void frontAndBackFacesStayOffTheSpriteBorderWithAnExactTexelMapping() throws Exception {
		// AUTO on Windows and Linux: no UV shrink, so the baked UVs show the model's mapping directly. Without the edge
		// guard the corners sample exactly on the sprite border, where a pixel can pick up the neighbouring sprite.
		GapFix.Settings settings = new GapFix.Settings(true, 0.0F, 0.005F, 0.0001F, false);
		BlockElement layer = frames(settings).get(0);
		float guard = GapFix.EDGE_GUARD_TEXELS * 16.0F / SIZE;
		assertEquals(guard, layer.from.x, 1.0E-6F);
		assertEquals(guard, layer.from.y, 1.0E-6F);
		assertEquals(16.0F - guard, layer.to.x, 1.0E-5F);
		assertEquals(16.0F - guard, layer.to.y, 1.0E-5F);
		float atlasPerModel = (sprite.getU1() - sprite.getU0()) / 16.0F;
		for (Direction direction : new Direction[]{Direction.SOUTH, Direction.NORTH}) {
			BlockElementFace face = layer.faces.get(direction);
			BakedQuad quad = new FaceBakery().bakeQuad(layer.from, layer.to, face, sprite, direction, IDENTITY, null, true);
			int[] vertices = quad.getVertices();
			for (int v = 0; v < 4; v++) {
				float x = Float.intBitsToFloat(vertices[v * 8]) * 16.0F;
				float y = Float.intBitsToFloat(vertices[v * 8 + 1]) * 16.0F;
				float u = (Float.intBitsToFloat(vertices[v * 8 + 4]) - sprite.getU0()) / atlasPerModel;
				float vv = (Float.intBitsToFloat(vertices[v * 8 + 5]) - sprite.getV0()) / atlasPerModel;
				// Vanilla stores the back face's u mirrored, which cancels the north face's own mirror: u = x on both.
				assertEquals(x, u, 1.0E-3F, direction + " vertex " + v + " u");
				assertEquals(16.0F - y, vv, 1.0E-3F, direction + " vertex " + v + " v");
				assertTrue(u >= guard * 0.5F && u <= 16.0F - guard * 0.5F, direction + " vertex " + v + " u " + u + " stays off the border");
				assertTrue(vv >= guard * 0.5F && vv <= 16.0F - guard * 0.5F, direction + " vertex " + v + " v " + vv + " stays off the border");
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
	 * Each adjusted quad must face the same way, keep vertex order, keep its UVs along the edge, move its UVs across the
	 * thickness inwards by the side thickness inset, move {@code indent} (plus the edge guard on the sprite border)
	 * against its baked normal and grow {@code expansion} away from its centre on both in-plane axes. Values are in model
	 * units.
	 */
	private void assertOnlyCornersMove(float expansion, float indent) throws Exception {
		List<BlockElement> vanillaElements = generate(GapFix.vanilla());
		List<BakedQuad> vanilla = bakeAll(vanillaElements);
		List<BakedQuad> adjusted = bakeAll(generate(new GapFix.Settings(true, 1.0F, expansion, indent, false)));
		assertEquals(vanilla.size(), adjusted.size());
		float guard = GapFix.EDGE_GUARD_TEXELS * 16.0F / SIZE;
		float inset = GapFix.SIDE_THICKNESS_INSET_TEXELS * 16.0F / SIZE;
		// Baked UVs: vanilla's shrink pulls the UVs towards the face's UV centre by its ratio, then the sprite maps model
		// units to atlas units.
		float bakedInset = inset * (1.0F - sprite.uvShrinkRatio()) * (sprite.getU1() - sprite.getU0()) / 16.0F;
		for (int q = 0; q < vanilla.size(); q++) {
			BakedQuad a = vanilla.get(q);
			BakedQuad b = adjusted.get(q);
			Direction key = face(vanillaElements.get(q));
			String what = "e=" + expansion + " i=" + indent + " " + key + " quad " + q;
			assertEquals(a.getDirection(), b.getDirection(), what + " facing");
			assertEquals(a.getDirection(), geometricFacing(b), what + " winding");
			int[] va = a.getVertices();
			int[] vb = b.getVertices();
			float normalMove = indent + (GapFix.isOnSpriteBorder(key, vanillaElements.get(q).from) ? guard : 0.0F);
			// UP and DOWN faces span the thickness on v (vertex int 5), EAST and WEST on u (vertex int 4).
			int thick = key == Direction.UP || key == Direction.DOWN ? 5 : 4;
			int along = thick == 5 ? 4 : 5;
			float thickCentre = 0.0F;
			for (int v = 0; v < 4; v++) {
				thickCentre += Float.intBitsToFloat(va[v * 8 + thick]) / 4.0F;
			}
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
				assertEquals(va[o + along], vb[o + along], what + " vertex " + v + " UV along the edge");
				float thickBefore = Float.intBitsToFloat(va[o + thick]);
				float thickExpected = thickBefore + Math.signum(thickCentre - thickBefore) * bakedInset;
				assertEquals(thickExpected, Float.intBitsToFloat(vb[o + thick]), 1.0E-6F, what + " vertex " + v + " UV across the thickness");
				for (int axis = 0; axis < 3; axis++) {
					float before = Float.intBitsToFloat(va[o + axis]);
					float after = Float.intBitsToFloat(vb[o + axis]);
					float expected;
					if (axis == normalAxis) {
						expected = before - normalSign * normalMove / 16.0F;
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

	private static List<BlockElement> frames(GapFix.Settings settings) throws Exception {
		GapFix.apply(settings);
		@SuppressWarnings("unchecked")
		List<BlockElement> elements = (List<BlockElement>) processFrames.invoke(new ItemModelGenerator(), 0, "layer0", contents);
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
