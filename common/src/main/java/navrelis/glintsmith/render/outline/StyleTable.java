package navrelis.glintsmith.render.outline;

/**
 * Per-composite table of the distinct outline styles on screen. Each style gets an index 1..255 that the mask
 * stores in its red channel; the composite shader looks the style up in a 256x2 texture built from this table
 * ({@link StyleTexture}).
 * <p>
 * Pure Java, no GL, render thread only. Lookups and inserts allocate nothing.
 */
final class StyleTable {
	/** Index 0 means "no style", so at most 255 distinct styles fit in one composite. */
	static final int MAX_STYLES = 255;
	/** Hard cap of the search radius of the distance transform, in pixels. */
	static final int MAX_RADIUS = 32;
	static final float MIN_THICKNESS = 0.5f;
	/** Thickness is stored as thickness * 8 in one byte. */
	static final float MAX_ENCODED_THICKNESS = 255.0f / 8.0f;

	private static final int CAPACITY = 512; // power of two, load factor <= 0.5
	private static final int MASK = CAPACITY - 1;

	private final int[] slotColor = new int[CAPACITY];
	private final int[] slotParams = new int[CAPACITY];
	private final short[] slotIndex = new short[CAPACITY]; // 0 = empty slot
	private final int[] usedSlots = new int[MAX_STYLES];

	private final int[] colors = new int[MAX_STYLES + 1];
	private final int[] params = new int[MAX_STYLES + 1];
	private int count;
	private int maxRadius = 1;
	private int rejected;

	/**
	 * The index for a style, inserting it if it is new.
	 *
	 * @param argb outline colour, alpha already multiplied by the opacity
	 * @param thickness in framebuffer pixels (already scaled); clamped by {@link #clampThickness}
	 * @param softness 0..1
	 * @return 1..255, or 0 if the colour is fully transparent or the table is full and holds no style of this colour
	 *         (a full table answers a new width of a known colour with its nearest style of that colour)
	 */
	int indexFor(int argb, float thickness, float softness) {
		if ((argb >>> 24) == 0) {
			return 0;
		}
		int packed = packParams(thickness, softness);
		int h = mix(argb, packed) & MASK;
		while (true) {
			int index = slotIndex[h];
			if (index == 0) {
				break;
			}
			if (slotColor[h] == argb && slotParams[h] == packed) {
				return index;
			}
			h = (h + 1) & MASK;
		}
		if (count >= MAX_STYLES) {
			rejected++;
			return nearestOfColor(argb, packed);
		}
		int index = ++count;
		slotColor[h] = argb;
		slotParams[h] = packed;
		slotIndex[h] = (short) index;
		usedSlots[index - 1] = h;
		colors[index] = argb;
		params[index] = packed;
		int radius = radiusOf(packed);
		if (radius > maxRadius) {
			maxRadius = radius;
		}
		return index;
	}

	/**
	 * For a full table: the style of the same colour whose thickness and softness are closest to {@code packed}, so
	 * that an item of a look already on screen keeps an outline (only slightly wider or narrower) when proportional
	 * thickness has filled the table with many widths of it. Linear, but only runs while the table is full.
	 *
	 * @return 1..255, or 0 if no style has this colour
	 */
	private int nearestOfColor(int argb, int packed) {
		int best = 0;
		int bestDistance = Integer.MAX_VALUE;
		int t8 = packed & 0xFF;
		int s8 = (packed >>> 8) & 0xFF;
		for (int index = 1; index <= count; index++) {
			if (colors[index] != argb) {
				continue;
			}
			int p = params[index];
			int distance = Math.abs((p & 0xFF) - t8) + Math.abs(((p >>> 8) & 0xFF) - s8);
			if (distance < bestDistance) {
				bestDistance = distance;
				best = index;
			}
		}
		return best;
	}

	/**
	 * @return number of styles in the table (the highest index in use)
	 */
	int count() {
		return count;
	}

	/**
	 * @return the search radius the distance transform needs for every style in the table, 1..32
	 */
	int maxRadius() {
		return maxRadius;
	}

	/**
	 * @return styles that did not fit since the last reset
	 */
	int rejected() {
		return rejected;
	}

	int color(int index) {
		return colors[index];
	}

	/** Thickness * 8, 4..255. */
	int thicknessByte(int index) {
		return params[index] & 0xFF;
	}

	/** Softness * 255, 0..255. */
	int softnessByte(int index) {
		return (params[index] >>> 8) & 0xFF;
	}

	/**
	 * Row 0 of the style texture: the colour, as the ABGR int {@code NativeImage.setPixelRGBA} expects.
	 */
	int colorPixel(int index) {
		int c = colors[index];
		int a = (c >>> 24) & 0xFF;
		int r = (c >>> 16) & 0xFF;
		int g = (c >>> 8) & 0xFF;
		int b = c & 0xFF;
		return (a << 24) | (b << 16) | (g << 8) | r;
	}

	/**
	 * Row 1 of the style texture: R = thickness * 8, G = softness * 255, A = 255, as ABGR.
	 */
	int paramPixel(int index) {
		return 0xFF000000 | (softnessByte(index) << 8) | thicknessByte(index);
	}

	/**
	 * Empties the table. Only the used slots are touched.
	 */
	void reset() {
		for (int i = 0; i < count; i++) {
			int h = usedSlots[i];
			slotIndex[h] = 0;
		}
		count = 0;
		maxRadius = 1;
		rejected = 0;
	}

	/**
	 * Clamps a thickness so the outline, including its soft edge, fits inside {@link #MAX_RADIUS}:
	 * {@code t * (1 + s) + 1 <= 32}, and so it can be encoded ({@code t * 8 <= 255}). Minimum 0.5.
	 */
	static float clampThickness(float thickness, float softness) {
		float s = clampSoftness(softness);
		float t = Float.isFinite(thickness) ? thickness : MIN_THICKNESS;
		float max = Math.min(MAX_ENCODED_THICKNESS, (MAX_RADIUS - 1.0f) / (1.0f + s));
		if (t > max) {
			t = max;
		}
		if (t < MIN_THICKNESS) {
			t = MIN_THICKNESS;
		}
		return t;
	}

	static float clampSoftness(float softness) {
		if (!(softness > 0.0f)) {
			return 0.0f;
		}
		return Math.min(softness, 1.0f);
	}

	/** Quantises thickness and softness into one int: bits 0-7 thickness * 8, bits 8-15 softness * 255. */
	static int packParams(float thickness, float softness) {
		int s8 = Math.max(0, Math.min(255, Math.round(clampSoftness(softness) * 255.0f)));
		// Clamp against the softness the shader will see, so the decoded values still fit the radius cap.
		float s = s8 / 255.0f;
		float t = clampThickness(thickness, s);
		int maxT8 = (int) Math.floor(Math.min(MAX_ENCODED_THICKNESS, (MAX_RADIUS - 1.0f) / (1.0f + s)) * 8.0f);
		int t8 = Math.max(4, Math.min(maxT8, Math.round(t * 8.0f)));
		return t8 | (s8 << 8);
	}

	/** {@code ceil(t * (1 + s) + 1)}, 1..32, from the quantised values the shader sees. */
	static int radiusOf(int packed) {
		float t = (packed & 0xFF) / 8.0f;
		float s = ((packed >>> 8) & 0xFF) / 255.0f;
		int radius = (int) Math.ceil(t * (1.0f + s) + 1.0f);
		return Math.max(1, Math.min(MAX_RADIUS, radius));
	}

	private static int mix(int a, int b) {
		int h = a * 0x9E3779B1 + b * 0x85EBCA6B;
		h ^= h >>> 15;
		h *= 0x2C1B3C6D;
		h ^= h >>> 12;
		return h;
	}
}
