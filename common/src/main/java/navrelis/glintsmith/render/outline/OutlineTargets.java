package navrelis.glintsmith.render.outline;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

/**
 * The two off-screen targets of the outline pipeline, created lazily at the size of the main target:
 * <ul>
 * <li>{@code mask}: RGBA8 colour (style index in red, the item's own window depth packed into green, blue and
 * alpha) plus a vanilla float depth texture holding a copy of the scene depth (after the mask draw),</li>
 * <li>{@code edt}: RGBA8 result of the horizontal distance pass.</li>
 * </ul>
 * Both are freed after {@link #IDLE_FRAMES} frames without use. Render thread only.
 */
final class OutlineTargets {
	static final int IDLE_FRAMES = 600;

	@Nullable
	private static TextureTarget mask;
	@Nullable
	private static TextureTarget edt;
	/** Boxed once per (re)creation, so binding the depth sampler allocates nothing. */
	@Nullable
	private static Integer maskDepthTexture;
	private static boolean usedThisFrame;
	private static int idleFrames;

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

	static RenderTarget mask() {
		return mask;
	}

	static RenderTarget edt() {
		return edt;
	}

	static Integer maskDepthTexture() {
		return maskDepthTexture;
	}

	/**
	 * Binds the mask target for drawing; the output shard of every mask render type calls this.
	 */
	static void bindMaskForWrite() {
		TextureTarget target = mask;
		if (target != null) {
			target.bindWrite(false);
		}
	}

	/**
	 * Call once per frame; frees the targets after a long idle time.
	 */
	static void tickFrame() {
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
	 * Frees both targets.
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
		maskDepthTexture = null;
		idleFrames = 0;
	}
}
