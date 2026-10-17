package navrelis.glintsmith.render.outline;

/**
 * The depth rules of the outline passes, as plain Java. The shaders {@code mask.fsh}, {@code outline_edt.fsh} and
 * {@code outline_composite.fsh} implement exactly these functions; the constants are the uniform values the
 * {@link OutlineCompositor} sets. Kept in one place so the rules can be tested on the JVM.
 * <p>
 * The mask holds every item pixel, visible or not, with the depth of the nearest item surface packed into G, B and A
 * (24 bits, window depth). The mask target's depth attachment holds a copy of the scene depth. From both:
 * <ul>
 * <li>an item pixel is a <b>seed</b> only if the item is visible there: the scene is not in front of the item by more
 * than {@link #SEED_EPSILON} (window depth);</li>
 * <li>an outline pixel that lies on a <b>hidden</b> item pixel is drawn only if the scene there is clearly behind the
 * seed (by more than the cover tolerance): otherwise the item continues behind an occluder at that pixel (a blade
 * going into the arm, a sword behind a pillar), and the outline would be drawn on the occluder;</li>
 * <li>any other outline pixel is hidden if the scene is in front of the seed by more than the front tolerance (the
 * rule the pipeline always had).</li>
 * </ul>
 */
final class OutlineDepth {
	/** Largest value of the 24-bit packed depth. */
	static final int DEPTH_STEPS = 0xFFFFFF;
	/**
	 * How far, in window depth, the scene may be in front of the item at a seed while the item still counts as
	 * visible: 32 steps of a 24-bit depth buffer. It only absorbs quantisation and rounding differences between the
	 * item's own draw and the mask draw (same vertex expression, same matrices, same layering). Unlike the polygon
	 * offset the mask used before, it does not grow with the depth slope, so item pixels hidden just behind a
	 * near-coplanar surface (a blade inside the sleeve) are no seeds.
	 */
	static final float SEED_EPSILON = 32.0f / DEPTH_STEPS;
	/** Front tolerance in blocks for world outlines (dropped items next to slightly nearer floor). */
	static final float WORLD_TOLERANCE = 0.15f;
	/** Front tolerance in blocks for first-person outlines: the hand scene is under one block deep. */
	static final float HAND_TOLERANCE = 0.03f;
	/** Tolerance in blocks for outline pixels on hidden item pixels: the occluder must be this far behind the seed. */
	static final float COVER_TOLERANCE = 0.15f;
	/** Relative part of every tolerance (fraction of the seed distance). */
	static final float RELATIVE_TOLERANCE = 0.02f;

	private OutlineDepth() {
	}

	/**
	 * {@code mask.fsh}: the window depth as a 24-bit integer, rounded to nearest.
	 */
	static int pack(float windowZ) {
		float z = Math.min(Math.max(windowZ, 0.0f), 1.0f);
		// In float, 1.0 * DEPTH_STEPS + 0.5 rounds up to 2^24; clamp so the far plane does not wrap to 0.
		return Math.min((int) Math.floor(z * DEPTH_STEPS + 0.5f), DEPTH_STEPS);
	}

	/**
	 * @return the three bytes written to G, B and A (most significant first)
	 */
	static int[] bytes(int packed) {
		return new int[] { (packed >>> 16) & 0xFF, (packed >>> 8) & 0xFF, packed & 0xFF };
	}

	/**
	 * {@code unpackDepth} in the shaders: the window depth from the three mask bytes.
	 */
	static float unpack(int g, int b, int a) {
		return ((g << 16) | (b << 8) | a) / (float) DEPTH_STEPS;
	}

	/**
	 * The seed rule ({@code outline_edt.fsh} and the inside check of {@code outline_composite.fsh}).
	 *
	 * @param sceneZ window depth of the scene at the pixel
	 * @param itemZ window depth of the nearest item surface at the pixel
	 */
	static boolean visible(float sceneZ, float itemZ) {
		return sceneZ >= itemZ - SEED_EPSILON;
	}

	/**
	 * {@code linearDepth} in the composite: distance in blocks from a window depth.
	 */
	static float linear(float windowZ, float near, float far) {
		return (2.0f * near * far) / (far + near - (2.0f * windowZ - 1.0f) * (far - near));
	}

	/**
	 * The occlusion rule of the composite for an outline pixel.
	 *
	 * @param onHiddenItem the outline pixel lies on a hidden item pixel
	 * @param zp linear scene depth at the outline pixel
	 * @param zs linear scene depth at the seed (the item's depth, since seeds are visible)
	 * @param frontTolerance {@link #WORLD_TOLERANCE} or {@link #HAND_TOLERANCE}
	 * @return whether the outline pixel is hidden
	 */
	static boolean hidden(boolean onHiddenItem, float zp, float zs, float frontTolerance) {
		float relative = zs * RELATIVE_TOLERANCE;
		if (onHiddenItem) {
			return zp < zs + Math.max(COVER_TOLERANCE, relative);
		}
		return zp < zs - Math.max(frontTolerance, relative);
	}
}
