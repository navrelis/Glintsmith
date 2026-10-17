package navrelis.glintsmith.render.glint;

import net.minecraft.resources.ResourceLocation;

/**
 * The look of one glint draw: what a {@link GlintSlot} reads at draw time. Mutable and reused.
 */
final class GlintParams {
	/** Tint value meaning "no tint". */
	static final int NO_TINT = 0xFFFFFFFF;
	static final float VANILLA_ROTATION = 10.0f;

	ResourceLocation texture;
	int tintArgb = NO_TINT;
	float speed = 1.0f;
	float scale = 1.0f;
	float strength = 1.0f;
	float rotationDeg = VANILLA_ROTATION;

	/**
	 * @return true if both describe the same look. Floats compare with {@code ==}; textures by value (the resolver hands
	 *         out the same instances, so this is nearly always an identity hit)
	 */
	boolean sameAs(GlintParams o) {
		return o != null
			&& tintArgb == o.tintArgb
			&& speed == o.speed
			&& scale == o.scale
			&& strength == o.strength
			&& rotationDeg == o.rotationDeg
			&& (texture == o.texture || (texture != null && texture.equals(o.texture)));
	}

	void copyFrom(GlintParams o) {
		texture = o.texture;
		tintArgb = o.tintArgb;
		speed = o.speed;
		scale = o.scale;
		strength = o.strength;
		rotationDeg = o.rotationDeg;
	}

	void set(ResourceLocation texture, int tintArgb, float speed, float scale, float strength, float rotationDeg) {
		this.texture = texture;
		this.tintArgb = tintArgb;
		this.speed = speed;
		this.scale = scale;
		this.strength = strength;
		this.rotationDeg = rotationDeg;
	}

	/**
	 * @return true if drawing with these params is indistinguishable from the vanilla glint of {@code kind}
	 */
	boolean isVanilla(GlintKind kind) {
		return tintArgb == NO_TINT
			&& speed == 1.0f
			&& scale == 1.0f
			&& strength == 1.0f
			&& rotationDeg == VANILLA_ROTATION
			&& texture != null
			&& texture.equals(kind.vanillaTexture());
	}
}
