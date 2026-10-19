package navrelis.glintsmith.style;

/**
 * A fully resolved outline for one stack and target.
 *
 * @param color the compiled colour animation
 * @param colorStyle the style it was compiled from
 * @param thickness screen pixels, 0.5..16
 * @param softness 0..1
 * @param opacity 0..1, multiplied onto the colour alpha
 */
public record OutlineLook(
	ColorEvaluator.Compiled color,
	ColorStyle colorStyle,
	float thickness,
	float softness,
	float opacity
) {
}
