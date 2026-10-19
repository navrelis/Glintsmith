package navrelis.glintsmith.render.outline;

import java.util.Map;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.Nullable;

/**
 * The buffer source that collects the mask geometry of one phase. Every mask type is registered as a fixed buffer
 * before its first use, so nothing ever draws early through the shared buffer. The buffers live for the session.
 */
final class MaskBufferSource extends MultiBufferSource.BufferSource {
	/** Distinct mask types per phase; a new type beyond this is not masked until the next composite. */
	static final int MAX_TYPES = 128;

	private boolean closed;

	MaskBufferSource() {
		super(new ByteBufferBuilder(256), new Object2ObjectLinkedOpenHashMap<>());
	}

	/**
	 * @return the buffer for a mask type, or null if the type cap is reached or the source is closed
	 */
	@Nullable
	VertexConsumer bufferFor(RenderType maskType) {
		if (closed) {
			return null;
		}
		if (!fixedBuffers.containsKey(maskType)) {
			if (fixedBuffers.size() >= MAX_TYPES) {
				return null;
			}
			fixedBuffers.put(maskType, new ByteBufferBuilder(maskType.bufferSize()));
		}
		return getBuffer(maskType);
	}

	/**
	 * @return whether any mask buffer has been started since the last flush or discard
	 */
	boolean hasStarted() {
		return !startedBuilders.isEmpty();
	}

	/**
	 * Throws the collected geometry away without drawing it.
	 */
	void discard() {
		if (closed) {
			return;
		}
		for (BufferBuilder builder : startedBuilders.values()) {
			MeshData mesh = builder.build();
			if (mesh != null) {
				mesh.close();
			}
		}
		startedBuilders.clear();
		lastSharedType = null;
	}

	/**
	 * Frees the fixed buffers when the type cap is reached, so a long session with many textures starts over.
	 * Only call when nothing is started.
	 */
	void trimIfFull() {
		if (!closed && fixedBuffers.size() >= MAX_TYPES && startedBuilders.isEmpty()) {
			for (ByteBufferBuilder buffer : fixedBuffers.values()) {
				buffer.close();
			}
			fixedBuffers.clear();
		}
	}

	/**
	 * @return number of registered mask types
	 */
	int typeCount() {
		return fixedBuffers.size();
	}

	/**
	 * Frees all native memory. The source is unusable afterwards.
	 */
	void close() {
		if (closed) {
			return;
		}
		closed = true;
		startedBuilders.clear();
		for (Map.Entry<RenderType, ByteBufferBuilder> entry : fixedBuffers.entrySet()) {
			entry.getValue().close();
		}
		fixedBuffers.clear();
		sharedBuffer.close();
	}
}
