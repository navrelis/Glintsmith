package navrelis.glintsmith.render.outline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StyleTableTest {
	@Test
	void sameStyleGetsTheSameIndex() {
		StyleTable table = new StyleTable();
		int a = table.indexFor(0xFFFF0000, 2.0f, 0.0f);
		int b = table.indexFor(0xFF00FF00, 2.0f, 0.0f);
		int c = table.indexFor(0xFFFF0000, 3.0f, 0.0f);
		int d = table.indexFor(0xFFFF0000, 2.0f, 0.5f);
		assertEquals(1, a);
		assertEquals(2, b);
		assertEquals(3, c);
		assertEquals(4, d);
		assertEquals(a, table.indexFor(0xFFFF0000, 2.0f, 0.0f));
		assertEquals(b, table.indexFor(0xFF00FF00, 2.0f, 0.0f));
		assertEquals(4, table.count());
	}

	@Test
	void valuesThatQuantiseAlikeAreDeduplicated() {
		StyleTable table = new StyleTable();
		int a = table.indexFor(0xFF123456, 2.0f, 0.5f);
		// 2.01 * 8 rounds to 16 like 2.0, and 0.5011 * 255 rounds to 128 like 0.5
		assertEquals(a, table.indexFor(0xFF123456, 2.01f, 0.5011f));
		assertEquals(1, table.count());
	}

	@Test
	void transparentColourIsRejected() {
		StyleTable table = new StyleTable();
		assertEquals(0, table.indexFor(0x00FFFFFF, 2.0f, 0.0f));
		assertEquals(0, table.count());
	}

	@Test
	void atMost255StylesFit() {
		StyleTable table = new StyleTable();
		for (int i = 0; i < StyleTable.MAX_STYLES; i++) {
			assertEquals(i + 1, table.indexFor(0xFF000000 | i, 2.0f, 0.0f));
		}
		assertEquals(255, table.count());
		assertEquals(0, table.indexFor(0xFFABCDEF, 2.0f, 0.0f), "the 256th style must be rejected");
		assertEquals(1, table.rejected());
		// Known styles still resolve when the table is full.
		assertEquals(17, table.indexFor(0xFF000000 | 16, 2.0f, 0.0f));
	}

	@Test
	void fullTableGivesANewWidthOfAKnownColourItsNearestStyle() {
		StyleTable table = new StyleTable();
		int a = table.indexFor(0xFF112233, 2.0f, 0.0f);
		int b = table.indexFor(0xFF112233, 6.0f, 0.0f);
		for (int i = 0; table.count() < StyleTable.MAX_STYLES; i++) {
			table.indexFor(0xFF000000 | i, 2.0f, 0.0f);
		}
		assertEquals(a, table.indexFor(0xFF112233, 2.5f, 0.0f), "2.5 px is nearest to 2 px");
		assertEquals(b, table.indexFor(0xFF112233, 5.0f, 0.0f), "5 px is nearest to 6 px");
		assertEquals(b, table.indexFor(0xFF112233, 30.0f, 0.0f));
		assertEquals(0, table.indexFor(0xFF445566, 2.0f, 0.0f), "an unknown colour still gets nothing");
		assertEquals(4, table.rejected());
		assertEquals(StyleTable.MAX_STYLES, table.count());
	}

	@Test
	void radiusFollowsTheWidestStyleAndIsCappedAt32() {
		StyleTable table = new StyleTable();
		assertEquals(1, table.maxRadius());
		table.indexFor(0xFFFFFFFF, 2.0f, 0.0f);
		assertEquals(3, table.maxRadius()); // ceil(2 * 1 + 1)
		table.indexFor(0xFFFFFFFE, 4.0f, 1.0f);
		assertEquals(9, table.maxRadius()); // ceil(4 * 2 + 1)
		table.indexFor(0xFFFFFFFD, 1000.0f, 1.0f);
		assertTrue(table.maxRadius() <= StyleTable.MAX_RADIUS);
		assertEquals(StyleTable.MAX_RADIUS, table.maxRadius());
		table.indexFor(0xFFFFFFFC, 31.0f, 0.0f);
		assertTrue(table.maxRadius() <= 32);
	}

	@Test
	void thicknessIsClampedSoTheOutlineFitsTheRadius() {
		for (float softness = 0.0f; softness <= 1.0f; softness += 0.125f) {
			float t = StyleTable.clampThickness(10000.0f, softness);
			assertTrue(t * (1.0f + softness) + 1.0f <= StyleTable.MAX_RADIUS + 1.0e-4f, "softness " + softness);
			int packed = StyleTable.packParams(10000.0f, softness);
			float decodedT = (packed & 0xFF) / 8.0f;
			float decodedS = ((packed >>> 8) & 0xFF) / 255.0f;
			assertTrue(decodedT * (1.0f + decodedS) + 1.0f <= StyleTable.MAX_RADIUS + 1.0e-4f, "decoded, softness " + softness);
		}
		assertEquals(StyleTable.MIN_THICKNESS, StyleTable.clampThickness(0.0f, 0.0f));
		assertEquals(StyleTable.MIN_THICKNESS, StyleTable.clampThickness(Float.NaN, 0.0f));
	}

	@Test
	void pixelsAreEncodedForTheShader() {
		StyleTable table = new StyleTable();
		int index = table.indexFor(0x80102030, 2.5f, 1.0f);
		// ABGR as NativeImage expects: alpha 0x80, blue 0x30, green 0x20, red 0x10
		assertEquals(0x80302010, table.colorPixel(index));
		assertEquals(20, table.thicknessByte(index)); // 2.5 * 8
		assertEquals(255, table.softnessByte(index));
		assertEquals(0xFF000000 | (255 << 8) | 20, table.paramPixel(index));
	}

	@Test
	void resetEmptiesTheTable() {
		StyleTable table = new StyleTable();
		table.indexFor(0xFFFF0000, 8.0f, 1.0f);
		table.indexFor(0xFF00FF00, 2.0f, 0.0f);
		table.reset();
		assertEquals(0, table.count());
		assertEquals(1, table.maxRadius());
		assertEquals(0, table.rejected());
		assertEquals(1, table.indexFor(0xFF00FF00, 2.0f, 0.0f));
		assertNotEquals(2, table.indexFor(0xFF00FF00, 2.0f, 0.0f));
		assertEquals(1, table.count());
	}

	@Test
	void resetAfterAFullTableAllowsNewStyles() {
		StyleTable table = new StyleTable();
		for (int i = 0; i < 300; i++) {
			table.indexFor(0xFF000000 | (i * 7919), 1.0f + (i % 5), 0.0f);
		}
		table.reset();
		for (int i = 0; i < StyleTable.MAX_STYLES; i++) {
			assertEquals(i + 1, table.indexFor(0xFF000000 | (i * 104729), 2.0f, 0.25f));
		}
	}
}
