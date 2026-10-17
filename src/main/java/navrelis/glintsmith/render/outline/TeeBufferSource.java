package navrelis.glintsmith.render.outline;

import java.util.Arrays;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.Object2ObjectSortedMaps;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

/**
 * The buffer source handed to a qualifying item render instead of the real one. Every buffer it returns writes to
 * the real buffer and, for maskable render types, also into the phase's mask.
 * <p>
 * It extends {@link MultiBufferSource.BufferSource} so renderers that check for or cast to that class keep working;
 * its own (dummy) buffers are never used. The flush methods forward to the delegate when that is a buffer source.
 * Pooled by {@link TeePool}; nothing is allocated per item in steady state.
 * <p>
 * An item render that receives a tee as its buffer source (a renderer drawing another item into the same source)
 * joins the outer item's outline: it is counted with {@link #enterNested()} and must not release the tee.
 */
final class TeeBufferSource extends MultiBufferSource.BufferSource {
	/** Distinct (render type, buffer) pairs per item render; further buffers are passed through without a mask. */
	static final int MAX_CONSUMERS = 64;
	private static final ByteBufferBuilder DUMMY = new ByteBufferBuilder(64);

	private MultiBufferSource delegate;
	private PhaseState phase;
	private int index;
	private boolean active;
	private int nested;
	private TeeVertexConsumer[] consumers = new TeeVertexConsumer[4];
	private int count;

	TeeBufferSource() {
		super(DUMMY, Object2ObjectSortedMaps.emptyMap());
	}

	void bind(MultiBufferSource delegate, PhaseState phase, int index) {
		this.delegate = delegate;
		this.phase = phase;
		this.index = index;
		this.active = true;
		this.nested = 0;
		this.count = 0;
	}

	/**
	 * Detaches every consumer from the mask; the delegate stays, so late calls still reach the real buffers.
	 */
	void release() {
		active = false;
		nested = 0;
		for (int i = 0; i < count; i++) {
			consumers[i].release();
		}
		count = 0;
		phase = null;
	}

	/** A nested item render starts with this tee as its source. */
	void enterNested() {
		nested++;
	}

	/**
	 * A nested item render ends.
	 *
	 * @return true if the end belonged to a nested render (the tee stays in use), false for the owner's end
	 */
	boolean exitNested() {
		if (nested > 0) {
			nested--;
			return true;
		}
		return false;
	}

	/** Whether the tee still copies into the mask. */
	boolean active() {
		return active;
	}

	@Override
	public VertexConsumer getBuffer(RenderType type) {
		VertexConsumer real = delegate.getBuffer(type);
		if (!active) {
			return real;
		}
		try {
			RenderType maskType = MaskTypes.forSource(type, phase.depthTested);
			if (maskType == null) {
				return real;
			}
			VertexConsumer maskBuffer = phase.maskBuffer(maskType);
			if (maskBuffer == null) {
				return real;
			}
			for (int i = 0; i < count; i++) {
				if (consumers[i].matches(real, maskBuffer)) {
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
			tee.bind(real, maskBuffer, index, MaskTypes.needsDefaultUv(type), phase.bounds);
			return tee;
		} catch (Throwable t) {
			active = false;
			OutlineFailsafe.trip("outline buffer source", t);
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
