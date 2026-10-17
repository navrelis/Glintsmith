package navrelis.glintsmith.style;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A colour that may change over time. Mutable POJO, serialised by Gson.
 * Evaluate it through {@link ColorEvaluator#compile(ColorStyle)}; the strings here are only the stored form.
 */
public final class ColorStyle {
	public static final String DEFAULT_COLOR = "#D088FC";
	public static final int MAX_COLORS = 16;

	public ColorMode mode = ColorMode.STATIC;
	public List<String> colors = new ArrayList<>(List.of(DEFAULT_COLOR));
	public float speed = 1.0f;
	public float saturation = 1.0f;
	public float brightness = 1.0f;

	public ColorStyle() {
	}

	/**
	 * @return a static style of the given hex colour (not validated here; see {@link #sanitize()})
	 */
	public static ColorStyle of(String hex) {
		ColorStyle style = new ColorStyle();
		style.colors = new ArrayList<>(List.of(hex));
		return style;
	}

	/**
	 * @return a rainbow style with the given speed
	 */
	public static ColorStyle rainbow(float speed) {
		ColorStyle style = new ColorStyle();
		style.mode = ColorMode.RAINBOW;
		style.speed = speed;
		return style;
	}

	/**
	 * @return an independent deep copy
	 */
	public ColorStyle copy() {
		ColorStyle c = new ColorStyle();
		c.mode = mode;
		c.colors = colors == null ? new ArrayList<>() : new ArrayList<>(colors);
		c.speed = speed;
		c.saturation = saturation;
		c.brightness = brightness;
		return c;
	}

	/**
	 * Clamps speed to 0..10 and saturation/brightness to 0..1, drops invalid or null colours,
	 * keeps at most {@link #MAX_COLORS} and guarantees at least one colour.
	 *
	 * @return {@code this}
	 */
	public ColorStyle sanitize() {
		if (mode == null) {
			mode = ColorMode.STATIC;
		}
		speed = Num.clamp(speed, 0.0f, 10.0f, 1.0f);
		saturation = Num.clamp(saturation, 0.0f, 1.0f, 1.0f);
		brightness = Num.clamp(brightness, 0.0f, 1.0f, 1.0f);
		List<String> valid = new ArrayList<>();
		if (colors != null) {
			for (String c : colors) {
				if (c != null && Colors.isValidHex(c) && valid.size() < MAX_COLORS) {
					valid.add(c.trim());
				}
			}
		}
		if (valid.isEmpty()) {
			valid.add(DEFAULT_COLOR);
		}
		colors = valid;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof ColorStyle other)) {
			return false;
		}
		return mode == other.mode
			&& Float.compare(speed, other.speed) == 0
			&& Float.compare(saturation, other.saturation) == 0
			&& Float.compare(brightness, other.brightness) == 0
			&& Objects.equals(colors, other.colors);
	}

	@Override
	public int hashCode() {
		return Objects.hash(mode, colors, speed, saturation, brightness);
	}

	@Override
	public String toString() {
		return "ColorStyle{" + mode + ' ' + colors + " speed=" + speed + '}';
	}
}
