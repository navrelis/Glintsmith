package navrelis.glintsmith.render.glint;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import navrelis.glintsmith.style.GlintSettings;

class GlintPresetsTest {
	private static final int N = GlintPresets.SIZE;

	private static int r(int p) {
		return p & 0xFF;
	}

	private static int g(int p) {
		return (p >> 8) & 0xFF;
	}

	private static int b(int p) {
		return (p >> 16) & 0xFF;
	}

	@Test
	void everyListedPresetIsGeneratedOrNeutral() {
		Set<String> all = new HashSet<>(GlintPresets.GENERATED);
		all.add("neutral");
		assertEquals(new HashSet<>(GlintSettings.PRESETS), all);
	}

	@Test
	void unknownNameIsRejected() {
		assertThrows(IllegalArgumentException.class, () -> GlintPresets.generate("neutral"));
		assertThrows(IllegalArgumentException.class, () -> GlintPresets.generate("nope"));
	}

	@Test
	void sizeAlphaAndPeak() {
		for (String name : GlintPresets.GENERATED) {
			int[] px = GlintPresets.generate(name);
			assertEquals(N * N, px.length, name);
			int max = 0;
			for (int p : px) {
				assertEquals(255, p >>> 24, name + " alpha");
				max = Math.max(max, Math.max(r(p), Math.max(g(p), b(p))));
			}
			assertTrue(max <= 0.9 * 255, name + " peak " + max);
			assertTrue(max >= 60, name + " is too dark: peak " + max);
		}
	}

	/**
	 * A glint texture is an additive overlay, so dark texels are fine (they mean "no glint there") and a preset may be
	 * bright everywhere too. What a preset must guarantee is that it is neither blank nor a flat fill, and that its light
	 * is spread over the tile (no large dead areas, so no part of an item is permanently without glint).
	 */
	@Test
	void everyPresetHasLightSpreadOverTheTile() {
		int block = N / 4;
		for (String name : GlintPresets.GENERATED) {
			int[] px = GlintPresets.generate(name);
			int lit = 0;
			int min = 255;
			int max = 0;
			int[] litPerBlock = new int[16];
			for (int y = 0; y < N; y++) {
				for (int x = 0; x < N; x++) {
					int p = px[y * N + x];
					int peak = Math.max(r(p), Math.max(g(p), b(p)));
					min = Math.min(min, peak);
					max = Math.max(max, peak);
					if (r(p) + g(p) + b(p) > 24) {
						lit++;
						litPerBlock[(y / block) * 4 + x / block]++;
					}
				}
			}
			assertTrue(lit >= N * N / 100, name + " has only " + lit + " lit pixels");
			assertTrue(max - min >= 60, name + " is a flat fill: brightest " + max + ", darkest " + min);
			for (int i = 0; i < litPerBlock.length; i++) {
				assertTrue(litPerBlock[i] > 0, name + " has a dead " + block + "x" + block + " block at index " + i);
			}
		}
	}

	/**
	 * The glint blend is additive (src * src + dst): a bright texture washes the whole item in one colour. Vanilla's item
	 * glint has a mean luminance of 0.23, so the presets must stay near that, with clear dark gaps, while the peaks still
	 * reach (nearly) full brightness. The sparkle preset is allowed to be much sparser than the others.
	 */
	@Test
	void presetsStayWithinTheBrightnessBudget() {
		for (String name : GlintPresets.GENERATED) {
			double sum = 0;
			int dark = 0;
			int peak = 0;
			int[] px = GlintPresets.generate(name);
			for (int p : px) {
				double lum = 0.2126 * r(p) / 255.0 + 0.7152 * g(p) / 255.0 + 0.0722 * b(p) / 255.0;
				sum += lum;
				if (lum < 0.08) {
					dark++;
				}
				peak = Math.max(peak, Math.max(r(p), Math.max(g(p), b(p))));
			}
			double mean = sum / px.length;
			double darkShare = dark / (double) px.length;
			double minMean = name.equals("sparkle") ? 0.03 : 0.15;
			assertTrue(mean >= minMean && mean <= 0.32, name + " mean luminance " + mean);
			assertTrue(darkShare >= 0.30, name + " dark share " + darkShare);
			assertTrue(peak >= 200, name + " peak " + peak + " should reach near full brightness");
		}
	}

	@Test
	void deterministic() {
		for (String name : GlintPresets.GENERATED) {
			assertArrayEquals(GlintPresets.generate(name), GlintPresets.generate(name), name);
		}
	}

	@Test
	void samplesArePeriodicInBothAxes() {
		float[] a = new float[3];
		float[] c = new float[3];
		for (String name : GlintPresets.GENERATED) {
			for (int y = 0; y < N; y += 5) {
				for (int x = 0; x < N; x += 5) {
					GlintPresets.sample(name, x, y, a);
					GlintPresets.sample(name, x + N, y + 2 * N, c);
					for (int k = 0; k < 3; k++) {
						assertEquals(a[k], c[k], 5.0e-3f, name + " at " + x + "," + y + " channel " + k);
					}
				}
			}
		}
	}

	@Test
	void samplesMatchTheGeneratedPixels() {
		float[] rgb = new float[3];
		for (String name : GlintPresets.GENERATED) {
			int[] px = GlintPresets.generate(name);
			for (int i = 0; i < 40; i++) {
				int x = (i * 37) % N;
				int y = (i * 53) % N;
				GlintPresets.sample(name, x, y, rgb);
				assertEquals(GlintPresets.pack(rgb[0], rgb[1], rgb[2]), px[y * N + x], name);
			}
		}
	}

	@Test
	void tileEdgesAreContinuous() {
		for (String name : GlintPresets.GENERATED) {
			int[] px = GlintPresets.generate(name);
			double seamX = 0;
			double seamY = 0;
			double innerX = 0;
			double innerY = 0;
			for (int i = 0; i < N; i++) {
				seamX += diff(px[i * N + (N - 1)], px[i * N]);
				seamY += diff(px[(N - 1) * N + i], px[i]);
				for (int j = 0; j < N - 1; j++) {
					innerX += diff(px[i * N + j], px[i * N + j + 1]) / (N - 1);
					innerY += diff(px[j * N + i], px[(j + 1) * N + i]) / (N - 1);
				}
			}
			seamX /= N;
			seamY /= N;
			innerX /= N;
			innerY /= N;
			assertTrue(seamX <= 2.0 * innerX + 1.0, name + " horizontal seam " + seamX + " vs " + innerX);
			assertTrue(seamY <= 2.0 * innerY + 1.0, name + " vertical seam " + seamY + " vs " + innerY);
		}
	}

	private static double diff(int p, int q) {
		return (Math.abs(r(p) - r(q)) + Math.abs(g(p) - g(q)) + Math.abs(b(p) - b(q))) / 3.0;
	}

	@Test
	void presetsLookDifferentFromEachOther() {
		int[][] all = GlintPresets.GENERATED.stream().map(GlintPresets::generate).toArray(int[][]::new);
		for (int i = 0; i < all.length; i++) {
			for (int j = i + 1; j < all.length; j++) {
				assertTrue(!Arrays.equals(all[i], all[j]));
			}
		}
	}

	@Test
	void goldIsWarmAndVoidIsPurple() {
		long goldR = 0;
		long goldB = 0;
		long voidR = 0;
		long voidG = 0;
		long voidB = 0;
		for (int p : GlintPresets.generate("gold")) {
			goldR += r(p);
			goldB += b(p);
		}
		for (int p : GlintPresets.generate("void")) {
			voidR += r(p);
			voidG += g(p);
			voidB += b(p);
		}
		assertTrue(goldR > 2 * goldB, "gold should be warm");
		assertTrue(voidB > voidR && voidR > voidG, "void should be purple: " + voidR + " " + voidG + " " + voidB);
	}

	@Test
	void packCapsAtThePeak() {
		int p = GlintPresets.pack(1.0f, 2.0f, -1.0f);
		assertEquals(GlintPresets.PEAK_BYTE, r(p));
		assertEquals(GlintPresets.PEAK_BYTE, g(p));
		assertEquals(0, b(p));
		assertEquals(255, p >>> 24);
	}

	@Test
	void neutralIsGreyNormalisedAndKeepsTheSize() {
		// ABGR: a green pixel, a cyan pixel, a transparent pixel, a black pixel
		int[] source = {
			0xFF00FF80, 0xFFFFFF00, 0x00FFFFFF, 0xFF000000};
		int[] grey = GlintPresets.neutral(source);
		assertEquals(source.length, grey.length);
		int peak = 0;
		for (int p : grey) {
			assertEquals(255, p >>> 24);
			assertEquals(r(p), g(p));
			assertEquals(g(p), b(p));
			peak = Math.max(peak, r(p));
		}
		assertEquals(GlintPresets.PEAK_BYTE, peak, "normalised to the peak");
		assertEquals(0, r(grey[2]), "transparent pixels carry no glint");
		assertEquals(0, r(grey[3]), "black stays black");
	}

	@Test
	void neutralOfBlackIsBlack() {
		int[] grey = GlintPresets.neutral(new int[] {0xFF000000, 0xFF000000});
		assertEquals(0, r(grey[0]));
		assertEquals(0, r(grey[1]));
	}
}
