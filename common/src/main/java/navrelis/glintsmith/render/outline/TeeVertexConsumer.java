package navrelis.glintsmith.render.outline;

import java.util.function.Supplier;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.lwjgl.system.MemoryUtil;

/**
 * Forwards every call to the real consumer first and then copies position and UV into the mask. The mask side is
 * guarded: the first error disables the mask for this consumer and trips the {@link OutlineFailsafe}, while the
 * real consumer keeps receiving everything, so vanilla rendering is never affected.
 * <p>
 * Pooled: bound per item render by {@link TeeBufferSource}. A released consumer keeps forwarding to its last real
 * consumer, so a renderer that holds on to it cannot crash.
 * <p>
 * Instances come from {@link #create()}. A loader layer may install a subclass there that also implements Sodium's
 * {@code VertexBufferWriter} (Fabric: {@code SodiumTeeVertexConsumer}), so Sodium's fast paths keep working for
 * outlined items and Sodium does not warn about this class; {@link #copyToMask} serves that path.
 */
class TeeVertexConsumer implements VertexConsumer {
	private static Supplier<TeeVertexConsumer> factory = TeeVertexConsumer::new;

	private VertexConsumer real;
	private final MaskVertexConsumer mask = new MaskVertexConsumer();
	private boolean maskActive;

	TeeVertexConsumer() {
	}

	/**
	 * @return a new consumer of the installed kind
	 */
	static TeeVertexConsumer create() {
		return factory.get();
	}

	/**
	 * Installs the kind of consumer {@link #create()} makes. Call during client initialisation, before anything is
	 * outlined.
	 */
	static void useFactory(Supplier<TeeVertexConsumer> newFactory) {
		factory = newFactory;
	}

	/**
	 * @return the consumer everything is forwarded to
	 */
	final VertexConsumer real() {
		return real;
	}

	/**
	 * Copies position and UV of {@code count} vertices that a caller hands over as raw memory in {@code format}
	 * (Sodium's {@code VertexBufferWriter.push}) into the mask, exactly as the per-vertex path would. The caller then
	 * pushes the same memory to the real consumer. Never throws; an error disables the mask like any other.
	 */
	final void copyToMask(long ptr, int count, VertexFormat format) {
		if (!maskActive) {
			return;
		}
		try {
			if (!format.contains(VertexFormatElement.POSITION)) {
				return;
			}
			long stride = format.getVertexSize();
			long position = format.getOffset(VertexFormatElement.POSITION);
			long uv = format.contains(VertexFormatElement.UV0) ? format.getOffset(VertexFormatElement.UV0) : -1L;
			long vertex = ptr;
			for (int i = 0; i < count; i++) {
				mask.addVertex(MemoryUtil.memGetFloat(vertex + position), MemoryUtil.memGetFloat(vertex + position + 4L),
					MemoryUtil.memGetFloat(vertex + position + 8L));
				if (uv >= 0L) {
					mask.setUv(MemoryUtil.memGetFloat(vertex + uv), MemoryUtil.memGetFloat(vertex + uv + 4L));
				}
				vertex += stride;
			}
		} catch (Throwable t) {
			maskFailed(t);
		}
	}

	/**
	 * Feeds raw vertices in {@code format} to a consumer that cannot take them as memory, one bulk {@code addVertex}
	 * each, as vanilla's {@code putBulkData} (and Sodium's own fallback) does. Elements the format lacks get the
	 * vanilla defaults (white, UV 0, no overlay, full bright, normal up). For the Sodium writer path when the real
	 * consumer has no fast path (for example while Iris extends the vertex format of a shader pack).
	 */
	static void replay(VertexConsumer real, long ptr, int count, VertexFormat format) {
		long stride = format.getVertexSize();
		long position = format.getOffset(VertexFormatElement.POSITION);
		long color = offset(format, VertexFormatElement.COLOR);
		long uv = offset(format, VertexFormatElement.UV0);
		long overlay = offset(format, VertexFormatElement.UV1);
		long light = offset(format, VertexFormatElement.UV2);
		long normal = offset(format, VertexFormatElement.NORMAL);
		long vertex = ptr;
		for (int i = 0; i < count; i++) {
			real.addVertex(MemoryUtil.memGetFloat(vertex + position), MemoryUtil.memGetFloat(vertex + position + 4L),
				MemoryUtil.memGetFloat(vertex + position + 8L),
				color < 0L ? -1 : argbOfAbgr(MemoryUtil.memGetInt(vertex + color)),
				uv < 0L ? 0.0f : MemoryUtil.memGetFloat(vertex + uv), uv < 0L ? 0.0f : MemoryUtil.memGetFloat(vertex + uv + 4L),
				overlay < 0L ? OverlayTexture.NO_OVERLAY : MemoryUtil.memGetInt(vertex + overlay),
				light < 0L ? LightTexture.FULL_BRIGHT : MemoryUtil.memGetInt(vertex + light),
				normal < 0L ? 0.0f : MemoryUtil.memGetByte(vertex + normal) / 127.0f,
				normal < 0L ? 1.0f : MemoryUtil.memGetByte(vertex + normal + 1L) / 127.0f,
				normal < 0L ? 0.0f : MemoryUtil.memGetByte(vertex + normal + 2L) / 127.0f);
			vertex += stride;
		}
	}

	private static long offset(VertexFormat format, VertexFormatElement element) {
		return format.contains(element) ? format.getOffset(element) : -1L;
	}

	/**
	 * The colour bytes R, G, B, A of a vertex read as one little-endian int (ABGR), as the ARGB int of
	 * {@code addVertex}.
	 */
	static int argbOfAbgr(int abgr) {
		return (abgr & 0xFF00FF00) | ((abgr & 0xFF) << 16) | ((abgr >>> 16) & 0xFF);
	}

	void bind(VertexConsumer real, VertexConsumer maskBuffer, int index, boolean defaultUv, ScreenBounds bounds) {
		this.real = real;
		this.mask.bind(maskBuffer, index, defaultUv, bounds);
		this.maskActive = true;
	}

	/**
	 * Stops copying into the mask; the real consumer stays attached.
	 */
	void release() {
		if (maskActive) {
			maskActive = false;
			try {
				mask.unbind();
			} catch (Throwable t) {
				OutlineFailsafe.trip("outline mask vertex", t);
			}
		}
	}

	boolean matches(VertexConsumer real, VertexConsumer maskBuffer) {
		return maskActive && this.real == real && mask.buffer() == maskBuffer;
	}

	private void maskFailed(Throwable t) {
		maskActive = false;
		OutlineFailsafe.trip("outline mask vertex", t);
	}

	@Override
	public VertexConsumer addVertex(float x, float y, float z) {
		real.addVertex(x, y, z);
		if (maskActive) {
			try {
				mask.addVertex(x, y, z);
			} catch (Throwable t) {
				maskFailed(t);
			}
		}
		return this;
	}

	@Override
	public void addVertex(float x, float y, float z, int color, float u, float v, int overlay, int light,
		float nx, float ny, float nz) {
		real.addVertex(x, y, z, color, u, v, overlay, light, nx, ny, nz);
		if (maskActive) {
			try {
				mask.addVertex(x, y, z);
				mask.setUv(u, v);
			} catch (Throwable t) {
				maskFailed(t);
			}
		}
	}

	@Override
	public VertexConsumer setUv(float u, float v) {
		real.setUv(u, v);
		if (maskActive) {
			try {
				mask.setUv(u, v);
			} catch (Throwable t) {
				maskFailed(t);
			}
		}
		return this;
	}

	@Override
	public VertexConsumer setColor(int r, int g, int b, int a) {
		real.setColor(r, g, b, a);
		return this;
	}

	@Override
	public VertexConsumer setColor(int argb) {
		real.setColor(argb);
		return this;
	}

	@Override
	public VertexConsumer setUv1(int u, int v) {
		real.setUv1(u, v);
		return this;
	}

	@Override
	public VertexConsumer setOverlay(int overlay) {
		real.setOverlay(overlay);
		return this;
	}

	@Override
	public VertexConsumer setUv2(int u, int v) {
		real.setUv2(u, v);
		return this;
	}

	@Override
	public VertexConsumer setLight(int light) {
		real.setLight(light);
		return this;
	}

	@Override
	public VertexConsumer setNormal(float x, float y, float z) {
		real.setNormal(x, y, z);
		return this;
	}
}
