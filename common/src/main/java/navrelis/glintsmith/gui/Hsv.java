package navrelis.glintsmith.gui;

/**
 * RGB to HSV conversion (the other direction is {@code Colors.hsv}). Pure Java.
 */
public final class Hsv {
	private Hsv() {
	}

	/**
	 * @param fallbackHue the hue returned when the colour is grey (hue is undefined there), 0..1
	 * @return {@code {hue, saturation, value}}, each 0..1
	 */
	public static float[] fromArgb(int argb, float fallbackHue) {
		float r = (argb >> 16 & 0xFF) / 255.0f;
		float g = (argb >> 8 & 0xFF) / 255.0f;
		float b = (argb & 0xFF) / 255.0f;
		float max = Math.max(r, Math.max(g, b));
		float min = Math.min(r, Math.min(g, b));
		float delta = max - min;
		float hue = fallbackHue;
		if (delta > 1.0e-6f) {
			if (max == r) {
				hue = ((g - b) / delta) / 6.0f;
			} else if (max == g) {
				hue = (2.0f + (b - r) / delta) / 6.0f;
			} else {
				hue = (4.0f + (r - g) / delta) / 6.0f;
			}
			hue -= (float) Math.floor(hue);
		}
		float saturation = max <= 0.0f ? 0.0f : delta / max;
		return new float[] {hue, saturation, max};
	}
}
