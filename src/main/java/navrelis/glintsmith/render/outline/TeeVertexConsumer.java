package navrelis.glintsmith.render.outline;

import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * Forwards every call to the real consumer first and then copies position and UV into the mask. The mask side is
 * guarded: the first error disables the mask for this consumer and trips the {@link OutlineFailsafe}, while the
 * real consumer keeps receiving everything, so vanilla rendering is never affected.
 * <p>
 * Pooled: bound per item render by {@link TeeBufferSource}. A released consumer keeps forwarding to its last real
 * consumer, so a renderer that holds on to it cannot crash.
 */
final class TeeVertexConsumer implements VertexConsumer {
	private VertexConsumer real;
	private final MaskVertexConsumer mask = new MaskVertexConsumer();
	private boolean maskActive;

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
