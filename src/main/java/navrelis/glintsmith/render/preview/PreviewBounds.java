package navrelis.glintsmith.render.preview;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Measures the bounds of an item as the preview draws it, by rendering it once into a {@link Recorder} that only
 * keeps the vertex positions.
 */
final class PreviewBounds {
	/** The fallback when an item cannot be measured: the unit block around the origin. */
	private static final float[] UNIT_CUBE = {-0.5f, -0.5f, -0.5f, 0.5f, 0.5f, 0.5f};

	private PreviewBounds() {
	}

	/**
	 * Renders {@code stack} with {@code model} in {@code ctx}, cancelling {@code cancel} if given (as the preview
	 * does), and returns {@code {minX, minY, minZ, maxX, maxY, maxZ}} in blocks. An item that throws or draws nothing
	 * gives the unit cube. Call only while no outline phase is open, or the measuring render would be captured.
	 */
	static float[] measure(ItemStack stack, BakedModel model, ItemDisplayContext ctx, @Nullable ItemTransform cancel) {
		Recorder recorder = new Recorder();
		try {
			PoseStack pose = new PoseStack();
			if (cancel != null) {
				PreviewPose.applyInverse(cancel, pose);
			}
			Minecraft.getInstance().getItemRenderer().render(stack, ctx, false, pose, recorder, LightTexture.FULL_BRIGHT,
				OverlayTexture.NO_OVERLAY, model);
		} catch (Throwable t) {
			// for example a block entity renderer that casts its buffer source to a BufferSource
			return unitCube();
		}
		float[] bounds = recorder.bounds();
		return bounds != null ? bounds : unitCube();
	}

	static float[] unitCube() {
		return UNIT_CUBE.clone();
	}

	/**
	 * A buffer source that records the extent of the vertex positions (already transformed by the pose) it receives.
	 * Everything else is ignored.
	 * <p>
	 * Every {@link #getBuffer} call hands out a view of its own: an enchanted item joins its glint buffer and its main
	 * buffer with {@code VertexMultiConsumer.create}, which refuses two identical delegates.
	 */
	static final class Recorder implements MultiBufferSource {
		private float minX = Float.POSITIVE_INFINITY;
		private float minY = Float.POSITIVE_INFINITY;
		private float minZ = Float.POSITIVE_INFINITY;
		private float maxX = Float.NEGATIVE_INFINITY;
		private float maxY = Float.NEGATIVE_INFINITY;
		private float maxZ = Float.NEGATIVE_INFINITY;
		private int vertices;

		@Override
		public VertexConsumer getBuffer(RenderType renderType) {
			return new View();
		}

		private void record(float x, float y, float z) {
			if (Float.isFinite(x) && Float.isFinite(y) && Float.isFinite(z)) {
				minX = Math.min(minX, x);
				minY = Math.min(minY, y);
				minZ = Math.min(minZ, z);
				maxX = Math.max(maxX, x);
				maxY = Math.max(maxY, y);
				maxZ = Math.max(maxZ, z);
				vertices++;
			}
		}

		/**
		 * @return {@code {minX, minY, minZ, maxX, maxY, maxZ}}, or null if no vertex was recorded
		 */
		@Nullable
		float[] bounds() {
			if (vertices == 0) {
				return null;
			}
			return new float[] {minX, minY, minZ, maxX, maxY, maxZ};
		}

		/** One buffer of the recorder: passes the positions on, ignores everything else. */
		private final class View implements VertexConsumer {
			@Override
			public VertexConsumer addVertex(float x, float y, float z) {
				record(x, y, z);
				return this;
			}

			@Override
			public VertexConsumer setColor(int red, int green, int blue, int alpha) {
				return this;
			}

			@Override
			public VertexConsumer setUv(float u, float v) {
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
	}
}
