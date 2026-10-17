package navrelis.glintsmith.style;

/**
 * Glint overrides of a rule. Every field is optional: {@code null} means "inherit" from the category defaults.
 * <ul>
 * <li>{@code enabled}: {@code false} hides the glint.</li>
 * <li>{@code texture}: a preset id like {@code glintsmith:rainbow}, a resource location, or
 * {@code config:<file>.png}. {@link #VANILLA_TEXTURE} resets to the vanilla texture.</li>
 * <li>{@code tint}: colour multiplied onto the glint.</li>
 * <li>{@code speed} 0..10 (1 = vanilla), {@code scale} 0.1..10 (1 = vanilla), {@code strength} 0..1,
 * {@code rotation} in degrees -180..180 (10 = vanilla).</li>
 * </ul>
 */
public final class GlintSettings {
	/** Texture value that resets the texture to vanilla even if an earlier rule or the category set one. */
	public static final String VANILLA_TEXTURE = "vanilla";

	public Boolean enabled;
	public String texture;
	public ColorStyle tint;
	public Float speed;
	public Float scale;
	public Float strength;
	public Float rotation;

	public GlintSettings() {
	}

	/**
	 * @return an independent deep copy
	 */
	public GlintSettings copy() {
		GlintSettings c = new GlintSettings();
		c.enabled = enabled;
		c.texture = texture;
		c.tint = tint == null ? null : tint.copy();
		c.speed = speed;
		c.scale = scale;
		c.strength = strength;
		c.rotation = rotation;
		return c;
	}

	/**
	 * Clamps the ranges; NaN becomes "inherit"; a blank texture becomes "inherit".
	 *
	 * @return {@code this}
	 */
	public GlintSettings sanitize() {
		if (texture != null && texture.isBlank()) {
			texture = null;
		}
		if (tint != null) {
			tint.sanitize();
		}
		speed = Num.clamp(speed, 0.0f, 10.0f);
		scale = Num.clamp(scale, 0.1f, 10.0f);
		strength = Num.clamp(strength, 0.0f, 1.0f);
		rotation = Num.clamp(rotation, -180.0f, 180.0f);
		return this;
	}
}
