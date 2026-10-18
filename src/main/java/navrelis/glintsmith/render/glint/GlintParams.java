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
	/** Atlas factor of the scale along u and v ({@link GlintAtlasScale}); 1 = none. */
	float atlasU = 1.0f;
	float atlasV = 1.0f;

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
			&& atlasU == o.atlasU
			&& atlasV == o.atlasV
			&& (texture == o.texture || (texture != null && texture.equals(o.texture)));
	}

	void copyFrom(GlintParams o) {
		texture = o.texture;
		tintArgb = o.tintArgb;
		speed = o.speed;
		scale = o.scale;
		strength = o.strength;
		rotationDeg = o.rotationDeg;
		atlasU = o.atlasU;
		atlasV = o.atlasV;
	}

	/**
	 * Sets the look and resets the atlas factor to 1 (see {@link #setAtlas}).
	 */
	void set(ResourceLocation texture, int tintArgb, float speed, float scale, float strength, float rotationDeg) {
		this.texture = texture;
		this.tintArgb = tintArgb;
		this.speed = speed;
		this.scale = scale;
		this.strength = strength;
		this.rotationDeg = rotationDeg;
		this.atlasU = 1.0f;
		this.atlasV = 1.0f;
	}

	void setAtlas(float atlasU, float atlasV) {
		this.atlasU = atlasU;
		this.atlasV = atlasV;
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
			&& atlasU == 1.0f
			&& atlasV == 1.0f
			&& texture != null
			&& texture.equals(kind.vanillaTexture());
	}
}
