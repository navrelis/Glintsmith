package navrelis.glintsmith.render.glint;

import java.util.List;

/**
 * Procedural, tileable glint textures. Pure Java: every preset is an {@code int[128 * 128]} of ABGR pixels (the layout of
 * {@code NativeImage.setPixelRGBA}), row major, alpha always 255. Black means "no glint", because the glint blend is
 * additive. The peak of every channel stays at or below {@link #PEAK_BYTE} (0.9 of 255).
 * <p>
 * Brightness budget: the glint is additive (src * src + dst), so a texture that is lit everywhere washes the item in
 * one colour. The generated presets are shaped with smoothstep thresholds, not darkened: the mean luminance stays near
 * that of the vanilla item glint (0.23), a large share of the texels is dark, and the peaks still reach {@link #PEAK_BYTE}.
 * <p>
 * Deterministic: the same input gives the same bytes on every run and platform.
 */
final class GlintPresets {
	static final int SIZE = GlintNoise.TILE;
	/** Highest value a generated channel may take: floor(0.9 * 255). */
	static final int PEAK_BYTE = 229;
	/** Presets generated here (the neutral textures come from the resource pack instead). */
	static final List<String> GENERATED = List.of("rainbow", "aurora", "fire", "frost", "sparkle", "gold", "void");

	private static final float TWO_PI = (float) (2.0 * StrictMath.PI);

	private GlintPresets() {
	}

	/**
	 * Generates a preset.
	 *
	 * @param name one of {@link #GENERATED}
	 * @return ABGR pixels, {@link #SIZE} by {@link #SIZE}
	 * @throws IllegalArgumentException for an unknown name
	 */
	static int[] generate(String name) {
		if (!GENERATED.contains(name)) {
			throw new IllegalArgumentException("Unknown generated glint preset: " + name);
		}
		int[] pixels = new int[SIZE * SIZE];
		float[] rgb = new float[3];
		float[] w = new float[4];
		float[] c = new float[3];
		for (int y = 0; y < SIZE; y++) {
			for (int x = 0; x < SIZE; x++) {
				sample(name, x, y, rgb, w, c);
				pixels[y * SIZE + x] = pack(rgb[0], rgb[1], rgb[2]);
			}
		}
		return pixels;
	}

	/**
	 * The colour of one pixel. Periodic: any integer x and y give the value of {@code (x mod 128, y mod 128)}.
	 *
	 * @param rgb receives r, g, b (about 0..0.9)
	 */
	static void sample(String name, int x, int y, float[] rgb) {
		sample(name, x, y, rgb, new float[4], new float[3]);
	}

	private static void sample(String name, int x, int y, float[] rgb, float[] w, float[] c) {
		switch (name) {
			case "rainbow" -> rainbow(x, y, rgb, c);
			case "aurora" -> aurora(x, y, rgb, c);
			case "fire" -> fire(x, y, rgb);
			case "frost" -> frost(x, y, rgb, w);
			case "sparkle" -> sparkle(x, y, rgb, w);
			case "gold" -> gold(x, y, rgb, w);
			case "void" -> voidPreset(x, y, rgb);
			default -> throw new IllegalArgumentException("Unknown generated glint preset: " + name);
		}
	}

	/**
	 * Packs a colour into ABGR with alpha 255 and every channel capped at {@link #PEAK_BYTE}.
	 */
	static int pack(float r, float g, float b) {
		int ri = GlintNoise.toByte(r, PEAK_BYTE);
		int gi = GlintNoise.toByte(g, PEAK_BYTE);
		int bi = GlintNoise.toByte(b, PEAK_BYTE);
		return 0xFF000000 | (bi << 16) | (gi << 8) | ri;
	}

	// u = x / 128 and v = y / 128 of the design document.

	private static void rainbow(int x, int y, float[] rgb, float[] c) {
		float u = x / (float) SIZE;
		float v = y / (float) SIZE;
		float streak = GlintNoise.fbm(x, y, 2, 2, 3, 11);
		float hue = frac(u + v + 0.25f * GlintNoise.fbm(x, y, 2, 2, 2, 12));
		float intensity = 0.95f * GlintNoise.smoothstep(0.37f, 0.70f, streak);
		GlintNoise.hsv(hue, 0.85f, 1.0f, c);
		rgb[0] = c[0] * intensity;
		rgb[1] = c[1] * intensity;
		rgb[2] = c[2] * intensity;
	}

	private static void aurora(int x, int y, float[] rgb, float[] c) {
		float u = x / (float) SIZE;
		float phase = 2.0f * u + 0.6f * GlintNoise.fbm(x, y, 2, 2, 2, 21);
		float curtain = 0.5f + 0.5f * (float) StrictMath.sin(TWO_PI * phase);
		float detail = GlintNoise.fbm(x, y, 4, 4, 4, 22);
		float intensity = 0.95f * curtain * curtain * (0.35f + 0.65f * GlintNoise.smoothstep(0.2f, 0.7f, detail));
		float hue = GlintNoise.lerp(GlintNoise.fbm(x, y, 2, 2, 2, 23), 0.33f, 0.78f);
		GlintNoise.hsv(hue, 0.7f, 1.0f, c);
		rgb[0] = c[0] * intensity;
		rgb[1] = c[1] * intensity;
		rgb[2] = c[2] * intensity;
	}

	private static void fire(int x, int y, float[] rgb) {
		float base = GlintNoise.fbm(x, y, 4, 2, 4, 31);
		float heat = GlintNoise.smoothstep(0.38f, 0.74f, base);
		ramp(heat, rgb,
			0.0f, 0.0f, 0.0f, 0.0f,
			0.35f, 0.55f, 0.05f, 0.0f,
			0.65f, 0.9f, 0.42f, 0.0f,
			1.0f, 0.9f, 0.85f, 0.55f);
	}

	private static void frost(int x, int y, float[] rgb, float[] w) {
		GlintNoise.worley(x, y, 8, 41, w);
		float edge = 1.0f - GlintNoise.smoothstep(0.0f, 0.22f, w[1] - w[0]);
		float spark = GlintNoise.hash(x & (SIZE - 1), y & (SIZE - 1), 42) > 0.996f ? 1.0f : 0.0f;
		float intensity = Math.max(0.9f * edge, spark) * (0.55f + 0.45f * GlintNoise.fbm(x, y, 4, 4, 3, 43));
		ramp(intensity, rgb,
			0.0f, 0.0f, 0.0f, 0.0f,
			0.5f, 0.22f, 0.5f, 0.85f,
			1.0f, 0.8f, 0.9f, 0.9f);
	}

	private static void sparkle(int x, int y, float[] rgb, float[] w) {
		GlintNoise.worley(x, y, 12, 51, w);
		float b = GlintNoise.hash((int) w[2], (int) w[3], 52);
		float intensity = 0.0f;
		if (b > 0.35f) {
			float falloff = 1.0f - GlintNoise.smoothstep(0.0f, 0.5f, w[0]);
			intensity = b * falloff * falloff;
		}
		rgb[0] = 0.9f * intensity;
		rgb[1] = 0.86f * intensity;
		rgb[2] = 0.78f * intensity;
	}

	/**
	 * Warm diagonal streaks plus rare bright glints. The streaks are value noise sampled on the sheared coordinates
	 * (x + y, x - y): both are periodic in the tile, so the pattern tiles, and its long axis runs along the diagonal.
	 */
	private static void gold(int x, int y, float[] rgb, float[] w) {
		float a = x + y;
		float b = x - y;
		float streak = GlintNoise.fbm(a, b, 10, 2, 3, 61);
		float grain = GlintNoise.fbm(x, y, 16, 16, 2, 62);
		float heat = GlintNoise.smoothstep(0.3f, 0.85f, streak) * (0.7f + 0.3f * grain);
		ramp(heat, rgb,
			0.0f, 0.0f, 0.0f, 0.0f,
			0.35f, 0.45f, 0.28f, 0.02f,
			0.75f, 0.9f, 0.7f, 0.2f,
			1.0f, 0.9f, 0.86f, 0.6f);

		GlintNoise.worley(x, y, 12, 63, w);
		float cell = GlintNoise.hash((int) w[2], (int) w[3], 64);
		if (cell > 0.8f) {
			float falloff = 1.0f - GlintNoise.smoothstep(0.0f, 0.15f, w[0]);
			float glint = cell * falloff * falloff * falloff;
			rgb[0] = Math.max(rgb[0], 0.9f * glint);
			rgb[1] = Math.max(rgb[1], 0.86f * glint);
			rgb[2] = Math.max(rgb[2], 0.6f * glint);
		}
	}

	/**
	 * Deep purple nebula, domain warped by periodic noise (so it still tiles), with sparse single-pixel stars that have
	 * a faint cross.
	 */
	private static void voidPreset(int x, int y, float[] rgb) {
		float warpX = GlintNoise.fbm(x, y, 2, 2, 3, 72);
		float warpY = GlintNoise.fbm(x, y, 2, 2, 3, 75);
		float t = GlintNoise.fbm(x + 28.0f * (warpX - 0.5f), y + 28.0f * (warpY - 0.5f), 3, 3, 5, 73);
		float nebula = GlintNoise.smoothstep(0.40f, 0.85f, t);
		ramp(nebula, rgb,
			0.0f, 0.0f, 0.0f, 0.0f,
			0.3f, 0.12f, 0.0f, 0.25f,
			0.7f, 0.45f, 0.1f, 0.7f,
			1.0f, 0.75f, 0.55f, 0.9f);

		float star = starAt(x, y);
		if (star == 0.0f) {
			float near = Math.max(Math.max(starAt(x - 1, y), starAt(x + 1, y)), Math.max(starAt(x, y - 1), starAt(x, y + 1)));
			star = 0.35f * near;
		}
		if (star > 0.0f) {
			rgb[0] = Math.max(rgb[0], 0.9f * star);
			rgb[1] = Math.max(rgb[1], 0.85f * star);
			rgb[2] = Math.max(rgb[2], 0.9f * star);
		}
	}

	/** 0 for no star, otherwise the star brightness 0.6 to 1. */
	private static float starAt(int x, int y) {
		int wx = x & (SIZE - 1);
		int wy = y & (SIZE - 1);
		if (GlintNoise.hash(wx, wy, 74) > 0.9985f) {
			return 0.6f + 0.4f * GlintNoise.hash(wx, wy, 76);
		}
		return 0.0f;
	}

	/**
	 * Piecewise-linear gradient. {@code stops} are (position, r, g, b) quadruples with rising positions.
	 */
	private static void ramp(float t, float[] out, float... stops) {
		int count = stops.length / 4;
		if (t <= stops[0]) {
			out[0] = stops[1];
			out[1] = stops[2];
			out[2] = stops[3];
			return;
		}
		for (int i = 1; i < count; i++) {
			float p1 = stops[i * 4];
			if (t <= p1) {
				float p0 = stops[(i - 1) * 4];
				float f = (t - p0) / (p1 - p0);
				for (int k = 0; k < 3; k++) {
					float c0 = stops[(i - 1) * 4 + 1 + k];
					float c1 = stops[i * 4 + 1 + k];
					out[k] = c0 + f * (c1 - c0);
				}
				return;
			}
		}
		int last = (count - 1) * 4;
		out[0] = stops[last + 1];
		out[1] = stops[last + 2];
		out[2] = stops[last + 3];
	}

	private static float frac(float v) {
		return v - (float) Math.floor(v);
	}

	/**
	 * The neutral grey base for tinting: luminance of the source (weighted by its alpha), normalised so the brightest
	 * pixel is {@code 0.9}. Same size as the source. A source without any light pixel gives black.
	 *
	 * @param abgr source pixels, ABGR
	 * @return grey ABGR pixels with alpha 255
	 */
	static int[] neutral(int[] abgr) {
		float[] lum = new float[abgr.length];
		float max = 0.0f;
		for (int i = 0; i < abgr.length; i++) {
			int p = abgr[i];
			float r = (p & 0xFF) / 255.0f;
			float g = ((p >> 8) & 0xFF) / 255.0f;
			float b = ((p >> 16) & 0xFF) / 255.0f;
			float a = ((p >>> 24) & 0xFF) / 255.0f;
			float l = (0.2126f * r + 0.7152f * g + 0.0722f * b) * a;
			lum[i] = l;
			max = Math.max(max, l);
		}
		int[] out = new int[abgr.length];
		for (int i = 0; i < out.length; i++) {
			float l = max > 0.0f ? lum[i] / max * 0.9f : 0.0f;
			out[i] = pack(l, l, l);
		}
		return out;
	}
}
