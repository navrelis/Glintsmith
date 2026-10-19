package navrelis.glintsmith.style;

import java.util.List;

/**
 * Glint overrides of a rule. Every field is optional: {@code null} means "inherit" from the category defaults.
 * <ul>
 * <li>{@code enabled}: {@code false} hides the glint.</li>
 * <li>{@code texture}: a texture spec, see {@link #validateTexture(String)}. {@link #VANILLA_TEXTURE} resets to the
 * vanilla texture.</li>
 * <li>{@code tint}: colour multiplied onto the glint.</li>
 * <li>{@code speed} 0..10 (1 = vanilla), {@code scale} 0.1..10 (1 = vanilla), {@code strength} 0..1,
 * {@code rotation} in degrees -180..180 (10 = vanilla).</li>
 * </ul>
 */
public final class GlintSettings {
	/** Texture value that resets the texture to vanilla even if an earlier rule or the category set one. */
	public static final String VANILLA_TEXTURE = "vanilla";
	/** Alias of {@link #VANILLA_TEXTURE}. */
	public static final String DEFAULT_TEXTURE = "default";
	/** Names usable as {@code preset:<name>}. */
	public static final List<String> PRESETS =
		List.of("neutral", "rainbow", "aurora", "fire", "frost", "sparkle", "gold", "void",
			"inferno", "nebula", "starlight", "opal", "storm");
	public static final String PRESET_PREFIX = "preset:";
	public static final String FILE_PREFIX = "file:";

	private static final java.util.regex.Pattern FILE_NAME = java.util.regex.Pattern.compile("[a-z0-9_.-]+\\.png");
	private static final java.util.regex.Pattern RESOURCE = java.util.regex.Pattern.compile("[a-z0-9_.-]+:[a-z0-9_.-]+(?:/[a-z0-9_.-]+)+");

	/**
	 * Checks a texture spec. The grammar, shared by config, rules, UI and renderer:
	 * <ul>
	 * <li>{@code null}, {@code "vanilla"}, {@code "default"}: the vanilla glint texture.</li>
	 * <li>{@code preset:<name>} with a name from {@link #PRESETS}.</li>
	 * <li>{@code file:<name>.png}: a PNG in {@code config/glintsmith/glints/}; the name is a plain file name,
	 * {@code [a-z0-9_.-]+}, no slashes and no {@code ..}.</li>
	 * <li>anything else must be a resource location {@code namespace:path} of a texture in a resource pack,
	 * for example {@code mymod:textures/misc/glint.png}. The path needs at least one folder, so {@code mymod:glint.png}
	 * is rejected.</li>
	 * </ul>
	 *
	 * @return null if valid, otherwise a short English error message
	 */
	public static String validateTexture(String spec) {
		if (spec == null || isVanillaTexture(spec)) {
			return null;
		}
		if (spec.startsWith(PRESET_PREFIX)) {
			String name = spec.substring(PRESET_PREFIX.length());
			return PRESETS.contains(name) ? null : "Unknown glint preset '" + name + "' (expected one of " + PRESETS + ")";
		}
		if (spec.startsWith(FILE_PREFIX)) {
			String name = spec.substring(FILE_PREFIX.length());
			if (name.contains("..") || !FILE_NAME.matcher(name).matches()) {
				return "Invalid glint file name '" + name + "' (use lower case letters, digits, _ . - and end with .png)";
			}
			return null;
		}
		if (RESOURCE.matcher(spec).matches() && !spec.contains("..") && spec.indexOf(':') == spec.lastIndexOf(':')) {
			return null;
		}
		return "Invalid glint texture '" + spec + "' (expected vanilla, preset:<name>, file:<name>.png or namespace:folder/path.png)";
	}

	/**
	 * @return true for null, {@code "vanilla"} and {@code "default"}
	 */
	public static boolean isVanillaTexture(String spec) {
		return spec == null || VANILLA_TEXTURE.equals(spec) || DEFAULT_TEXTURE.equals(spec);
	}

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
