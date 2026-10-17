package navrelis.glintsmith.render.outline;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.Nullable;

/**
 * Everything one phase collects until its composite: the mask geometry, the styles in use and the screen bounds.
 * Phases never share a buffer source, so a nested phase cannot flush another phase's geometry.
 */
final class PhaseState {
	final OutlinePhase phase;
	/** Mask types test against the scene depth (world and hand) or not (GUI). */
	final boolean depthTested;
	final MaskBufferSource maskSource = new MaskBufferSource();
	final StyleTable styles = new StyleTable();
	final ScreenBounds bounds = new ScreenBounds();
	boolean hasGeometry;
	boolean open;
	/** Thickness factor for this phase, set when the phase opens. */
	float thicknessScale = 1.0f;

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
	 * Forgets styles, bounds and geometry flags (after a composite or a discard). If the mask source has reached its
	 * type cap, its buffers are freed so that new textures can be outlined again.
	 */
	void reset() {
		styles.reset();
		bounds.reset();
		hasGeometry = false;
		maskSource.trimIfFull();
	}

	/**
	 * Throws the collected geometry away without drawing it and resets.
	 */
	void discard() {
		if (hasGeometry || maskSource.hasStarted()) {
			maskSource.discard();
		}
		reset();
	}
}
