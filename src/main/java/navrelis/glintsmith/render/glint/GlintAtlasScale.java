package navrelis.glintsmith.render.glint;

import net.minecraft.resources.ResourceLocation;

/**
 * The atlas factor of the glint scale (design 5.4.1). Item glints are textured through the item's UVs on the block
 * atlas, so the pattern density on an item depends on how much of the atlas the item covers: in a pack with a large
 * block atlas each item covers a tiny UV range and the pattern becomes a flat wash. The factor, applied to the glint
 * scale per axis, brings that UV range back to what it is in vanilla 1.21.1.
 * <p>
 * {@code factor = (reference atlas size / actual atlas size) * (actual reference sprite size / 16)}, separately for u
 * and v. The first term is the atlas ratio; the second keeps high-resolution packs neutral (a 32x pack doubles both the
 * atlas and the sprites, so the UV range of an item, and the vanilla look, do not change).
 * <p>
 * The values are taken when the block atlas is uploaded (G10, after every resource reload); before the first upload
 * both factors are 1. Written by the upload hook and read by {@link GlintSystem} on the render thread.
 */
final class GlintAtlasScale {
	/**
	 * Size of the vanilla 1.21.1 block atlas at the default mipmap level 4: 1024 x 512. Computed by running the vanilla
	 * {@code Stitcher} over the 1684 sprites that {@code atlases/blocks.json} of the 1.21.1 client jar lists (the
	 * {@code block}, {@code item} and {@code entity/conduit} folders, three single sprites, 56 trim permutations and
	 * {@code missingno}); development runs with Fabric API log the same {@code Created: 1024x512x4} line.
	 */
	static final int REFERENCE_WIDTH = 1024;
	static final int REFERENCE_HEIGHT = 512;
	/** Size of the reference sprite in the vanilla atlas. */
	static final int REFERENCE_SPRITE = 16;
	/** A sprite every block atlas contains; its size tells the resolution of the resource pack. */
	static final ResourceLocation REFERENCE_SPRITE_ID = ResourceLocation.withDefaultNamespace("item/diamond_sword");
	/** Bounds of a factor, so a broken atlas size can never produce an absurd matrix. */
	static final float MIN_FACTOR = 1.0f / 64.0f;
	static final float MAX_FACTOR = 64.0f;

	private static float u = 1.0f;
	private static float v = 1.0f;

	private GlintAtlasScale() {
	}

	/**
	 * One axis of the factor. Pure.
	 *
	 * @param referenceAtlas  the vanilla atlas size on this axis
	 * @param actualAtlas     the current atlas size on this axis
	 * @param referenceSprite the vanilla size of the reference sprite on this axis
	 * @param actualSprite    the current size of the reference sprite on this axis
	 * @return the factor, 1 if any size is not positive, clamped to {@link #MIN_FACTOR}..{@link #MAX_FACTOR}
	 */
	static float factor(int referenceAtlas, int actualAtlas, int referenceSprite, int actualSprite) {
		if (referenceAtlas <= 0 || actualAtlas <= 0 || referenceSprite <= 0 || actualSprite <= 0) {
			return 1.0f;
		}
		double f = (double) referenceAtlas / actualAtlas * ((double) actualSprite / referenceSprite);
		return (float) Math.max(MIN_FACTOR, Math.min(MAX_FACTOR, f));
	}

	/**
	 * Stores the factors of a freshly uploaded block atlas.
	 *
	 * @param spriteWidth  width of {@link #REFERENCE_SPRITE_ID} in it, or 0 if it is missing (the sprite term is then 1)
	 * @param spriteHeight height of it, or 0
	 * @return true if a factor changed
	 */
	static boolean update(int atlasWidth, int atlasHeight, int spriteWidth, int spriteHeight) {
		float nu = factor(REFERENCE_WIDTH, atlasWidth, REFERENCE_SPRITE, spriteWidth > 0 ? spriteWidth : REFERENCE_SPRITE);
		float nv = factor(REFERENCE_HEIGHT, atlasHeight, REFERENCE_SPRITE, spriteHeight > 0 ? spriteHeight : REFERENCE_SPRITE);
		boolean changed = nu != u || nv != v;
		u = nu;
		v = nv;
		return changed;
	}

	/**
	 * @return the factor along u (the atlas width)
	 */
	static float u() {
		return u;
	}

	/**
	 * @return the factor along v (the atlas height)
	 */
	static float v() {
		return v;
	}

	/** Test hook: the state before the first upload. */
	static void resetForTests() {
		u = 1.0f;
		v = 1.0f;
	}
}
