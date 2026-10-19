package navrelis.glintsmith.style;

/**
 * What to draw for one stack, target and glint category.
 *
 * @param outline the outline, or null for none
 * @param glint the glint, or null to leave the vanilla/category defaults untouched
 */
public record ResolvedStyle(OutlineLook outline, GlintLook glint) {
	/** Nothing to draw, nothing to change. */
	public static final ResolvedStyle NONE = new ResolvedStyle(null, null);
}
