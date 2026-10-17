package navrelis.glintsmith.render.glint;

/**
 * Deterministic, tileable noise helpers for the procedural glint presets. Pure Java, no Minecraft classes.
 * <p>
 * Every "pixel" function works on a tile of {@link #TILE} pixels. Lattice indices wrap, so all functions are periodic
 * with period {@link #TILE} in both axes: {@code f(x + 128, y) == f(x, y)} up to float rounding.
 * Transcendental functions use {@link StrictMath} so the result is identical on every platform.
 */
final class GlintNoise {
	/** Edge length of a preset tile in pixels. */
	static final int TILE = 128;

	private GlintNoise() {
	}

	/**
	 * Integer hash to a float in [0, 1).
	 */
	static float hash(int x, int y, int seed) {
		int h = x * 374761393 + y * 668265263 + seed * 1442695041;
		h = (h ^ (h >>> 13)) * 1274126177;
		h ^= h >>> 16;
		return (h & 0xFFFFFF) / 16777216.0f;
	}

	static float smooth(float t) {
		return t * t * (3.0f - 2.0f * t);
	}

	static float smoothstep(float edge0, float edge1, float x) {
		float t = (x - edge0) / (edge1 - edge0);
		if (t <= 0.0f) {
			return 0.0f;
		}
		if (t >= 1.0f) {
			return 1.0f;
		}
		return t * t * (3.0f - 2.0f * t);
	}

	static float lerp(float t, float a, float b) {
		return a + t * (b - a);
	}

	/**
	 * Value noise in [0, 1) with {@code px} by {@code py} lattice cells across one tile.
	 */
	static float value(float x, float y, int px, int py, int seed) {
		float cx = x * px / TILE;
		float cy = y * py / TILE;
		int ix = (int) Math.floor(cx);
		int iy = (int) Math.floor(cy);
		float fx = smooth(cx - ix);
		float fy = smooth(cy - iy);
		int x0 = Math.floorMod(ix, px);
		int x1 = Math.floorMod(ix + 1, px);
		int y0 = Math.floorMod(iy, py);
		int y1 = Math.floorMod(iy + 1, py);
		float a = hash(x0, y0, seed);
		float b = hash(x1, y0, seed);
		float c = hash(x0, y1, seed);
		float d = hash(x1, y1, seed);
		return lerp(fy, lerp(fx, a, b), lerp(fx, c, d));
	}

	/**
	 * Fractal sum of {@link #value} octaves; octave {@code o} uses the periods {@code px << o} and {@code py << o}.
	 * The result is in [0, 1).
	 */
	static float fbm(float x, float y, int px, int py, int octaves, int seed) {
		float sum = 0.0f;
		float amp = 1.0f;
		float norm = 0.0f;
		for (int o = 0; o < octaves; o++) {
			sum += amp * value(x, y, px << o, py << o, seed + o * 101);
			norm += amp;
			amp *= 0.5f;
		}
		return sum / norm;
	}

	/**
	 * Toroidal Worley noise: one feature point per cell of a {@code g} by {@code g} grid over the tile.
	 *
	 * @param out receives F1 and F2 (distances to the nearest and second nearest feature point, in cell units) at
	 *            indices 0 and 1; if it has at least four entries it also receives the wrapped cell x and y of the
	 *            nearest feature point at indices 2 and 3
	 */
	static void worley(float x, float y, int g, int seed, float[] out) {
		float cx = x * g / TILE;
		float cy = y * g / TILE;
		int ix = (int) Math.floor(cx);
		int iy = (int) Math.floor(cy);
		float fx = cx - ix;
		float fy = cy - iy;
		float f1 = Float.MAX_VALUE;
		float f2 = Float.MAX_VALUE;
		int bestX = 0;
		int bestY = 0;
		for (int dy = -1; dy <= 1; dy++) {
			for (int dx = -1; dx <= 1; dx++) {
				int wx = Math.floorMod(ix + dx, g);
				int wy = Math.floorMod(iy + dy, g);
				float px = dx + hash(wx, wy, seed) - fx;
				float py = dy + hash(wx, wy, seed + 7919) - fy;
				float d = (float) StrictMath.sqrt(px * px + py * py);
				if (d < f1) {
					f2 = f1;
					f1 = d;
					bestX = wx;
					bestY = wy;
				} else if (d < f2) {
					f2 = d;
				}
			}
		}
		out[0] = f1;
		out[1] = f2;
		if (out.length >= 4) {
			out[2] = bestX;
			out[3] = bestY;
		}
	}

	/**
	 * HSV to RGB, each output channel in [0, 1].
	 *
	 * @param h hue, wraps around (1 = full circle)
	 * @param out receives r, g, b
	 */
	static void hsv(float h, float s, float v, float[] out) {
		h = h - (float) Math.floor(h);
		float scaled = h * 6.0f;
		int sector = (int) scaled;
		float f = scaled - sector;
		float p = v * (1.0f - s);
		float q = v * (1.0f - s * f);
		float t = v * (1.0f - s * (1.0f - f));
		switch (sector) {
			case 0 -> set(out, v, t, p);
			case 1 -> set(out, q, v, p);
			case 2 -> set(out, p, v, t);
			case 3 -> set(out, p, q, v);
			case 4 -> set(out, t, p, v);
			default -> set(out, v, p, q);
		}
	}

	/**
	 * HSV to packed {@code 0xRRGGBB}.
	 */
	static int hsv(float h, float s, float v) {
		float[] rgb = new float[3];
		hsv(h, s, v, rgb);
		return (toByte(rgb[0], 255) << 16) | (toByte(rgb[1], 255) << 8) | toByte(rgb[2], 255);
	}

	private static void set(float[] out, float r, float g, float b) {
		out[0] = r;
		out[1] = g;
		out[2] = b;
	}

	/**
	 * Converts a [0, 1] channel to a byte, capped at {@code max}.
	 */
	static int toByte(float c, int max) {
		int v = (int) (c * 255.0f + 0.5f);
		return v < 0 ? 0 : Math.min(v, max);
	}
}
