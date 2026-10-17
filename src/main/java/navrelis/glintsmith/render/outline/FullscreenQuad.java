package navrelis.glintsmith.render.outline;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

/**
 * A static vertex buffer with one screen-covering quad in normalised device coordinates, drawn by both outline
 * passes. The fragment work is limited by the scissor box set by the compositor. Created lazily on first use.
 */
final class FullscreenQuad {
	private static final Matrix4f IDENTITY = new Matrix4f();
	@Nullable
	private static VertexBuffer buffer;

	private FullscreenQuad() {
	}

	/**
	 * Draws the quad with a shader whose samplers and uniforms are already set.
	 */
	static void draw(ShaderInstance shader) {
		VertexBuffer vb = buffer;
		if (vb == null) {
			vb = create();
			buffer = vb;
		}
		vb.bind();
		try {
			vb.drawWithShader(IDENTITY, IDENTITY, shader);
		} finally {
			VertexBuffer.unbind();
		}
	}

	private static VertexBuffer create() {
		// Own scratch memory instead of the shared Tesselator, which another renderer may be building with.
		try (ByteBufferBuilder bytes = new ByteBufferBuilder(4 * DefaultVertexFormat.POSITION.getVertexSize())) {
			BufferBuilder builder = new BufferBuilder(bytes, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
			builder.addVertex(-1.0f, -1.0f, 0.0f);
			builder.addVertex(1.0f, -1.0f, 0.0f);
			builder.addVertex(1.0f, 1.0f, 0.0f);
			builder.addVertex(-1.0f, 1.0f, 0.0f);
			VertexBuffer vb = new VertexBuffer(VertexBuffer.Usage.STATIC);
			vb.bind();
			vb.upload(builder.buildOrThrow());
			VertexBuffer.unbind();
			return vb;
		}
	}

	/**
	 * Frees the buffer (client shutdown).
	 */
	static void close() {
		VertexBuffer vb = buffer;
		buffer = null;
		if (vb != null) {
			vb.close();
		}
	}
}
