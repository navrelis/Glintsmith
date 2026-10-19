package navrelis.glintsmith.style;

/**
 * A fully resolved glint. Every value is already merged: the category defaults overlaid by all matching rules
 * in order, including {@code rotation}; the renderer must not apply further defaults or merging.
 *
 * @param enabled {@code false} hides the glint
 * @param texture a texture spec as in {@link GlintSettings#validateTexture(String)}; null for the vanilla texture
 * @param tint compiled tint, or null for none
 * @param tintStyle the style the tint was compiled from, or null
 * @param speed 0..10, 1 = vanilla
 * @param scale 0.1..10, 1 = vanilla
 * @param strength 0..1
 * @param rotation degrees, 10 = vanilla
 */
public record GlintLook(
	boolean enabled,
	String texture,
	ColorEvaluator.Compiled tint,
	ColorStyle tintStyle,
	float speed,
	float scale,
	float strength,
	float rotation
) {
	/**
	 * A 64-bit key that is equal for equal looks, usable as a render or texture cache key.
	 * (The record itself is a valid key too, this one just avoids holding the references.)
	 */
	public long lookKey() {
		long h = enabled ? 1L : 0L;
		h = mix(h, texture == null ? 0 : texture.hashCode());
		h = mix(h, tint == null ? 0 : tint.hashCode());
		h = mix(h, Float.floatToIntBits(speed));
		h = mix(h, Float.floatToIntBits(scale));
		h = mix(h, Float.floatToIntBits(strength));
		h = mix(h, Float.floatToIntBits(rotation));
		return h;
	}

	private static long mix(long h, int v) {
		h ^= v * 0x9E3779B97F4A7C15L;
		h ^= h >>> 32;
		h *= 0xD6E8FEB86659FD93L;
		return h ^ h >>> 29;
	}
}
