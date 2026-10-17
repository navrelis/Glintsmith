package navrelis.glintsmith.render.outline;

import java.util.Arrays;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.Object2ObjectSortedMaps;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.Nullable;

/**
 * The buffer source handed to {@code renderHandsWithItems} under a shader pack. Every buffer it returns writes to the
 * real buffer and, for render types that write depth, also into the hand phase's occluder geometry: the arms and
 * sleeves (vanilla's {@code PlayerRenderer.renderHand}, or a first-person animation mod drawing its own arm model into
 * the same source), worn armor on the arm, maps and the held items themselves.
 * <p>
 * Why: with a shader pack the hand is drawn into the pack's own targets, so the main depth holds no hand at all and
 * cannot hide the item's outline where the arm covers the item. The occluders are drawn into the mask depth after the
 * item masks, which gives the composite the hand depth the main target has without a shader pack.
 * <p>
 * Outlined items rendered through this source get their item tee on top of it ({@link OutlineSystem#beginItem}); it is
 * no {@link TeeBufferSource}, so it never counts as an outer item. Like the tee it extends
 * {@link MultiBufferSource.BufferSource} and forwards the flushes to the delegate. One instance, reused every frame.
 */
final class OccluderBufferSource extends MultiBufferSource.BufferSource {
	/** Distinct (render type, buffer) pairs per hand pass; further buffers are passed through without occluding. */
	static final int MAX_CONSUMERS = 64;
	private static final ByteBufferBuilder DUMMY = new ByteBufferBuilder(64);

	private MultiBufferSource delegate;
	@Nullable
	private PhaseState phase;
	private boolean active;
	private TeeVertexConsumer[] consumers = new TeeVertexConsumer[8];
	private int count;

	OccluderBufferSource() {
		super(DUMMY, Object2ObjectSortedMaps.emptyMap());
	}

	/**
	 * Starts capturing occluders of the given hand pass.
	 *
	 * @return this
	 */
	OccluderBufferSource bind(MultiBufferSource delegate, PhaseState phase) {
		release();
		this.delegate = delegate;
		this.phase = phase;
		this.active = true;
		return this;
	}

	/**
	 * Stops capturing and completes every pending occluder vertex, so the occluder geometry can be drawn. The delegate
	 * stays, so late calls still reach the real buffers.
	 */
	void release() {
		active = false;
		for (int i = 0; i < count; i++) {
			consumers[i].release();
		}
		count = 0;
		phase = null;
	}

	/** Whether the source still captures occluders. */
	boolean active() {
		return active;
	}

	/** The real buffer source of the hand pass. */
	MultiBufferSource delegate() {
		return delegate;
	}

	@Override
	public VertexConsumer getBuffer(RenderType type) {
		VertexConsumer real = delegate.getBuffer(type);
		if (!active) {
			return real;
		}
		try {
			RenderType occluderType = MaskTypes.occluderFor(type);
			if (occluderType == null) {
				return real;
			}
			VertexConsumer occluderBuffer = phase.occluderBuffer(occluderType);
			if (occluderBuffer == null) {
				return real;
			}
			for (int i = 0; i < count; i++) {
				if (consumers[i].matches(real, occluderBuffer)) {
					return consumers[i];
				}
			}
			if (count >= MAX_CONSUMERS) {
				return real;
			}
			if (count == consumers.length) {
				consumers = Arrays.copyOf(consumers, Math.min(MAX_CONSUMERS, consumers.length * 2));
			}
			TeeVertexConsumer tee = consumers[count];
			if (tee == null) {
				tee = new TeeVertexConsumer();
				consumers[count] = tee;
			}
			count++;
			// Style index 0 and no screen bounds: occluders never become seeds and never widen the region.
			tee.bind(real, occluderBuffer, 0, MaskTypes.needsDefaultUv(type), null);
			return tee;
		} catch (Throwable t) {
			active = false;
			OutlineFailsafe.trip("outline occluder source", t);
			return real;
		}
	}

	@Override
	public void endLastBatch() {
		if (delegate instanceof MultiBufferSource.BufferSource source) {
			source.endLastBatch();
		}
	}

	@Override
	public void endBatch() {
		if (delegate instanceof MultiBufferSource.BufferSource source) {
			source.endBatch();
		}
	}

	@Override
	public void endBatch(RenderType type) {
		if (delegate instanceof MultiBufferSource.BufferSource source) {
			source.endBatch(type);
		}
	}
}
