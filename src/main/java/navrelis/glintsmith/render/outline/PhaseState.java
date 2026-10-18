package navrelis.glintsmith.render.outline;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

/**
 * Everything one phase collects until its composite: the mask geometry, the styles in use and the screen bounds.
 * Phases never share a buffer source, so a nested phase cannot flush another phase's geometry.
 * <p>
 * The first-person phase under a shader pack also collects occluder geometry (arms, sleeves, everything else drawn in
 * the hand pass) in a buffer source of its own, which is drawn after the item masks, and remembers the depth terms of
 * the hand projection for the deferred composite.
 */
final class PhaseState {
	final OutlinePhase phase;
	/** Mask types keep the nearest item surface and its depth (world and hand) or have no depth at all (GUI). */
	final boolean depthTested;
	final MaskBufferSource maskSource = new MaskBufferSource();
	/** Depth-only geometry that hides outlines (first person under a shader pack); drawn after {@link #maskSource}. */
	final MaskBufferSource occluderSource = new MaskBufferSource();
	final StyleTable styles = new StyleTable();
	final ScreenBounds bounds = new ScreenBounds();
	boolean hasGeometry;
	boolean open;
	/**
	 * The settings the phase's items get their thickness from ({@link OutlineRuntimeSettings#itemThickness}): the live
	 * ones, or the preview's snapshot. Set when the phase opens.
	 */
	OutlineRuntimeSettings settings = OutlineRuntimeSettings.DISABLED;
	/** Height of the main render target when the phase opened, in pixels. */
	int framebufferHeight;
	/** z terms of the projection the mask was drawn with ({@link #captureProjection}); valid if {@link #hasProjection}. */
	float projZ;
	float projW;
	boolean hasProjection;

	PhaseState(OutlinePhase phase, boolean depthTested) {
		this.phase = phase;
		this.depthTested = depthTested;
	}

	/**
	 * Takes the projection snapshot for the screen bounds if this is the first capture since the last reset.
	 */
	void prepareCapture() {
		if (!bounds.hasSnapshot()) {
			bounds.snapshot(RenderSystem.getProjectionMatrix(), RenderSystem.getModelViewMatrix());
		}
	}

	/**
	 * Remembers the depth terms of the projection the mask is drawn with, so a composite that runs later (under
	 * another projection) can still turn the mask depth into distances. A projection {@link OutlineDepth#linear}
	 * cannot invert leaves the terms unset.
	 */
	void captureProjection(Matrix4f projection) {
		if (OutlineDepth.invertible(projection.m22(), projection.m23(), projection.m32(), projection.m33())) {
			projZ = projection.m22();
			projW = projection.m32();
			hasProjection = true;
		}
	}

	/**
	 * @return the mask buffer for a mask type (registered as a fixed buffer on first use), or null if the type cap
	 *         of the mask source is reached
	 */
	@Nullable
	VertexConsumer maskBuffer(RenderType maskType) {
		VertexConsumer buffer = maskSource.bufferFor(maskType);
		if (buffer != null) {
			hasGeometry = true;
		}
		return buffer;
	}

	/**
	 * @return the buffer for an occluder type, or null if the type cap is reached. Occluders alone are no reason to
	 *         composite, so they leave {@link #hasGeometry} alone.
	 */
	@Nullable
	VertexConsumer occluderBuffer(RenderType occluderType) {
		return occluderSource.bufferFor(occluderType);
	}

	/**
	 * Throws away the geometry that was not drawn (both sources).
	 */
	void discardGeometry() {
		maskSource.discard();
		occluderSource.discard();
	}

	/**
	 * Forgets styles, bounds, projection and geometry flags (after a composite or a discard). If a mask source has
	 * reached its type cap, its buffers are freed so that new textures can be outlined again.
	 */
	void reset() {
		styles.reset();
		bounds.reset();
		hasGeometry = false;
		hasProjection = false;
		maskSource.trimIfFull();
		occluderSource.trimIfFull();
	}

	/**
	 * Throws the collected geometry away without drawing it and resets.
	 */
	void discard() {
		if (hasGeometry || maskSource.hasStarted()) {
			maskSource.discard();
		}
		if (occluderSource.hasStarted()) {
			occluderSource.discard();
		}
		reset();
	}

	/**
	 * Frees the native memory of both sources (client shutdown).
	 */
	void close() {
		maskSource.close();
		occluderSource.close();
	}
}
