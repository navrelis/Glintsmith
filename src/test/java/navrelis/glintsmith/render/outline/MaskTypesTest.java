package navrelis.glintsmith.render.outline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.Optional;
import java.util.function.Supplier;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import navrelis.glintsmith.mixin.outline.CompositeRenderTypeAccessor;
import navrelis.glintsmith.mixin.outline.CompositeStateAccessor;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Uses real vanilla render types; their construction needs no GL context, only bootstrapped registries. The
 * accessors used by {@link MaskTypes} only work when the outline mixins are applied, so these tests also cover A1 to
 * A3 at run time.
 */
class MaskTypesTest {
	private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/zombie/zombie.png");

	@BeforeAll
	static void bootstrap() {
		GameBootstrap.ensure();
	}

	@BeforeEach
	void clear() {
		MaskTypes.clearCaches();
	}

	@Test
	void vanillaGlintTypesAreNeverMasked() {
		assertNull(MaskTypes.forSource(RenderType.glint(), true));
		assertNull(MaskTypes.forSource(RenderType.glintTranslucent(), true));
		assertNull(MaskTypes.forSource(RenderType.entityGlint(), true));
		assertNull(MaskTypes.forSource(RenderType.entityGlintDirect(), true));
		assertNull(MaskTypes.forSource(RenderType.armorEntityGlint(), false));
	}

	@Test
	void anyTypeWithGlintStateOrNameIsExcluded() {
		assertTrue(MaskTypes.isGlint(RenderStateShard.GLINT_TRANSPARENCY, RenderStateShard.DEFAULT_TEXTURING, "custom"));
		assertTrue(MaskTypes.isGlint(RenderStateShard.TRANSLUCENT_TRANSPARENCY, RenderStateShard.GLINT_TEXTURING, "custom"));
		assertTrue(MaskTypes.isGlint(RenderStateShard.TRANSLUCENT_TRANSPARENCY, RenderStateShard.ENTITY_GLINT_TEXTURING, "custom"));
		assertTrue(MaskTypes.isGlint(RenderStateShard.NO_TRANSPARENCY, RenderStateShard.DEFAULT_TEXTURING, "mymod_glint_slot"));
		assertFalse(MaskTypes.isGlint(RenderStateShard.NO_TRANSPARENCY, RenderStateShard.DEFAULT_TEXTURING, "entity_cutout"));
		assertEquals("entity_cutout", MaskTypes.nameOf("RenderType[entity_cutout:CompositeState[[texture[...]]]]"));
		assertEquals("odd", MaskTypes.nameOf("odd"));
		assertEquals("", MaskTypes.nameOf(null));
	}

	@Test
	void cutoutAndTranslucentEntityTypesAreMaskedWithTheirTexture() {
		RenderType cutout = RenderType.entityCutoutNoCull(TEXTURE);
		RenderType translucent = RenderType.entityTranslucent(TEXTURE);
		MaskTypes.Entry cutoutEntry = MaskTypes.lookup(cutout);
		MaskTypes.Entry translucentEntry = MaskTypes.lookup(translucent);
		assertTrue(cutoutEntry.maskable());
		assertTrue(translucentEntry.maskable());
		assertEquals(TEXTURE, cutoutEntry.texture);
		assertEquals(TEXTURE, translucentEntry.texture);
		assertFalse(MaskTypes.needsDefaultUv(cutout));

		RenderType mask = MaskTypes.forSource(cutout, true);
		assertNotNull(mask);
		assertEquals(DefaultVertexFormat.POSITION_TEX_COLOR, mask.format());
		assertEquals(cutout.mode(), mask.mode());
		assertTrue(mask.toString().contains(MaskTypes.MASK_NAME));
		// Separate types per source (no interleaving in one mask buffer) and per depth mode.
		assertNotSame(mask, MaskTypes.forSource(translucent, true));
		assertNotSame(mask, MaskTypes.forSource(cutout, false));
		assertSame(mask, MaskTypes.forSource(cutout, true));
		assertTrue(mask.toString().contains("<="), "depth-tested mask uses LEQUAL");
		assertTrue(MaskTypes.forSource(cutout, false).toString().contains("always"), "GUI mask has no depth test");
	}

	@Test
	void depthTestedMaskKeepsTheItemDepthWithTheSourceLayering() {
		RenderType cutout = RenderType.entityCutoutNoCull(TEXTURE);
		RenderType armor = RenderType.armorCutoutNoCull(TEXTURE); // VIEW_OFFSET_Z_LAYERING in vanilla
		RenderType item = RenderType.itemEntityTranslucentCull(TEXTURE);

		// Depth-tested masks write their own depth (nearest item surface) and never use a polygon offset of their
		// own: the offset let item pixels just behind the arm pass as seeds.
		CompositeStateAccessor cutoutMask = stateOf(MaskTypes.forSource(cutout, true));
		assertSame(RenderStateShard.COLOR_DEPTH_WRITE, cutoutMask.glintsmith$outlineWriteMaskState());
		assertSame(RenderStateShard.NO_LAYERING, cutoutMask.glintsmith$outlineLayeringState());
		assertSame(RenderStateShard.NO_LAYERING, stateOf(MaskTypes.forSource(item, true)).glintsmith$outlineLayeringState());

		// The armor's view offset is repeated, so the mask depth equals the armor depth in the scene.
		CompositeStateAccessor armorMask = stateOf(MaskTypes.forSource(armor, true));
		assertSame(RenderStateShard.VIEW_OFFSET_Z_LAYERING, armorMask.glintsmith$outlineLayeringState());
		assertSame(RenderStateShard.COLOR_DEPTH_WRITE, armorMask.glintsmith$outlineWriteMaskState());

		// The flat GUI mask: no depth test, no depth write, no layering.
		CompositeStateAccessor flatArmor = stateOf(MaskTypes.forSource(armor, false));
		assertSame(RenderStateShard.COLOR_WRITE, flatArmor.glintsmith$outlineWriteMaskState());
		assertSame(RenderStateShard.NO_LAYERING, flatArmor.glintsmith$outlineLayeringState());
	}

	@Test
	void armAndSleeveTypesGetADepthOnlyOccluder() {
		// PlayerRenderer.renderHand: arm entitySolid(skin), sleeve entityTranslucent(skin); both write depth.
		RenderType arm = RenderType.entitySolid(TEXTURE);
		RenderType sleeve = RenderType.entityTranslucent(TEXTURE);
		RenderType occluder = MaskTypes.occluderFor(arm);
		assertNotNull(occluder);
		assertNotNull(MaskTypes.occluderFor(sleeve));
		assertSame(occluder, MaskTypes.occluderFor(arm), "built once per source");
		assertNotSame(occluder, MaskTypes.occluderFor(sleeve));
		assertNotSame(occluder, MaskTypes.forSource(arm, true), "never shared with the item mask type");
		assertTrue(occluder.toString().contains(MaskTypes.OCCLUDER_NAME));
		assertEquals(DefaultVertexFormat.POSITION_TEX_COLOR, occluder.format());

		// Depth only, LEQUAL against the mask depth, with the source's layering (armor worn on the arm).
		CompositeStateAccessor state = stateOf(occluder);
		assertSame(RenderStateShard.DEPTH_WRITE, state.glintsmith$outlineWriteMaskState());
		assertSame(RenderStateShard.LEQUAL_DEPTH_TEST, state.glintsmith$outlineDepthTestState());
		CompositeStateAccessor armor = stateOf(MaskTypes.occluderFor(RenderType.armorCutoutNoCull(TEXTURE)));
		assertSame(RenderStateShard.VIEW_OFFSET_Z_LAYERING, armor.glintsmith$outlineLayeringState());
	}

	@Test
	void typesThatWriteNoDepthAndUnmaskableTypesOccludeNothing() {
		assertNull(MaskTypes.occluderFor(RenderType.eyes(TEXTURE)), "glowing eyes write colour only");
		assertNull(MaskTypes.occluderFor(RenderType.entityGlint()));
		assertNull(MaskTypes.occluderFor(RenderType.waterMask()));
		assertNull(MaskTypes.occluderFor(RenderType.lines()));
		assertTrue(MaskTypes.lookup(RenderType.eyes(TEXTURE)).maskable(), "still outlined as an item would be");

		assertTrue(MaskTypes.writesDepth(RenderStateShard.COLOR_DEPTH_WRITE, RenderStateShard.LEQUAL_DEPTH_TEST));
		assertTrue(MaskTypes.writesDepth(RenderStateShard.COLOR_DEPTH_WRITE, RenderStateShard.EQUAL_DEPTH_TEST));
		assertFalse(MaskTypes.writesDepth(RenderStateShard.COLOR_WRITE, RenderStateShard.LEQUAL_DEPTH_TEST));
		assertFalse(MaskTypes.writesDepth(RenderStateShard.COLOR_DEPTH_WRITE, RenderStateShard.NO_DEPTH_TEST));
	}

	@Test
	void depthTestedMaskAndOccluderRepeatTheSourceCulling() {
		// Vanilla's item sheets (itemEntityTranslucentCull, entitySolid, entityCutout) cull back faces: drawn
		// double-sided, the back quad and the inner side quads of a generated item seeded stray 1-px outline rings.
		RenderType item = RenderType.itemEntityTranslucentCull(TEXTURE);
		RenderType solid = RenderType.entitySolid(TEXTURE);
		RenderType cutoutNoCull = RenderType.entityCutoutNoCull(TEXTURE);
		RenderType sleeve = RenderType.entityTranslucent(TEXTURE); // NO_CULL in vanilla
		assertSame(RenderStateShard.CULL, MaskTypes.lookup(item).cull);
		assertSame(RenderStateShard.NO_CULL, MaskTypes.lookup(cutoutNoCull).cull);

		assertSame(RenderStateShard.CULL, stateOf(MaskTypes.forSource(item, true)).glintsmith$outlineCullState());
		assertSame(RenderStateShard.CULL, stateOf(MaskTypes.forSource(solid, true)).glintsmith$outlineCullState());
		assertSame(RenderStateShard.CULL, stateOf(MaskTypes.forSource(RenderType.entityCutout(TEXTURE), true)).glintsmith$outlineCullState());
		assertSame(RenderStateShard.NO_CULL, stateOf(MaskTypes.forSource(cutoutNoCull, true)).glintsmith$outlineCullState(),
			"double-sided sources stay double-sided");

		// The flat GUI mask draws both sides whatever the source does.
		assertSame(RenderStateShard.NO_CULL, stateOf(MaskTypes.forSource(item, false)).glintsmith$outlineCullState());
		assertSame(RenderStateShard.NO_CULL, stateOf(MaskTypes.forSource(solid, false)).glintsmith$outlineCullState());

		// Occluders hide exactly what the source's own faces hide: the arm culls, the sleeve does not.
		assertSame(RenderStateShard.CULL, stateOf(MaskTypes.occluderFor(solid)).glintsmith$outlineCullState());
		assertSame(RenderStateShard.CULL, stateOf(MaskTypes.occluderFor(item)).glintsmith$outlineCullState());
		assertSame(RenderStateShard.NO_CULL, stateOf(MaskTypes.occluderFor(sleeve)).glintsmith$outlineCullState());
	}

	@Test
	void unreadableCullStateFallsBackToDoubleSidedMasks() {
		CompositeStateAccessor missing = accessorWithCull(() -> {
			throw new AbstractMethodError("accessor not applied");
		});
		assertSame(RenderStateShard.NO_CULL, MaskTypes.cullOf(missing));
		assertSame(RenderStateShard.NO_CULL, MaskTypes.cullOf(accessorWithCull(() -> {
			throw new IllegalStateException("broken");
		})));
		assertSame(RenderStateShard.NO_CULL, MaskTypes.cullOf(accessorWithCull(() -> null)));
		assertSame(RenderStateShard.CULL, MaskTypes.cullOf(accessorWithCull(() -> RenderStateShard.CULL)));

		// An entry without a cull state still builds its types, double-sided as before.
		MaskTypes.Entry entry = new MaskTypes.Entry(TEXTURE, VertexFormat.Mode.QUADS, false, null, null, true);
		assertSame(RenderStateShard.NO_CULL, entry.cull);
		assertSame(RenderStateShard.NO_CULL, stateOf(entry.maskType(true)).glintsmith$outlineCullState());
		assertSame(RenderStateShard.NO_CULL, stateOf(entry.occluderType()).glintsmith$outlineCullState());
		assertSame(RenderStateShard.NO_CULL, MaskTypes.Entry.NOT_MASKABLE.cull);
	}

	/** An accessor whose cull state comes from {@code cull}; every other shard is null. */
	private static CompositeStateAccessor accessorWithCull(Supplier<RenderStateShard.CullStateShard> cull) {
		return (CompositeStateAccessor) Proxy.newProxyInstance(CompositeStateAccessor.class.getClassLoader(),
			new Class<?>[] {CompositeStateAccessor.class}, (proxy, method, args) -> {
				if (method.getName().equals("glintsmith$outlineCullState")) {
					return cull.get();
				}
				return null;
			});
	}

	private static CompositeStateAccessor stateOf(RenderType type) {
		assertNotNull(type);
		RenderType.CompositeState state = ((CompositeRenderTypeAccessor) (Object) type).glintsmith$outlineState();
		return (CompositeStateAccessor) (Object) state;
	}

	@Test
	void blockAtlasItemTypesAreMasked() {
		assertTrue(MaskTypes.lookup(RenderType.cutout()).maskable());
		assertTrue(MaskTypes.lookup(RenderType.translucent()).maskable());
	}

	@Test
	void untexturedFilledTypesUseTheWhiteTexture() {
		RenderType gui = RenderType.gui(); // POSITION_COLOR quads
		MaskTypes.Entry entry = MaskTypes.lookup(gui);
		assertTrue(entry.maskable());
		assertEquals(MaskTypes.WHITE, entry.texture);
		assertTrue(MaskTypes.needsDefaultUv(gui));
	}

	@Test
	void linesOutlinesAndInvisibleTypesAreNotMasked() {
		assertNull(MaskTypes.forSource(RenderType.lines(), true));
		assertNull(MaskTypes.forSource(RenderType.outline(TEXTURE), true));
		assertNull(MaskTypes.forSource(RenderType.waterMask(), true));
		assertNull(MaskTypes.forSource(RenderType.leash(), true));
	}

	@Test
	void eligibilityRules() {
		assertTrue(MaskTypes.shapeEligible(false, VertexFormat.Mode.QUADS, true));
		assertTrue(MaskTypes.shapeEligible(false, VertexFormat.Mode.TRIANGLES, true));
		assertFalse(MaskTypes.shapeEligible(true, VertexFormat.Mode.QUADS, true));
		assertFalse(MaskTypes.shapeEligible(false, VertexFormat.Mode.LINES, true));
		assertFalse(MaskTypes.shapeEligible(false, VertexFormat.Mode.TRIANGLE_STRIP, true));
		assertFalse(MaskTypes.shapeEligible(false, VertexFormat.Mode.QUADS, false));
		assertEquals(TEXTURE, MaskTypes.textureFor(true, Optional.of(TEXTURE)));
		assertNull(MaskTypes.textureFor(true, Optional.empty()), "textured format with unknown texture");
		assertEquals(MaskTypes.WHITE, MaskTypes.textureFor(false, Optional.empty()));
	}

	@Test
	void decisionsAreCachedByIdentity() {
		RenderType type = RenderType.entitySolid(TEXTURE);
		MaskTypes.Entry first = MaskTypes.lookup(type);
		assertSame(first, MaskTypes.lookup(type));
		assertEquals(1, MaskTypes.cachedSourceCount());
		MaskTypes.lookup(RenderType.glint());
		assertEquals(2, MaskTypes.cachedSourceCount());
		assertSame(MaskTypes.Entry.NOT_MASKABLE, MaskTypes.lookup(RenderType.glint()));
		MaskTypes.clearCaches();
		assertEquals(0, MaskTypes.cachedSourceCount());
		assertNotSame(first, MaskTypes.lookup(type));
	}

	@Test
	void cacheIsBounded() {
		for (int i = 0; i < MaskTypes.MAX_CACHED_SOURCES + 10; i++) {
			MaskTypes.lookup(RenderType.entityCutout(ResourceLocation.fromNamespaceAndPath("glintsmith", "test/" + i + ".png")));
		}
		assertTrue(MaskTypes.cachedSourceCount() <= MaskTypes.MAX_CACHED_SOURCES);
	}
}
