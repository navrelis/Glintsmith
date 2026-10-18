package navrelis.glintsmith.render.glint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.minecraft.resources.ResourceLocation;

class GlintParamsTest {
	private static final ResourceLocation A = ResourceLocation.fromNamespaceAndPath("glintsmith", "glint/preset/a");
	private static final ResourceLocation B = ResourceLocation.fromNamespaceAndPath("glintsmith", "glint/preset/b");

	private static GlintParams params() {
		GlintParams p = new GlintParams();
		p.set(A, 0xFF336699, 2.0f, 1.5f, 0.5f, 30.0f);
		return p;
	}

	@Test
	void equalParamsAreTheSame() {
		assertTrue(params().sameAs(params()));
		GlintParams other = new GlintParams();
		other.set(ResourceLocation.fromNamespaceAndPath("glintsmith", "glint/preset/a"), 0xFF336699, 2.0f, 1.5f, 0.5f, 30.0f);
		assertTrue(params().sameAs(other), "equal texture ids from different instances");
	}

	@Test
	void everyFieldMatters() {
		GlintParams base = params();
		GlintParams p = params();
		p.texture = B;
		assertFalse(base.sameAs(p));
		p = params();
		p.tintArgb = 0xFF336698;
		assertFalse(base.sameAs(p));
		p = params();
		p.speed = 2.1f;
		assertFalse(base.sameAs(p));
		p = params();
		p.scale = 1.6f;
		assertFalse(base.sameAs(p));
		p = params();
		p.strength = 0.6f;
		assertFalse(base.sameAs(p));
		p = params();
		p.rotationDeg = 31.0f;
		assertFalse(base.sameAs(p));
		p = params();
		p.atlasU = 0.125f;
		assertFalse(base.sameAs(p));
		p = params();
		p.atlasV = 0.25f;
		assertFalse(base.sameAs(p));
		assertFalse(base.sameAs(null));
	}

	@Test
	void nullTexturesCompare() {
		GlintParams a = new GlintParams();
		GlintParams b = new GlintParams();
		assertTrue(a.sameAs(b));
		b.texture = A;
		assertFalse(a.sameAs(b));
		assertFalse(b.sameAs(a));
	}

	@Test
	void copyFromCopiesEverything() {
		GlintParams copy = new GlintParams();
		copy.copyFrom(params());
		assertTrue(copy.sameAs(params()));
		assertEquals(A, copy.texture);
		assertEquals(0xFF336699, copy.tintArgb);

		GlintParams scaled = params();
		scaled.setAtlas(0.125f, 0.25f);
		copy.copyFrom(scaled);
		assertEquals(0.125f, copy.atlasU);
		assertEquals(0.25f, copy.atlasV);
		assertTrue(copy.sameAs(scaled));
	}

	@Test
	void vanillaDetection() {
		for (GlintKind kind : GlintKind.values()) {
			GlintParams p = new GlintParams();
			p.texture = kind.vanillaTexture();
			assertTrue(p.isVanilla(kind), kind.name());

			GlintParams wrongFamily = new GlintParams();
			wrongFamily.texture = kind.entityFamily ? GlintTextures.VANILLA_ITEM : GlintTextures.VANILLA_ENTITY;
			assertFalse(wrongFamily.isVanilla(kind), kind.name() + " with the other family's texture");

			p.tintArgb = 0xFFFF0000;
			assertFalse(p.isVanilla(kind));
			p.tintArgb = GlintParams.NO_TINT;
			p.rotationDeg = 0.0f;
			assertFalse(p.isVanilla(kind));
			p.rotationDeg = 10.0f;
			p.speed = 0.5f;
			assertFalse(p.isVanilla(kind));
			p.speed = 1.0f;
			p.scale = 2.0f;
			assertFalse(p.isVanilla(kind));
			p.scale = 1.0f;
			p.strength = 0.9f;
			assertFalse(p.isVanilla(kind));
			p.strength = 1.0f;
			p.setAtlas(0.125f, 1.0f);
			assertFalse(p.isVanilla(kind), "atlas factor u");
			p.setAtlas(1.0f, 0.5f);
			assertFalse(p.isVanilla(kind), "atlas factor v");
			p.setAtlas(1.0f, 1.0f);
			assertTrue(p.isVanilla(kind));
			p.texture = A;
			assertFalse(p.isVanilla(kind));
			p.texture = null;
			assertFalse(p.isVanilla(kind));
		}
	}

	@Test
	void setResetsTheAtlasFactor() {
		GlintParams p = params();
		assertEquals(1.0f, p.atlasU);
		assertEquals(1.0f, p.atlasV);
		p.setAtlas(0.125f, 0.25f);
		p.set(A, 0xFF336699, 2.0f, 1.5f, 0.5f, 30.0f);
		assertEquals(1.0f, p.atlasU, "the scratch params of one request must not leak the factor into the next");
		assertEquals(1.0f, p.atlasV);
	}

	@Test
	void kindMetadataMatchesVanilla() {
		assertEquals(8.0f, GlintKind.GLINT.baseScale);
		assertEquals(8.0f, GlintKind.GLINT_TRANSLUCENT.baseScale);
		assertEquals(0.16f, GlintKind.ENTITY_GLINT.baseScale);
		assertEquals(0.16f, GlintKind.ENTITY_GLINT_DIRECT.baseScale);
		assertEquals(0.16f, GlintKind.ARMOR_ENTITY_GLINT.baseScale);
		assertFalse(GlintKind.GLINT.entityFamily);
		assertFalse(GlintKind.GLINT_TRANSLUCENT.entityFamily);
		assertTrue(GlintKind.ENTITY_GLINT.entityFamily);
		assertTrue(GlintKind.ENTITY_GLINT_DIRECT.entityFamily);
		assertTrue(GlintKind.ARMOR_ENTITY_GLINT.entityFamily);
		// output and layering as in RenderType.java
		assertTrue(GlintKind.GLINT_TRANSLUCENT.itemEntityTarget);
		assertTrue(GlintKind.ENTITY_GLINT.itemEntityTarget);
		assertFalse(GlintKind.GLINT.itemEntityTarget);
		assertFalse(GlintKind.ENTITY_GLINT_DIRECT.itemEntityTarget);
		assertFalse(GlintKind.ARMOR_ENTITY_GLINT.itemEntityTarget);
		assertTrue(GlintKind.ARMOR_ENTITY_GLINT.viewOffset);
		assertEquals(1, java.util.Arrays.stream(GlintKind.values()).filter(k -> k.viewOffset).count());
	}
}
