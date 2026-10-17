package navrelis.glintsmith.render.outline;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.lwjgl.opengl.GL11;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Turns the mask of a phase into outlines on the main target: draw the mask into the mask target (every item pixel
 * with its own depth, then the scene depth where the phase needs one: a copy of the main depth, or under a shader pack
 * the first-person occluders drawn into the mask depth), run the horizontal distance pass
 * into the EDT target, then the vertical pass that blends the outline onto the main target. All passes are clipped to the bounds of the mask grown by the
 * search radius. The GL state is saved before and restored after, also on errors, which are rethrown for the
 * caller's failsafe.
 * <p>
 * Render thread only.
 */
final class OutlineCompositor {
	private static final Logger LOGGER = LoggerFactory.getLogger("Glintsmith/Outline");
	/** Near plane of the vanilla world and hand projections. */
	static final float NEAR_PLANE = 0.05f;
	private static final int GL_INVALID_OPERATION = 0x0502;

	/** How the mask target's depth is prepared around the mask draw. */
	enum MaskDepth {
		/**
		 * Cleared to the far plane for the mask draw (item depth, nearest surface wins), then overwritten with a copy
		 * of the main depth for the passes, which compare both to tell visible from hidden item pixels.
		 */
		COPY,
		/**
		 * Cleared to the far plane and not replaced: the main depth is not the scene (shader packs). The hand's
		 * occluder geometry, drawn after the item masks, makes the depth attachment the hand depth.
		 */
		CLEAR,
		/** Not used at all (GUI). */
		NONE
	}

	private static final GlState STATE = new GlState();
	private static final StyleTexture STYLE_TEXTURE = new StyleTexture();
	private static final int[] REGION = new int[4];
	private static boolean depthCopyChecked;
	private static boolean depthCopyBroken;
	private static boolean forceFailure = Boolean.getBoolean("glintsmith.debug.failOutline");

	private OutlineCompositor() {
	}

	/**
	 * @return whether copying the main depth failed once (for example a stencil main framebuffer); world and
	 *         vanilla hand outlines are then off
	 */
	static boolean depthCopyBroken() {
		return depthCopyBroken;
	}

	/**
	 * Draws the mask and both passes, then resets the phase. Discards the geometry if nothing can be drawn.
	 *
	 * @param fadeStart distance fade start in blocks (world only; 0 and 0 = off)
	 * @param fadeEnd distance fade end in blocks
	 */
	static void composite(PhaseState phase, MaskDepth depth, boolean useDepth, float fadeStart, float fadeEnd) {
		if (!phase.hasGeometry || phase.bounds.isEmpty() || (depth == MaskDepth.COPY && depthCopyBroken)
			|| !OutlineShaders.available()) {
			phase.discard();
			return;
		}
		RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
		int width = main.width;
		int height = main.height;
		if (width <= 0 || height <= 0) {
			phase.discard();
			return;
		}
		boolean drawn = false;
		STATE.save();
		try {
			checkForcedFailure();
			OutlineTargets.ensureSize(width, height);
			if (!region(phase, width, height)) {
				return;
			}
			drawn = drawMask(phase, depth, width, height, true, true);
			if (drawn) {
				float far = Minecraft.getInstance().gameRenderer.getDepthFar();
				passes(phase, main, width, height, useDepth, fadeStart, fadeEnd, OutlineDepth.projectionZ(NEAR_PLANE, far),
					OutlineDepth.projectionW(NEAR_PLANE, far));
			}
		} finally {
			STATE.restore();
			if (!drawn) {
				phase.discardGeometry();
			}
			phase.reset();
		}
	}

	/**
	 * Draws the mask geometry collected so far without compositing; the bounds and styles stay for
	 * {@link #resolve}. Used for the hand under a shader pack, where the composite must wait for the final image.
	 * The item masks are drawn first, then the occluders into the depth only, and the depth terms of the hand
	 * projection are kept for the composite.
	 *
	 * @param clear whether this is the first flush of the frame (clears colour and depth of the mask target)
	 * @return false if nothing could be drawn (the phase was discarded)
	 */
	static boolean drawMaskOnly(PhaseState phase, boolean clear) {
		if (!phase.hasGeometry || !OutlineShaders.available()) {
			phase.discard();
			return false;
		}
		RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
		int width = main.width;
		int height = main.height;
		if (width <= 0 || height <= 0) {
			phase.discard();
			return false;
		}
		boolean drawn = false;
		STATE.save();
		try {
			checkForcedFailure();
			OutlineTargets.ensureSize(width, height);
			phase.captureProjection(RenderSystem.getProjectionMatrix());
			drawn = drawMask(phase, clear ? MaskDepth.CLEAR : MaskDepth.NONE, width, height, false, clear);
		} finally {
			STATE.restore();
			if (!drawn) {
				phase.discardGeometry();
				phase.reset();
			}
		}
		return drawn;
	}

	/**
	 * Runs both passes over a mask drawn earlier by {@link #drawMaskOnly}, then resets the phase. The depth rules use
	 * the mask depth (items and occluders), linearised with the projection the mask was drawn with.
	 */
	static void resolve(PhaseState phase) {
		if (phase.bounds.isEmpty() || !OutlineShaders.available()) {
			phase.discard();
			return;
		}
		RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
		int width = main.width;
		int height = main.height;
		RenderTarget mask = OutlineTargets.mask();
		if (width <= 0 || height <= 0 || mask == null || mask.width != width || mask.height != height) {
			phase.discard();
			return;
		}
		STATE.save();
		try {
			OutlineTargets.ensureSize(width, height);
			if (region(phase, width, height)) {
				GlStateManager._enableScissorTest();
				GlStateManager._scissorBox(REGION[0], REGION[1], REGION[2] - REGION[0] + 1, REGION[3] - REGION[1] + 1);
				float far = Minecraft.getInstance().gameRenderer.getDepthFar();
				float projZ = phase.hasProjection ? phase.projZ : OutlineDepth.projectionZ(NEAR_PLANE, far);
				float projW = phase.hasProjection ? phase.projW : OutlineDepth.projectionW(NEAR_PLANE, far);
				passes(phase, main, width, height, true, 0.0f, 0.0f, projZ, projW);
			}
		} finally {
			STATE.restore();
			phase.discard();
		}
	}

	private static void checkForcedFailure() {
		if (forceFailure) {
			forceFailure = false;
			throw new IllegalStateException("outline failure forced by -Dglintsmith.debug.failOutline=true");
		}
	}

	/**
	 * Computes the pixel region of the phase (bounds grown by the search radius), clipped to an active scissor box.
	 */
	private static boolean region(PhaseState phase, int width, int height) {
		int radius = phase.styles.maxRadius();
		if (!phase.bounds.toPixels(width, height, radius + 2, REGION)) {
			return false;
		}
		if (STATE.scissorEnabled()) {
			int[] box = STATE.scissorBox();
			REGION[0] = Math.max(REGION[0], box[0]);
			REGION[1] = Math.max(REGION[1], box[1]);
			REGION[2] = Math.min(REGION[2], box[0] + box[2] - 1);
			REGION[3] = Math.min(REGION[3], box[1] + box[3] - 1);
		}
		return REGION[2] >= REGION[0] && REGION[3] >= REGION[1];
	}

	/**
	 * Binds the mask target, prepares colour and depth, and flushes the phase's mask buffer source into it.
	 *
	 * @param scissored clip the clear (and the depth copy) to {@link #REGION}
	 * @param clearColor clear the mask colour first (false when adding to a mask drawn earlier this frame)
	 * @return whether the mask was drawn
	 */
	private static boolean drawMask(PhaseState phase, MaskDepth depth, int width, int height, boolean scissored,
		boolean clearColor) {
		RenderTarget mask = OutlineTargets.mask();
		mask.bindWrite(false);
		GlStateManager._viewport(0, 0, width, height);
		if (scissored) {
			GlStateManager._enableScissorTest();
			GlStateManager._scissorBox(REGION[0], REGION[1], REGION[2] - REGION[0] + 1, REGION[3] - REGION[1] + 1);
		} else {
			GlStateManager._disableScissorTest();
		}
		GlStateManager._colorMask(true, true, true, true);
		GlStateManager._clearColor(0.0f, 0.0f, 0.0f, 0.0f);
		int clearBits = clearColor ? GL11.GL_COLOR_BUFFER_BIT : 0;
		if (depth == MaskDepth.COPY || depth == MaskDepth.CLEAR) {
			// The mask records the item's own depth: nearest item surface, not tested against the scene.
			GlStateManager._depthMask(true);
			GlStateManager._clearDepth(1.0);
			clearBits |= GL11.GL_DEPTH_BUFFER_BIT;
		}
		if (clearBits != 0) {
			GlStateManager._clear(clearBits, Minecraft.ON_OSX);
		}
		if (!phase.depthTested) {
			// NO_DEPTH_TEST leaves the depth test as it is, so switch it off explicitly.
			GlStateManager._disableDepthTest();
		}
		phase.maskSource.endBatch();
		if (phase.occluderSource.hasStarted()) {
			// Depth only, after the items: drawn first, an occluder would keep the item pixels behind it out of the
			// mask colour, and outlines on it could no longer be told apart from outlines next to the item.
			phase.occluderSource.endBatch();
		}
		if (depth == MaskDepth.COPY) {
			// The item depth now lives in the mask colour; the depth attachment becomes the scene depth.
			GlStateManager._depthMask(true);
			if (!copyDepth(mask)) {
				return false;
			}
		}
		mask.bindWrite(false);
		return true;
	}

	/**
	 * Blits the main depth into the mask target (inside the current scissor box). The first copy of the session
	 * checks for a GL error: a main framebuffer with a stencil attachment cannot be blitted into a plain depth
	 * texture, in which case depth-based outlines are switched off with one log line.
	 */
	private static boolean copyDepth(RenderTarget mask) {
		RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
		if (!depthCopyChecked) {
			for (int i = 0; i < 16 && GL11.glGetError() != GL11.GL_NO_ERROR; i++) {
				// drain errors raised before us, so the check below sees only the blit
			}
		}
		mask.copyDepthFrom(main);
		if (!depthCopyChecked) {
			depthCopyChecked = true;
			int error = GL11.glGetError();
			if (error == GL_INVALID_OPERATION) {
				depthCopyBroken = true;
				LOGGER.warn("[Glintsmith] The main framebuffer depth cannot be copied (GL error {}); world and first-person outlines are off for this session.",
					error);
				return false;
			}
		}
		return true;
	}

	/**
	 * Pass H into the EDT target, then pass V blended onto the main target. Expects the scissor box to be set.
	 *
	 * @param projZ z term {@code m22} of the projection the depth was drawn with ({@link OutlineDepth#linear})
	 * @param projW z term {@code m32} of that projection
	 */
	private static void passes(PhaseState phase, RenderTarget main, int width, int height, boolean useDepth,
		float fadeStart, float fadeEnd, float projZ, float projW) {
		ShaderInstance edtShader = OutlineShaders.edt();
		ShaderInstance compositeShader = OutlineShaders.composite();
		RenderTarget mask = OutlineTargets.mask();
		RenderTarget edt = OutlineTargets.edt();
		if (edtShader == null || compositeShader == null || mask == null || edt == null) {
			return;
		}
		int radius = phase.styles.maxRadius();
		float x0 = REGION[0];
		float y0 = REGION[1];
		float x1 = REGION[2];
		float y1 = REGION[3];

		GlStateManager._disableDepthTest();
		GlStateManager._depthMask(false);
		GlStateManager._disableBlend();
		GlStateManager._disableCull();

		edt.bindWrite(false);
		GlStateManager._viewport(0, 0, width, height);
		Integer depthTexture = OutlineTargets.maskDepthTexture();
		edtShader.setSampler("MaskSampler", mask);
		edtShader.setSampler("DepthSampler", depthTexture);
		edtShader.safeGetUniform("Radius").set(radius);
		edtShader.safeGetUniform("Region").set(x0, y0, x1, y1);
		edtShader.safeGetUniform("UseDepth").set(useDepth ? 1 : 0);
		edtShader.safeGetUniform("SeedEpsilon").set(OutlineDepth.SEED_EPSILON);
		FullscreenQuad.draw(edtShader);

		DynamicTexture styles = STYLE_TEXTURE.upload(phase.styles);
		main.bindWrite(false);
		GlStateManager._viewport(0, 0, width, height);
		compositeShader.setSampler("MaskSampler", mask);
		compositeShader.setSampler("EdtSampler", edt);
		compositeShader.setSampler("DepthSampler", depthTexture);
		compositeShader.setSampler("StyleSampler", styles);
		compositeShader.safeGetUniform("Radius").set(radius);
		compositeShader.safeGetUniform("Region").set(x0, y0, x1, y1);
		compositeShader.safeGetUniform("UseDepth").set(useDepth ? 1 : 0);
		compositeShader.safeGetUniform("DepthProj").set(projZ, projW);
		compositeShader.safeGetUniform("DepthTolerance").set(frontTolerance(phase.phase));
		compositeShader.safeGetUniform("CoverTolerance").set(OutlineDepth.COVER_TOLERANCE);
		compositeShader.safeGetUniform("SeedEpsilon").set(OutlineDepth.SEED_EPSILON);
		compositeShader.safeGetUniform("DistanceFade").set(fadeStart, fadeEnd);
		GlStateManager._enableBlend();
		GlStateManager._blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ZERO, GL11.GL_ONE);
		FullscreenQuad.draw(compositeShader);
	}

	/**
	 * @return how far, in blocks, scene geometry next to an item may be in front of it before it hides the outline
	 */
	static float frontTolerance(OutlinePhase phase) {
		return phase == OutlinePhase.HAND ? OutlineDepth.HAND_TOLERANCE : OutlineDepth.WORLD_TOLERANCE;
	}

	/**
	 * Frees the style texture (client shutdown).
	 */
	static void close() {
		STYLE_TEXTURE.close();
	}
}
