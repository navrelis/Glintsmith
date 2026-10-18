package navrelis.glintsmith.render.glint;

import java.util.List;

/**
 * Procedural, tileable glint textures. Pure Java: every preset is an {@code int[128 * 128]} of ABGR pixels (the layout of
 * {@code NativeImage.setPixelRGBA}), row major, alpha always 255. Black means "no glint", because the glint blend is
 * additive. The peak of every channel stays at or below {@link #PEAK_BYTE} (0.9 of 255).
 * <p>
 * Two families:
 * <ul>
 * <li>{@link #CLEAN}: the character of the vanilla glint in other colours. A soft glow over the whole tile carries two
 * crossing families of soft streaks ({@link GlintNoise#streaks}), so the item is never without glint and the scrolling
 * texture sweeps light across it.</li>
 * <li>{@link #RICH}: more detailed, eye-catching textures (flames, nebula, star sparkles, opal, lightning).</li>
 * </ul>
 * Brightness budget: the glint blend squares the texture colour ({@code src * src + dst}), so what an item receives is
 * the mean of the squared channels, the "glint energy". The presets keep it near that of the vanilla item glint.
 * <p>
 * Deterministic: the same input gives the same bytes on every run and platform.
 */
final class GlintPresets {
	static final int SIZE = GlintNoise.TILE;
	/** Highest value a generated channel may take: floor(0.9 * 255). */
	static final int PEAK_BYTE = 229;
	/** The vanilla-like presets. */
	static final List<String> CLEAN = List.of("rainbow", "aurora", "fire", "frost", "sparkle", "gold", "void");
	/** The detailed presets. */
	static final List<String> RICH = List.of("inferno", "nebula", "starlight", "opal", "storm");
	/** Presets generated here (the neutral textures come from the resource pack instead), in UI order. */
	static final List<String> GENERATED = List.of(
		"rainbow", "aurora", "fire", "frost", "sparkle", "gold", "void",
		"inferno", "nebula", "starlight", "opal", "storm");

	private static final float TWO_PI = (float) (2.0 * StrictMath.PI);
	/** Size of the scratch array the sample functions share. */
	private static final int SCRATCH = 8;

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
		float[] s = new float[SCRATCH];
		for (int y = 0; y < SIZE; y++) {
			for (int x = 0; x < SIZE; x++) {
				sample(name, x, y, rgb, s);
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
		sample(name, x, y, rgb, new float[SCRATCH]);
	}

	private static void sample(String name, int x, int y, float[] rgb, float[] s) {
		switch (name) {
			case "rainbow" -> rainbow(x, y, rgb, s);
			case "aurora" -> aurora(x, y, rgb, s);
			case "fire" -> fire(x, y, rgb, s);
			case "frost" -> frost(x, y, rgb, s);
			case "sparkle" -> sparkle(x, y, rgb, s);
			case "gold" -> gold(x, y, rgb, s);
			case "void" -> voidPreset(x, y, rgb, s);
			case "inferno" -> inferno(x, y, rgb, s);
			case "nebula" -> nebula(x, y, rgb, s);
			case "starlight" -> starlight(x, y, rgb, s);
			case "opal" -> opal(x, y, rgb, s);
			case "storm" -> storm(x, y, rgb, s);
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

	// ---------------------------------------------------------------- the vanilla-like family

	/**
	 * The structure every clean preset shares: a gentle glow over the whole tile and two crossing families of soft
	 * streaks, made slightly wavy by periodic noise. The main streaks run diagonally on the item (the glint matrix shows
	 * the texture at half its height), about one per item, so the scrolling texture sweeps light across it.
	 *
	 * @param glow  strength of the glow under the streaks
	 * @param cross strength of the cross streaks
	 * @param s receives the main streaks at index 0, the cross streaks at 1 and the slow glow wave at 2
	 * @return brightness, about 0..1
	 */
	private static float sheen(float x, float y, float glow, float cross, int seed, float[] s) {
		float wobble = GlintNoise.fbm(x, y, 2, 2, 3, seed) - 0.5f;
		float main = GlintNoise.streaks(x, y, 2, 1, 2, 0.34f, 0.7f * wobble, seed + 1);
		float second = GlintNoise.streaks(x, y, -1, 1, 3, 0.26f, 0.6f * wobble, seed + 2);
		float wave = 0.5f + 0.5f * (float) StrictMath.sin(TWO_PI * ((x + 2.0f * y) / SIZE + 0.6f * wobble));
		s[0] = main;
		s[1] = second;
		s[2] = wave;
		float v = glow * (0.2f + 0.3f * wave) + main + cross * second;
		return Math.min(1.0f, (1.0f - (float) StrictMath.exp(-1.8f * v)) / 0.8f);
	}

	/** Soft rainbow streaks: the hue shifts diagonally, so the colours cycle as the texture scrolls; cores turn whiter. */
	private static void rainbow(int x, int y, float[] rgb, float[] s) {
		float v = sheen(x, y, 0.24f, 0.3f, 11, s);
		float hue = frac((x + y) / (float) SIZE + 0.3f * GlintNoise.fbm(x, y, 2, 2, 2, 15));
		float sat = 0.68f - 0.3f * s[0] * s[0];
		GlintNoise.hsv(hue, sat, 1.0f, s);
		scale(s, v, rgb);
	}

	/** Green, teal and violet veils: the hue swings between green and violet in wavy diagonal bands. */
	private static void aurora(int x, int y, float[] rgb, float[] s) {
		float v = sheen(x, y, 0.34f, 0.3f, 21, s);
		float band = 0.5f + 0.5f * (float) StrictMath.sin(TWO_PI * ((x + y) / (float) SIZE + 0.8f * GlintNoise.fbm(x, y, 2, 2, 3, 25)));
		float hue = GlintNoise.lerp(band, 0.36f, 0.8f);
		GlintNoise.hsv(hue, 0.8f - 0.3f * s[0] * s[0], 1.0f, s);
		scale(s, v, rgb);
	}

	/** Ember red glow with orange streaks and yellow-white cores. */
	private static void fire(int x, int y, float[] rgb, float[] s) {
		float v = sheen(x, y, 0.2f, 0.25f, 31, s);
		ramp(v, rgb,
			0.0f, 0.0f, 0.0f, 0.0f,
			0.35f, 0.34f, 0.04f, 0.02f,
			0.65f, 0.76f, 0.26f, 0.03f,
			0.88f, 0.93f, 0.6f, 0.2f,
			1.0f, 0.97f, 0.86f, 0.56f);
	}

	/**
	 * Pale ice: a cool, low-saturation sheen with crisp white streaks. Kept close to white so a tint (the item layer's
	 * default rainbow tint) colours it cleanly.
	 */
	private static void frost(int x, int y, float[] rgb, float[] s) {
		float v = sheen(x, y, 0.3f, 0.45f, 41, s);
		ramp(v, rgb,
			0.0f, 0.0f, 0.0f, 0.0f,
			0.35f, 0.22f, 0.3f, 0.4f,
			0.7f, 0.6f, 0.72f, 0.84f,
			1.0f, 0.9f, 0.95f, 1.0f);
	}

	/** Silver sheen with fine glitter specks. */
	private static void sparkle(int x, int y, float[] rgb, float[] s) {
		float v = sheen(x, y, 0.26f, 0.25f, 51, s);
		float speck = 0.0f;
		float h = GlintNoise.hash(x & (SIZE - 1), y & (SIZE - 1), 52);
		if (h > 0.975f) {
			speck = 0.5f + 0.5f * (h - 0.975f) / 0.025f;
		}
		float base = 0.72f * v;
		rgb[0] = Math.max(0.86f * base, speck);
		rgb[1] = Math.max(0.88f * base, 0.96f * speck);
		rgb[2] = Math.max(0.98f * base, 0.86f * speck);
	}

	/** Polished gold: amber glow, bright streaks like light on metal. */
	private static void gold(int x, int y, float[] rgb, float[] s) {
		float v = sheen(x, y, 0.24f, 0.25f, 61, s);
		ramp(v, rgb,
			0.0f, 0.0f, 0.0f, 0.0f,
			0.35f, 0.44f, 0.28f, 0.04f,
			0.7f, 0.9f, 0.68f, 0.18f,
			1.0f, 0.98f, 0.9f, 0.58f);
	}

	/** Deep violet glow with magenta streaks and a few faint stars. */
	private static void voidPreset(int x, int y, float[] rgb, float[] s) {
		float v = sheen(x, y, 0.2f, 0.3f, 71, s);
		ramp(v, rgb,
			0.0f, 0.0f, 0.0f, 0.0f,
			0.35f, 0.18f, 0.03f, 0.36f,
			0.7f, 0.5f, 0.14f, 0.78f,
			1.0f, 0.88f, 0.62f, 0.98f);
		float star = starAt(x, y);
		if (star > 0.0f) {
			rgb[0] = Math.max(rgb[0], 0.85f * star);
			rgb[1] = Math.max(rgb[1], 0.8f * star);
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

	// ---------------------------------------------------------------- the detailed family

	/**
	 * Flame tongues: one tongue per cell of a 12 by 4 grid, each with its own position, length and width, hot at its base
	 * and narrowing to a tip above it. The texture scrolls towards the top of the tile, so the tongues rise. Periodic
	 * noise sways the tongues sideways, more towards the tips; a faint ember glow fills the gaps.
	 */
	private static void inferno(int x, int y, float[] rgb, float[] s) {
		float sway = GlintNoise.fbm(x, y, 8, 4, 3, 81) - 0.5f;
		float heat = flames(x + 6.0f * sway, y, 82);
		float ember = 0.32f * GlintNoise.smoothstep(0.25f, 0.8f, GlintNoise.fbm(x, y, 8, 4, 3, 85));
		float flame = 1.0f - (1.0f - heat) * (1.0f - ember);
		ramp(flame, rgb,
			0.0f, 0.0f, 0.0f, 0.0f,
			0.2f, 0.26f, 0.02f, 0.0f,
			0.5f, 0.72f, 0.18f, 0.01f,
			0.78f, 0.93f, 0.52f, 0.08f,
			1.0f, 0.98f, 0.86f, 0.5f);
	}

	/**
	 * The heat of the flame tongues at a point, 0..1 (see {@link #inferno}). The tongues are drawn in the texture twice
	 * as tall as they should look, because the glint matrix shows the texture squashed to half its height.
	 */
	private static float flames(float x, float y, int seed) {
		int columns = 12;
		int rows = 4;
		float cellW = SIZE / (float) columns;
		float cellH = SIZE / (float) rows;
		int cx = (int) Math.floor(x / cellW);
		int cy = (int) Math.floor(y / cellH);
		float cold = 1.0f;
		for (int dy = -1; dy <= 2; dy++) {
			for (int dx = -1; dx <= 1; dx++) {
				int ix = cx + dx;
				int iy = cy + dy;
				int wx = Math.floorMod(ix, columns);
				int wy = Math.floorMod(iy, rows);
				float tx = (ix + 0.2f + 0.6f * GlintNoise.hash(wx, wy, seed)) * cellW;
				float baseY = (iy + 0.5f + 0.5f * GlintNoise.hash(wx, wy, seed + 1)) * cellH;
				float length = cellH * (1.0f + 0.8f * GlintNoise.hash(wx, wy, seed + 2));
				float width = 4.0f + 3.5f * GlintNoise.hash(wx, wy, seed + 3);
				float t = (baseY - y) / length;
				if (t < -0.25f || t > 1.0f) {
					continue;
				}
				float along = t < 0.0f ? 1.0f + t / 0.25f : (float) StrictMath.pow(1.0f - t, 1.3f);
				float w = width * (t < 0.0f ? 1.0f : 0.15f + 0.85f * (float) StrictMath.pow(1.0f - t, 0.8f));
				float across = 1.0f - GlintNoise.smoothstep(0.0f, w, Math.abs(x - tx));
				float h = along * across;
				cold *= 1.0f - h;
			}
		}
		return 1.0f - cold;
	}

	/** Domain-warped clouds in magenta, blue and cyan over a deep indigo glow, with stars. */
	private static void nebula(int x, int y, float[] rgb, float[] s) {
		float qx = GlintNoise.fbm(x, y, 3, 3, 4, 91) - 0.5f;
		float qy = GlintNoise.fbm(x, y, 3, 3, 4, 92) - 0.5f;
		float rx = GlintNoise.fbm(x + 30.0f * qx, y + 30.0f * qy, 3, 3, 4, 93) - 0.5f;
		float ry = GlintNoise.fbm(x + 30.0f * qx, y + 30.0f * qy, 3, 3, 4, 94) - 0.5f;
		float t = GlintNoise.fbm(x + 36.0f * rx, y + 36.0f * ry, 6, 6, 4, 95);
		float d = GlintNoise.smoothstep(0.28f, 0.75f, t);
		float mix = GlintNoise.smoothstep(-0.18f, 0.18f, rx - ry);
		float r = GlintNoise.lerp(mix, 0.8f, 0.15f);
		float g = GlintNoise.lerp(mix, 0.2f, 0.55f);
		float b = GlintNoise.lerp(mix, 0.82f, 0.92f);
		float core = d * d * d;
		rgb[0] = 0.1f + 0.75f * d * r + 0.35f * core;
		rgb[1] = 0.04f + 0.75f * d * g + 0.35f * core;
		rgb[2] = 0.22f + 0.75f * d * b + 0.25f * core;
		float star = starAt(x, y);
		if (star > 0.0f) {
			rgb[0] = Math.max(rgb[0], star);
			rgb[1] = Math.max(rgb[1], star);
			rgb[2] = Math.max(rgb[2], star);
		}
	}

	/** Four-pointed star sparkles of many sizes over a cool shimmer. */
	private static void starlight(int x, int y, float[] rgb, float[] s) {
		float shimmer = sheen(x, y, 0.3f, 0.2f, 101, s) * 0.5f;
		float stars = Math.min(1.0f, sparkles(x, y, 10, 102, 1.0f) + 0.6f * sparkles(x, y, 16, 105, 0.45f));
		rgb[0] = 0.6f * shimmer + stars * 0.96f;
		rgb[1] = 0.7f * shimmer + stars * 0.96f;
		rgb[2] = 0.9f * shimmer + stars;
	}

	/**
	 * Sum of four-pointed sparkles: {@code grid} by {@code grid} cells, each with one sparkle of random position,
	 * size and brightness. The shapes are twice as tall as wide in the texture, because the glint matrix shows the
	 * texture squashed to half its height on items and armor. Arms reach into the neighbouring cells, so the 3x3
	 * neighbourhood is summed. Wraps.
	 *
	 * @param big largest sparkle size, 0..1
	 */
	private static float sparkles(float x, float y, int grid, int seed, float big) {
		float cell = SIZE / (float) grid;
		int cx = (int) Math.floor(x / cell);
		int cy = (int) Math.floor(y / cell);
		float sum = 0.0f;
		for (int dy = -1; dy <= 1; dy++) {
			for (int dx = -1; dx <= 1; dx++) {
				int ix = cx + dx;
				int iy = cy + dy;
				int wx = Math.floorMod(ix, grid);
				int wy = Math.floorMod(iy, grid);
				float sx = (ix + 0.15f + 0.7f * GlintNoise.hash(wx, wy, seed)) * cell;
				float sy = (iy + 0.15f + 0.7f * GlintNoise.hash(wx, wy, seed + 1)) * cell;
				float size = big * GlintNoise.hash(wx, wy, seed + 2);
				float bright = 0.35f + 0.65f * size;
				float arm = 2.0f + 5.0f * size * size;
				float ax = Math.abs(x - sx);
				float ay = 0.5f * Math.abs(y - sy);
				float core = (float) StrictMath.exp(-(ax * ax + ay * ay) / (0.6f + 1.6f * size));
				float h = (float) StrictMath.exp(-ay * ay / 0.3f) * Math.max(0.0f, 1.0f - ax / arm);
				float v = (float) StrictMath.exp(-ax * ax / 0.3f) * Math.max(0.0f, 1.0f - ay / arm);
				sum += bright * (core + 0.8f * (h * h + v * v));
			}
		}
		return Math.min(1.0f, sum);
	}

	/** Opal: a milky blue-grey glow with flowing flashes of iridescent colour along the ridges of a warped noise. */
	private static void opal(int x, int y, float[] rgb, float[] s) {
		float qx = GlintNoise.fbm(x, y, 2, 2, 3, 111) - 0.5f;
		float qy = GlintNoise.fbm(x, y, 2, 2, 3, 112) - 0.5f;
		float t = GlintNoise.fbm(x + 36.0f * qx, y + 36.0f * qy, 3, 3, 4, 113);
		float film = frac(2.5f * t + 0.5f * qx);
		float glow = GlintNoise.ridged(x + 30.0f * qy, y + 30.0f * qx, 4, 4, 3, 114);
		float flash = GlintNoise.smoothstep(0.35f, 0.85f, glow);
		GlintNoise.hsv(0.5f + 0.25f * t + 0.3f * qy, 0.35f, 0.32f + 0.16f * t, s);
		GlintNoise.hsv(GlintNoise.lerp(film, 0.45f, 1.25f), 0.72f, 0.95f, rgb);
		for (int k = 0; k < 3; k++) {
			rgb[k] = GlintNoise.lerp(flash, s[k], rgb[k]);
		}
	}

	/** Branching cyan-white lightning veins with a violet halo. */
	private static void storm(int x, int y, float[] rgb, float[] s) {
		float qx = GlintNoise.fbm(x, y, 3, 3, 3, 121) - 0.5f;
		float qy = GlintNoise.fbm(x, y, 3, 3, 3, 122) - 0.5f;
		float r = GlintNoise.ridged(x + 20.0f * qx, y + 20.0f * qy, 3, 3, 4, 123);
		float bolt = GlintNoise.smoothstep(0.58f, 0.9f, r);
		float halo = GlintNoise.smoothstep(0.25f, 0.8f, r);
		float cloud = GlintNoise.fbm(x, y, 2, 2, 3, 124);
		rgb[0] = 0.2f * cloud + 0.15f * halo + 0.2f * halo * halo + 0.75f * bolt * bolt;
		rgb[1] = 0.08f * cloud + 0.08f * halo + 0.22f * halo * halo + 0.88f * bolt;
		rgb[2] = 0.5f * cloud + 0.3f * halo + 0.4f * halo * halo + 0.95f * bolt;
	}

	// ---------------------------------------------------------------- helpers

	private static void scale(float[] c, float f, float[] out) {
		out[0] = c[0] * f;
		out[1] = c[1] * f;
		out[2] = c[2] * f;
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
