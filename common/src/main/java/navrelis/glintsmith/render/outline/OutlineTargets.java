package navrelis.glintsmith.render.outline;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

/**
 * The off-screen targets of the outline pipeline, created lazily at the size of the main target:
 * <ul>
 * <li>{@code mask}: RGBA8 colour (style index in red, the item's own window depth packed into green, blue and
 * alpha) plus a vanilla float depth texture holding a copy of the scene depth (after the mask draw),</li>
 * <li>{@code edt}: RGBA8 result of the horizontal distance pass,</li>
 * <li>{@code worldMask}: a second mask, only used for world outlines under a shader pack. Their mask is drawn in the
 * middle of the level render but composited after the pack's final pass, at the same point as the first-person
 * mask, which lives in {@code mask} from the hand pass until then.</li>
 * </ul>
 * Each is freed after {@link #IDLE_FRAMES} frames without use. Render thread only.
 */
final class OutlineTargets {
	static final int IDLE_FRAMES = 600;

	@Nullable
	private static TextureTarget mask;
	@Nullable
	private static TextureTarget edt;
	@Nullable
	private static TextureTarget worldMask;
	/** Boxed once per (re)creation, so binding the depth sampler allocates nothing. */
	@Nullable
	private static Integer maskDepthTexture;
	/** Whether the mask render types draw into {@link #worldMask} instead of {@link #mask} ({@link #bindMaskForWrite}). */
	private static boolean drawToWorldMask;
	private static boolean usedThisFrame;
	private static int idleFrames;
	private static boolean worldMaskUsedThisFrame;
	private static int worldMaskIdleFrames;

	private OutlineTargets() {
	}

	/**
	 * Creates or resizes both targets to the given size.
	 */
	static void ensureSize(int width, int height) {
		usedThisFrame = true;
		idleFrames = 0;
		if (mask == null) {
			mask = new TextureTarget(width, height, true, Minecraft.ON_OSX);
			mask.setClearColor(0.0f, 0.0f, 0.0f, 0.0f);
			maskDepthTexture = mask.getDepthTextureId();
		} else if (mask.width != width || mask.height != height) {
			mask.resize(width, height, Minecraft.ON_OSX);
			maskDepthTexture = mask.getDepthTextureId();
		}
		if (edt == null) {
			edt = new TextureTarget(width, height, false, Minecraft.ON_OSX);
			edt.setClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		} else if (edt.width != width || edt.height != height) {
			edt.resize(width, height, Minecraft.ON_OSX);
		}
	}

	/**
	 * Creates or resizes the world mask of shader packs (and, through {@link #ensureSize}, the other two targets).
	 */
	static void ensureWorldMaskSize(int width, int height) {
		ensureSize(width, height);
		worldMaskUsedThisFrame = true;
		worldMaskIdleFrames = 0;
		if (worldMask == null) {
			worldMask = new TextureTarget(width, height, true, Minecraft.ON_OSX);
			worldMask.setClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		} else if (worldMask.width != width || worldMask.height != height) {
			worldMask.resize(width, height, Minecraft.ON_OSX);
		}
	}

	static RenderTarget mask() {
		return mask;
	}

	static RenderTarget edt() {
		return edt;
	}

	@Nullable
	static RenderTarget worldMask() {
		return worldMask;
	}

	static Integer maskDepthTexture() {
		return maskDepthTexture;
	}

	/**
	 * Makes the mask render types draw into the world mask of shader packs ({@code true}) or the normal mask.
	 */
	static void drawToWorldMask(boolean world) {
		drawToWorldMask = world;
	}

	/**
	 * Binds the mask target for drawing; the output shard of every mask render type calls this.
	 */
	static void bindMaskForWrite() {
		TextureTarget target = drawToWorldMask ? worldMask : mask;
		if (target != null) {
			target.bindWrite(false);
		}
	}

	/**
	 * Call once per frame; frees the targets after a long idle time.
	 */
	static void tickFrame() {
		if (worldMaskUsedThisFrame) {
			worldMaskUsedThisFrame = false;
			worldMaskIdleFrames = 0;
		} else if (worldMask != null && ++worldMaskIdleFrames >= IDLE_FRAMES) {
			worldMask.destroyBuffers();
			worldMask = null;
			worldMaskIdleFrames = 0;
		}
		if (usedThisFrame) {
			usedThisFrame = false;
			idleFrames = 0;
			return;
		}
		if (mask != null || edt != null) {
			if (++idleFrames >= IDLE_FRAMES) {
				release();
			}
		}
	}

	/**
	 * Frees every target.
	 */
	static void release() {
		if (mask != null) {
			mask.destroyBuffers();
			mask = null;
		}
		if (edt != null) {
			edt.destroyBuffers();
			edt = null;
		}
		if (worldMask != null) {
			worldMask.destroyBuffers();
			worldMask = null;
		}
		maskDepthTexture = null;
		drawToWorldMask = false;
		idleFrames = 0;
		worldMaskIdleFrames = 0;
	}
}
