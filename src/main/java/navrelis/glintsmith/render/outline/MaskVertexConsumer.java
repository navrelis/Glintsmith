package navrelis.glintsmith.render.outline;

import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * Writes the mask copy of a vertex: position, the style index as red channel, and the UV for the texture cutout.
 * Every other attribute is ignored. Modelled on vanilla's {@code OutlineBufferSource.EntityOutlineGenerator}.
 * <p>
 * The mask vertex is always completed: if the source never set a UV for a vertex, a default UV is written before
 * the next vertex starts (or in {@link #finish()}), so the mask buffer can never hold a half-written vertex.
 */
final class MaskVertexConsumer implements VertexConsumer {
	private VertexConsumer buffer;
	private int index;
	private boolean defaultUv;
	private ScreenBounds bounds;
	private boolean uvPending;

	void bind(VertexConsumer buffer, int index, boolean defaultUv, ScreenBounds bounds) {
		this.buffer = buffer;
		this.index = index;
		this.defaultUv = defaultUv;
		this.bounds = bounds;
		this.uvPending = false;
	}

	VertexConsumer buffer() {
		return buffer;
	}

	/**
	 * Completes the last vertex if its UV is still missing.
	 */
	void finish() {
		if (uvPending && buffer != null) {
			uvPending = false;
			buffer.setUv(0.5f, 0.5f);
		}
	}

	void unbind() {
		finish();
		buffer = null;
		bounds = null;
	}

	@Override
	public VertexConsumer addVertex(float x, float y, float z) {
		VertexConsumer b = buffer;
		if (b == null) {
			return this;
		}
		finish();
		b.addVertex(x, y, z);
		b.setColor(index, 0, 0, 255);
		if (defaultUv) {
			b.setUv(0.5f, 0.5f);
		} else {
			uvPending = true;
		}
		bounds.include(x, y, z);
		return this;
	}

	@Override
	public VertexConsumer setUv(float u, float v) {
		if (uvPending) {
			uvPending = false;
			buffer.setUv(u, v);
		}
		return this;
	}

	@Override
	public VertexConsumer setColor(int r, int g, int b, int a) {
		return this;
	}

	@Override
	public VertexConsumer setUv1(int u, int v) {
		return this;
	}

	@Override
	public VertexConsumer setUv2(int u, int v) {
		return this;
	}

	@Override
	public VertexConsumer setNormal(float x, float y, float z) {
		return this;
	}
}
