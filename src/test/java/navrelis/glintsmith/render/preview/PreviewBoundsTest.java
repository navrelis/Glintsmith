package navrelis.glintsmith.render.preview;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import net.minecraft.client.renderer.RenderType;
import org.junit.jupiter.api.Test;

class PreviewBoundsTest {
	private static final float EPS = 1.0e-6f;

	@Test
	void recordsNothingWithoutVertices() {
		assertNull(new PreviewBounds.Recorder().bounds());
	}

	@Test
	void recordsTheExtentOfThePositions() {
		PreviewBounds.Recorder recorder = new PreviewBounds.Recorder();
		VertexConsumer buffer = recorder.getBuffer((RenderType) null);
		buffer.addVertex(-0.5f, 2.0f, 0.25f);
		buffer.addVertex(0.75f, -1.0f, -0.25f);
		buffer.addVertex(Float.NaN, 9.0f, 9.0f);
		assertArrayEquals(new float[] {-0.5f, -1.0f, -0.25f, 0.75f, 2.0f, 0.25f}, recorder.bounds(), EPS);
	}

	/**
	 * An enchanted item joins its glint buffer and its main buffer with {@code VertexMultiConsumer.create(glint, main)}
	 * (the held trident in {@code BlockEntityWithoutLevelRenderer}, the bow in {@code ItemRenderer}), which refuses two
	 * identical delegates. The measured bounds must still be the model's, not the unit-cube fallback.
	 */
	@Test
	void anEnchantedRenderIsMeasured() {
		PreviewBounds.Recorder recorder = new PreviewBounds.Recorder();
		VertexConsumer joined = VertexMultiConsumer.create(recorder.getBuffer((RenderType) null),
			recorder.getBuffer((RenderType) null));
		// the in-hand trident after the cancelled first-person transform: long and below the origin
		joined.addVertex(-0.656f, -2.1875f, -0.53f, -1, 0.0f, 0.0f, 0, 0, 0.0f, 1.0f, 0.0f);
		joined.addVertex(-0.344f, -0.25f, -0.47f, -1, 0.0f, 0.0f, 0, 0, 0.0f, 1.0f, 0.0f);
		float[] bounds = recorder.bounds();
		assertNotNull(bounds);
		assertArrayEquals(new float[] {-0.656f, -2.1875f, -0.53f, -0.344f, -0.25f, -0.47f}, bounds, EPS);
	}

	@Test
	void manyJoinedBuffersAreMeasured() {
		PreviewBounds.Recorder recorder = new PreviewBounds.Recorder();
		VertexConsumer joined = VertexMultiConsumer.create(recorder.getBuffer((RenderType) null),
			recorder.getBuffer((RenderType) null), recorder.getBuffer((RenderType) null));
		joined.addVertex(1.0f, 2.0f, 3.0f);
		assertArrayEquals(new float[] {1.0f, 2.0f, 3.0f, 1.0f, 2.0f, 3.0f}, recorder.bounds(), EPS);
	}
}
