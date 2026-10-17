package navrelis.glintsmith.style;

/**
 * Outline overrides of a rule. Every field is optional: {@code null} means "inherit".
 * Ranges: thickness 0.5..8 screen pixels, softness 0..1, opacity 0..1.
 */
public final class OutlineSettings {
	public static final float MIN_THICKNESS = 0.5f;
	public static final float MAX_THICKNESS = 8.0f;

	public Boolean enabled;
	public ColorStyle color;
	public Float thickness;
	public Float softness;
	public Float opacity;

	public OutlineSettings() {
	}

	/**
	 * @return settings that only switch the outline on or off
	 */
	public static OutlineSettings enabled(boolean enabled) {
		OutlineSettings s = new OutlineSettings();
		s.enabled = enabled;
		return s;
	}

	/**
	 * @return settings that switch the outline on with the given colour
	 */
	public static OutlineSettings colored(ColorStyle color) {
		OutlineSettings s = new OutlineSettings();
		s.enabled = true;
		s.color = color;
		return s;
	}

	/**
	 * @return an independent deep copy
	 */
	public OutlineSettings copy() {
		OutlineSettings c = new OutlineSettings();
		c.enabled = enabled;
		c.color = color == null ? null : color.copy();
		c.thickness = thickness;
		c.softness = softness;
		c.opacity = opacity;
		return c;
	}

	/**
	 * Clamps the ranges; NaN becomes "inherit".
	 *
	 * @return {@code this}
	 */
	public OutlineSettings sanitize() {
		if (color != null) {
			color.sanitize();
		}
		thickness = Num.clamp(thickness, MIN_THICKNESS, MAX_THICKNESS);
		softness = Num.clamp(softness, 0.0f, 1.0f);
		opacity = Num.clamp(opacity, 0.0f, 1.0f);
		return this;
	}
}
