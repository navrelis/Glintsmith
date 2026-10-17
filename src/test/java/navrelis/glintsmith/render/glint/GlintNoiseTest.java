package navrelis.glintsmith.render.glint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GlintNoiseTest {
	private static final float EPS = 1.0e-4f;

	@Test
	void hashIsDeterministicAndInRange() {
		for (int y = -20; y < 40; y++) {
			for (int x = -20; x < 40; x++) {
				float h = GlintNoise.hash(x, y, 7);
				assertTrue(h >= 0.0f && h < 1.0f, "hash out of range: " + h);
				assertEquals(h, GlintNoise.hash(x, y, 7));
			}
		}
		assertNotEquals(GlintNoise.hash(3, 4, 1), GlintNoise.hash(3, 4, 2));
		assertNotEquals(GlintNoise.hash(3, 4, 1), GlintNoise.hash(4, 3, 1));
	}

	@Test
	void hashIsRoughlyUniform() {
		double sum = 0;
		int n = 0;
		for (int y = 0; y < 128; y++) {
			for (int x = 0; x < 128; x++) {
				sum += GlintNoise.hash(x, y, 99);
				n++;
			}
		}
		assertEquals(0.5, sum / n, 0.02);
	}

	@Test
	void valueNoiseIsInRangeAndTiles() {
		for (int y = 0; y < 128; y += 5) {
			for (int x = 0; x < 128; x += 5) {
				float v = GlintNoise.value(x, y, 4, 3, 5);
				assertTrue(v >= 0.0f && v < 1.0f, "value out of range: " + v);
				assertEquals(v, GlintNoise.value(x + 128, y, 4, 3, 5), EPS);
				assertEquals(v, GlintNoise.value(x, y + 128, 4, 3, 5), EPS);
				assertEquals(v, GlintNoise.value(x - 128, y - 256, 4, 3, 5), EPS);
			}
		}
	}

	@Test
	void valueNoiseIsContinuousAcrossTheSeam() {
		for (int y = 0; y < 128; y += 9) {
			float last = GlintNoise.value(127, y, 8, 8, 3);
			float wrapped = GlintNoise.value(128, y, 8, 8, 3);
			float first = GlintNoise.value(0, y, 8, 8, 3);
			assertEquals(wrapped, first, EPS);
			assertTrue(Math.abs(last - wrapped) < 0.2f);
		}
	}

	@Test
	void fbmIsInRangeAndTiles() {
		for (int y = 0; y < 128; y += 7) {
			for (int x = 0; x < 128; x += 7) {
				float v = GlintNoise.fbm(x, y, 2, 2, 4, 11);
				assertTrue(v >= 0.0f && v < 1.0f, "fbm out of range: " + v);
				assertEquals(v, GlintNoise.fbm(x + 128, y + 128, 2, 2, 4, 11), EPS);
				assertEquals(v, GlintNoise.fbm(x, y, 2, 2, 4, 11));
			}
		}
	}

	@Test
	void worleyDistancesAreOrderedAndTile() {
		float[] a = new float[4];
		float[] b = new float[4];
		for (int y = 0; y < 128; y += 6) {
			for (int x = 0; x < 128; x += 6) {
				GlintNoise.worley(x, y, 8, 41, a);
				assertTrue(a[0] >= 0.0f && a[0] <= a[1], "F1 " + a[0] + " F2 " + a[1]);
				assertTrue(a[2] >= 0 && a[2] < 8 && a[3] >= 0 && a[3] < 8, "cell out of range");
				GlintNoise.worley(x + 128, y + 128, 8, 41, b);
				assertEquals(a[0], b[0], EPS);
				assertEquals(a[1], b[1], EPS);
				assertEquals(a[2], b[2]);
				assertEquals(a[3], b[3]);
			}
		}
	}

	@Test
	void worleyAcceptsAShortOutputArray() {
		float[] out = new float[2];
		GlintNoise.worley(10, 10, 8, 41, out);
		assertTrue(out[0] <= out[1]);
	}

	@Test
	void hsvPrimaries() {
		assertEquals(0xFF0000, GlintNoise.hsv(0.0f, 1.0f, 1.0f));
		assertEquals(0x00FF00, GlintNoise.hsv(1.0f / 3.0f, 1.0f, 1.0f));
		assertEquals(0x0000FF, GlintNoise.hsv(2.0f / 3.0f, 1.0f, 1.0f));
		assertEquals(0xFFFFFF, GlintNoise.hsv(0.4f, 0.0f, 1.0f));
		assertEquals(0x000000, GlintNoise.hsv(0.4f, 1.0f, 0.0f));
		assertEquals(0x808080, GlintNoise.hsv(0.9f, 0.0f, 0.5f));
	}

	@Test
	void hsvWrapsTheHue() {
		assertEquals(GlintNoise.hsv(0.0f, 0.8f, 0.9f), GlintNoise.hsv(1.0f, 0.8f, 0.9f));
		assertEquals(GlintNoise.hsv(0.25f, 0.8f, 0.9f), GlintNoise.hsv(1.25f, 0.8f, 0.9f));
		assertEquals(GlintNoise.hsv(0.75f, 0.8f, 0.9f), GlintNoise.hsv(-0.25f, 0.8f, 0.9f));
	}

	@Test
	void hsvChannelsStayInRange() {
		float[] rgb = new float[3];
		for (int i = 0; i <= 100; i++) {
			GlintNoise.hsv(i / 100.0f, 0.7f, 1.0f, rgb);
			for (float c : rgb) {
				assertTrue(c >= 0.0f && c <= 1.0f);
			}
		}
	}

	@Test
	void toByteClampsAndRounds() {
		assertEquals(0, GlintNoise.toByte(-1.0f, 255));
		assertEquals(255, GlintNoise.toByte(2.0f, 255));
		assertEquals(229, GlintNoise.toByte(1.0f, 229));
		assertEquals(128, GlintNoise.toByte(0.5f, 255));
	}
}
