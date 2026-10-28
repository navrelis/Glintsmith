package navrelis.glintsmith.render.outline;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import org.lwjgl.system.MemoryStack;

/**
 * The outline tee with Sodium's {@code VertexBufferWriter}, installed by {@link #install()} only when Sodium is loaded
 * (the interface does not exist otherwise). Sodium's fast paths (item quads, entity model cubes) push whole vertices
 * as raw memory to a consumer that implements it; to any other consumer they fall back to the per-vertex calls and
 * log one "does not support optimized vertex writing code paths" warning per class. With this class the pushed
 * vertices go to the mask ({@link TeeVertexConsumer#copyToMask}) and then to the real consumer: pushed unchanged if it
 * takes them (plain {@code BufferBuilder}), otherwise replayed through its per-vertex {@code addVertex}, which is what
 * Sodium's own fallback does (for example while Iris extends the vertex format of a shader pack). Modelled on
 * Sodium's writer for vanilla's {@code EntityOutlineGenerator}.
 * <p>
 * Fabric only: this lives in the loader folder because only the Fabric build has Sodium's API on its compile class
 * path (in the package of the outline core, whose tee is package-private).
 */
public final class SodiumTeeVertexConsumer extends TeeVertexConsumer implements VertexBufferWriter {
	private SodiumTeeVertexConsumer() {
	}

	/**
	 * Makes every outline tee a Sodium vertex writer. Call once from the client initialiser, and only if Sodium is
	 * loaded.
	 */
	public static void install() {
		TeeVertexConsumer.useFactory(SodiumTeeVertexConsumer::new);
	}

	@Override
	public boolean canUseIntrinsics() {
		return true;
	}

	@Override
	public boolean canUseIntrinsics(VertexFormat format) {
		return format.contains(VertexFormatElement.POSITION);
	}

	@Override
	public void push(MemoryStack stack, long ptr, int count, VertexFormat format) {
		// The mask only reads the vertices; the real consumer may change them in place, so it comes second.
		copyToMask(ptr, count, format);
		VertexConsumer real = real();
		VertexBufferWriter writer = VertexBufferWriter.tryOf(real, format);
		if (writer != null) {
			writer.push(stack, ptr, count, format);
		} else {
			TeeVertexConsumer.replay(real, ptr, count, format);
		}
	}
}
