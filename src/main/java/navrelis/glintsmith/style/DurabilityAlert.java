package navrelis.glintsmith.style;

/**
 * Pulses an outline towards an alert colour when an item is nearly broken. Mutable POJO for Gson.
 */
public final class DurabilityAlert {
	public static final String DEFAULT_COLOR = "#FF2A2A";
	private static final int DEFAULT_ARGB = 0xFFFF2A2A;
	private static final double PULSE_PERIOD = 1.2;

	public boolean enabled = true;
	/** Remaining durability fraction (0..1) at or below which the alert shows. */
	public float threshold = 0.10f;
	public String color = DEFAULT_COLOR;

	private transient volatile Parsed parsed;

	public DurabilityAlert() {
	}

	/**
	 * @return an independent copy
	 */
	public DurabilityAlert copy() {
		DurabilityAlert c = new DurabilityAlert();
		c.enabled = enabled;
		c.threshold = threshold;
		c.color = color;
		return c;
	}

	/**
	 * Clamps the threshold to 0..1 and replaces an invalid colour by the default.
	 *
	 * @return {@code this}
	 */
	public DurabilityAlert sanitize() {
		threshold = Num.clamp(threshold, 0.0f, 1.0f, 0.10f);
		if (color == null || !Colors.isValidHex(color)) {
			color = DEFAULT_COLOR;
		}
		parsed = null;
		return this;
	}

	/**
	 * @param baseArgb the colour to alert on top of
	 * @param remainingFraction remaining durability, 1 = new, 0 = about to break
	 * @return the base colour, or while at or below the threshold a colour pulsing between base and alert colour
	 *         (blend factor {@code 0.55 + 0.45 sin(2 pi t / 1.2)}); the alpha of the base is kept
	 */
	public int apply(int baseArgb, float remainingFraction, double timeSeconds) {
		if (!enabled || !(remainingFraction <= threshold)) {
			return baseArgb;
		}
		float factor = (float) (0.55 + 0.45 * Math.sin(2.0 * Math.PI * timeSeconds / PULSE_PERIOD));
		int alert = Colors.withAlpha(alertArgb(), (baseArgb >>> 24) / 255.0f);
		return Colors.lerpOklab(baseArgb, alert, factor);
	}

	private int alertArgb() {
		String source = color;
		Parsed p = parsed;
		if (p == null || p.source != source) {
			int argb;
			try {
				argb = Colors.parseHex(source);
			} catch (IllegalArgumentException e) {
				argb = DEFAULT_ARGB;
			}
			p = new Parsed(source, argb);
			parsed = p;
		}
		return p.argb;
	}

	private record Parsed(String source, int argb) {
	}
}
