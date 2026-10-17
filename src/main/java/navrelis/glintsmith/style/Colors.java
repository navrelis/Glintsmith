package navrelis.glintsmith.style;

/**
 * Colour helpers on packed ARGB ints. Pure Java.
 */
public final class Colors {
	private static final float[] SRGB_TO_LINEAR = new float[256];

	static {
		for (int i = 0; i < 256; i++) {
			float c = i / 255.0f;
			SRGB_TO_LINEAR[i] = c <= 0.04045f ? c / 12.92f : (float) Math.pow((c + 0.055) / 1.055, 2.4);
		}
	}

	private Colors() {
	}

	/**
	 * Parses {@code #RGB}, {@code #RRGGBB} or {@code #AARRGGBB} (case-insensitive, the {@code #} is optional).
	 *
	 * @return ARGB; alpha is {@code 0xFF} when the text has none
	 * @throws IllegalArgumentException on anything else
	 */
	public static int parseHex(String text) {
		if (text == null) {
			throw new IllegalArgumentException("Colour is null");
		}
		String s = text.trim();
		if (s.startsWith("#")) {
			s = s.substring(1);
		}
		int len = s.length();
		if (len != 3 && len != 6 && len != 8) {
			throw new IllegalArgumentException("Invalid colour '" + text + "': expected #RGB, #RRGGBB or #AARRGGBB");
		}
		int[] digits = new int[len];
		for (int i = 0; i < len; i++) {
			char ch = s.charAt(i);
			int d = ch < 128 ? Character.digit(ch, 16) : -1;
			if (d < 0) {
				throw new IllegalArgumentException("Invalid colour '" + text + "': bad hex digit '" + ch + "'");
			}
			digits[i] = d;
		}
		switch (len) {
			case 3:
				return 0xFF000000 | (digits[0] * 17) << 16 | (digits[1] * 17) << 8 | digits[2] * 17;
			case 6:
				return 0xFF000000 | digits[0] << 20 | digits[1] << 16 | digits[2] << 12 | digits[3] << 8 | digits[4] << 4 | digits[5];
			default:
				return digits[0] << 28 | digits[1] << 24 | digits[2] << 20 | digits[3] << 16
					| digits[4] << 12 | digits[5] << 8 | digits[6] << 4 | digits[7];
		}
	}

	/**
	 * @return {@code true} if {@link #parseHex(String)} accepts the text
	 */
	public static boolean isValidHex(String text) {
		try {
			parseHex(text);
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	/**
	 * @return {@code #RRGGBB}, or {@code #AARRGGBB} when alpha is not {@code 0xFF}; upper case
	 */
	public static String toHex(int argb) {
		if ((argb >>> 24) == 0xFF) {
			return String.format("#%06X", argb & 0xFFFFFF);
		}
		return String.format("#%08X", argb);
	}

	/**
	 * Converts HSV to opaque ARGB.
	 *
	 * @param h hue as a fraction; wraps, so any value is fine
	 * @param s saturation, clamped to 0..1
	 * @param v value, clamped to 0..1
	 */
	public static int hsv(float h, float s, float v) {
		s = Math.max(0.0f, Math.min(1.0f, s));
		v = Math.max(0.0f, Math.min(1.0f, v));
		h = h - (float) Math.floor(h);
		float h6 = h * 6.0f;
		int sector = (int) h6;
		float f = h6 - sector;
		float p = v * (1.0f - s);
		float q = v * (1.0f - s * f);
		float t = v * (1.0f - s * (1.0f - f));
		float r;
		float g;
		float b;
		switch (sector) {
			case 0 -> {
				r = v;
				g = t;
				b = p;
			}
			case 1 -> {
				r = q;
				g = v;
				b = p;
			}
			case 2 -> {
				r = p;
				g = v;
				b = t;
			}
			case 3 -> {
				r = p;
				g = q;
				b = v;
			}
			case 4 -> {
				r = t;
				g = p;
				b = v;
			}
			default -> {
				r = v;
				g = p;
				b = q;
			}
		}
		return 0xFF000000 | toByte(r) << 16 | toByte(g) << 8 | toByte(b);
	}

	/**
	 * Interpolates in OkLab (perceptually even, no muddy midpoints). Alpha is interpolated linearly.
	 * {@code t <= 0} returns {@code a} and {@code t >= 1} returns {@code b} exactly.
	 */
	public static int lerpOklab(int a, int b, float t) {
		if (!(t > 0.0f)) {
			return a;
		}
		if (t >= 1.0f) {
			return b;
		}
		float lr = SRGB_TO_LINEAR[a >> 16 & 0xFF];
		float lg = SRGB_TO_LINEAR[a >> 8 & 0xFF];
		float lb = SRGB_TO_LINEAR[a & 0xFF];
		float l1 = (float) Math.cbrt(0.4122214708f * lr + 0.5363325363f * lg + 0.0514459929f * lb);
		float m1 = (float) Math.cbrt(0.2119034982f * lr + 0.6806995451f * lg + 0.1073969566f * lb);
		float s1 = (float) Math.cbrt(0.0883024619f * lr + 0.2817188376f * lg + 0.6299787005f * lb);

		float rr = SRGB_TO_LINEAR[b >> 16 & 0xFF];
		float rg = SRGB_TO_LINEAR[b >> 8 & 0xFF];
		float rb = SRGB_TO_LINEAR[b & 0xFF];
		float l2 = (float) Math.cbrt(0.4122214708f * rr + 0.5363325363f * rg + 0.0514459929f * rb);
		float m2 = (float) Math.cbrt(0.2119034982f * rr + 0.6806995451f * rg + 0.1073969566f * rb);
		float s2 = (float) Math.cbrt(0.0883024619f * rr + 0.2817188376f * rg + 0.6299787005f * rb);

		float l = l1 + (l2 - l1) * t;
		float m = m1 + (m2 - m1) * t;
		float s = s1 + (s2 - s1) * t;

		float l3 = l * l * l;
		float m3 = m * m * m;
		float s3 = s * s * s;
		float r = 4.0767416621f * l3 - 3.3077115913f * m3 + 0.2309699292f * s3;
		float g = -1.2684380046f * l3 + 2.6097574011f * m3 - 0.3413193965f * s3;
		float bl = -0.0041960863f * l3 - 0.7034186147f * m3 + 1.7076147010f * s3;

		int alphaA = a >>> 24;
		int alphaB = b >>> 24;
		int alpha = Math.round(alphaA + (alphaB - alphaA) * t);
		return alpha << 24 | linearToByte(r) << 16 | linearToByte(g) << 8 | linearToByte(bl);
	}

	/**
	 * Replaces the alpha channel.
	 *
	 * @param alpha 0..1
	 */
	public static int withAlpha(int argb, float alpha) {
		return toByte(alpha) << 24 | argb & 0xFFFFFF;
	}

	/**
	 * Multiplies the existing alpha channel by {@code factor} (0..1).
	 */
	public static int multiplyAlpha(int argb, float factor) {
		if (factor >= 1.0f) {
			return argb;
		}
		int alpha = argb >>> 24;
		return toByte(alpha / 255.0f * factor) << 24 | argb & 0xFFFFFF;
	}

	/**
	 * Scales the RGB channels by {@code factor} (0..1), keeping alpha.
	 */
	static int scaleRgb(int argb, float factor) {
		int r = Math.round((argb >> 16 & 0xFF) * factor);
		int g = Math.round((argb >> 8 & 0xFF) * factor);
		int b = Math.round((argb & 0xFF) * factor);
		return argb & 0xFF000000 | r << 16 | g << 8 | b;
	}

	private static int toByte(float v) {
		if (!(v > 0.0f)) {
			return 0;
		}
		return v >= 1.0f ? 255 : Math.round(v * 255.0f);
	}

	private static int linearToByte(float c) {
		if (!(c > 0.0f)) {
			return 0;
		}
		if (c >= 1.0f) {
			return 255;
		}
		float s = c <= 0.0031308f ? 12.92f * c : (float) (1.055 * Math.pow(c, 1.0 / 2.4) - 0.055);
		return toByte(s);
	}
}
