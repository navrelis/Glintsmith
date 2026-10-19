package navrelis.glintsmith.style;

/**
 * Small numeric helpers for sanitising config values. NaN never survives.
 */
public final class Num {
	private Num() {
	}

	/**
	 * Clamps to {@code [min, max]}; NaN becomes {@code fallback} (itself clamped).
	 */
	public static float clamp(float value, float min, float max, float fallback) {
		if (Float.isNaN(value)) {
			value = fallback;
		}
		return Math.max(min, Math.min(max, value));
	}

	/**
	 * Clamps a nullable override; null and NaN stay null ("inherit").
	 */
	public static Float clamp(Float value, float min, float max) {
		if (value == null || value.isNaN()) {
			return null;
		}
		return Math.max(min, Math.min(max, value));
	}
}
