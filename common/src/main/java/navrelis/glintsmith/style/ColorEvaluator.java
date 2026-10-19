package navrelis.glintsmith.style;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Evaluates a {@link ColorStyle} at a point in time. Pure Java, allocation free once compiled.
 * <p>
 * All animation positions are {@code time * speed}, so speed 0 freezes the colour at {@code t = 0}.
 * <ul>
 * <li>STATIC: the first colour.</li>
 * <li>RAINBOW: hue = frac(p / 4), i.e. one cycle per 4 s at speed 1; uses saturation and brightness.</li>
 * <li>PULSE with one colour: brightness follows a cosine between 100 % (at t = 0) and 35 %, period 2 s / speed.
 * With two or more colours: OkLab blend between the first two with {@code 0.5 - 0.5 cos(2 pi p / 2)}.</li>
 * <li>GRADIENT: every colour in order, looping back to the first, 1.5 s / speed per segment,
 * smoothstep easing inside the segment, OkLab interpolation.</li>
 * <li>ENCHANTMENT: GRADIENT over the enchantment colours; empty falls back to the first colour,
 * a single entry is returned as is.</li>
 * </ul>
 */
public final class ColorEvaluator {
	private static final int DEFAULT_COLOR = 0xFFD088FC;
	private static final double RAINBOW_PERIOD = 4.0;
	private static final double PULSE_PERIOD = 2.0;
	private static final double GRADIENT_SEGMENT = 1.5;
	private static final float PULSE_MIN = 0.35f;

	private ColorEvaluator() {
	}

	/**
	 * Parses the colours of the style once. Invalid colours are skipped; an empty result becomes the default colour.
	 */
	public static Compiled compile(ColorStyle style) {
		if (style == null) {
			return new Compiled(ColorMode.STATIC, 1.0f, 1.0f, 1.0f, new int[] {DEFAULT_COLOR});
		}
		return new Compiled(
			style.mode == null ? ColorMode.STATIC : style.mode,
			Num.clamp(style.speed, 0.0f, 10.0f, 1.0f),
			Num.clamp(style.saturation, 0.0f, 1.0f, 1.0f),
			Num.clamp(style.brightness, 0.0f, 1.0f, 1.0f),
			parseAll(style.colors)
		);
	}

	/**
	 * Convenience for one-off evaluation.
	 *
	 * @param enchantmentColors ARGB colours of the enchantments, may be null or empty
	 * @param parsedColors the pre-parsed colours of the style, or null to parse them now (allocates)
	 */
	public static int evaluate(ColorStyle style, double timeSeconds, int[] enchantmentColors, int[] parsedColors) {
		int[] colors = parsedColors != null ? parsedColors : parseAll(style.colors);
		return eval(
			style.mode == null ? ColorMode.STATIC : style.mode,
			Num.clamp(style.speed, 0.0f, 10.0f, 1.0f),
			Num.clamp(style.saturation, 0.0f, 1.0f, 1.0f),
			Num.clamp(style.brightness, 0.0f, 1.0f, 1.0f),
			colors, timeSeconds, enchantmentColors
		);
	}

	private static int[] parseAll(List<String> hex) {
		List<Integer> parsed = new ArrayList<>();
		if (hex != null) {
			for (String s : hex) {
				try {
					parsed.add(Colors.parseHex(s));
				} catch (IllegalArgumentException ignored) {
					// skipped, sanitize() removes these from the stored config
				}
			}
		}
		if (parsed.isEmpty()) {
			return new int[] {DEFAULT_COLOR};
		}
		int[] out = new int[parsed.size()];
		for (int i = 0; i < out.length; i++) {
			out[i] = parsed.get(i);
		}
		return out;
	}

	private static int eval(ColorMode mode, float speed, float saturation, float brightness, int[] colors,
		double time, int[] enchantmentColors) {
		double p = time * speed;
		int first = colors.length > 0 ? colors[0] : DEFAULT_COLOR;
		switch (mode) {
			case RAINBOW:
				return Colors.hsv((float) frac(p / RAINBOW_PERIOD), saturation, brightness);
			case PULSE:
				if (colors.length >= 2) {
					float t = (float) (0.5 - 0.5 * Math.cos(2.0 * Math.PI * p / PULSE_PERIOD));
					return Colors.lerpOklab(colors[0], colors[1], t);
				} else {
					double wave = 0.5 + 0.5 * Math.cos(2.0 * Math.PI * p / PULSE_PERIOD);
					return Colors.scaleRgb(first, (float) (PULSE_MIN + (1.0 - PULSE_MIN) * wave));
				}
			case GRADIENT:
				return gradient(colors, p / GRADIENT_SEGMENT, first);
			case ENCHANTMENT:
				if (enchantmentColors == null || enchantmentColors.length == 0) {
					return first;
				}
				return gradient(enchantmentColors, p / GRADIENT_SEGMENT, first);
			case STATIC:
			default:
				return first;
		}
	}

	private static int gradient(int[] colors, double position, int fallback) {
		int n = colors.length;
		if (n == 0) {
			return fallback;
		}
		if (n == 1) {
			return colors[0];
		}
		double floor = Math.floor(position);
		int index = (int) Math.floorMod((long) floor, (long) n);
		float f = (float) (position - floor);
		float eased = f * f * (3.0f - 2.0f * f);
		return Colors.lerpOklab(colors[index], colors[(index + 1) % n], eased);
	}

	private static double frac(double v) {
		return v - Math.floor(v);
	}

	/**
	 * A {@link ColorStyle} with its colours parsed. Immutable; evaluation does no parsing and no allocation.
	 */
	public static final class Compiled {
		private final ColorMode mode;
		private final float speed;
		private final float saturation;
		private final float brightness;
		private final int[] colors;

		private Compiled(ColorMode mode, float speed, float saturation, float brightness, int[] colors) {
			this.mode = mode;
			this.speed = speed;
			this.saturation = saturation;
			this.brightness = brightness;
			this.colors = colors;
		}

		/**
		 * @param enchantmentColors ARGB colours for {@link ColorMode#ENCHANTMENT}; may be null
		 * @return ARGB
		 */
		public int evaluate(double timeSeconds, int[] enchantmentColors) {
			return eval(mode, speed, saturation, brightness, colors, timeSeconds, enchantmentColors);
		}

		/**
		 * @return {@code true} if the colour changes over time (so callers can skip per-frame work for static colours)
		 */
		public boolean isAnimated() {
			if (mode == ColorMode.STATIC || !(speed > 0.0f)) {
				return false;
			}
			return mode != ColorMode.GRADIENT || colors.length > 1;
		}

		/**
		 * @return {@code true} if the colour depends on the enchantments of the stack
		 */
		public boolean usesEnchantmentColors() {
			return mode == ColorMode.ENCHANTMENT;
		}

		public ColorMode mode() {
			return mode;
		}

		/**
		 * @return the first parsed colour (ARGB)
		 */
		public int firstColor() {
			return colors[0];
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (!(o instanceof Compiled other)) {
				return false;
			}
			return mode == other.mode
				&& Float.compare(speed, other.speed) == 0
				&& Float.compare(saturation, other.saturation) == 0
				&& Float.compare(brightness, other.brightness) == 0
				&& Arrays.equals(colors, other.colors);
		}

		@Override
		public int hashCode() {
			int h = mode.hashCode();
			h = 31 * h + Float.hashCode(speed);
			h = 31 * h + Float.hashCode(saturation);
			h = 31 * h + Float.hashCode(brightness);
			return 31 * h + Arrays.hashCode(colors);
		}

		@Override
		public String toString() {
			return "Compiled{" + mode + " speed=" + speed + " colors=" + colors.length + '}';
		}
	}
}
