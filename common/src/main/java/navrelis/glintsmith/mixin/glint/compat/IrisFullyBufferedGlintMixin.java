package navrelis.glintsmith.mixin.glint.compat;

import navrelis.glintsmith.render.glint.GlintSystem;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * G7: the glint swap of G5a for Iris's {@code FullyBufferedMultiBufferSource}. While Iris is installed (shaders on or
 * off) that source replaces {@code RenderBuffers.bufferSource()} for the whole of {@code LevelRenderer.renderLevel},
 * and its {@code getBuffer} override never calls the vanilla {@code BufferSource.getBuffer}, so G5a does not see world
 * glints. {@link GlintSystem#substituteBuffered} decides; it is a no-op under an active shader pack.
 * <p>
 * Iris is optional: {@code @Pseudo} lets the target be absent, and the injector is {@code require = 0}. The mixin is
 * not remapped. The class name is Iris's own, and {@code getBuffer} is the method name in every namespace (vanilla's
 * {@code MultiBufferSource.getBuffer} is not obfuscated, so it has no intermediary name), while the descriptor differs
 * between the named dev environment and intermediary at run time; hence the bare method name (there is exactly one
 * {@code getBuffer} in the class) and an argument matched by type.
 */
@Pseudo
@Mixin(targets = "net.irisshaders.batchedentityrendering.impl.FullyBufferedMultiBufferSource", remap = false)
public abstract class IrisFullyBufferedGlintMixin {
	@ModifyVariable(method = "getBuffer", at = @At("HEAD"), argsOnly = true, require = 0, remap = false)
	private RenderType glintsmith$swapGlint(RenderType requested) {
		return GlintSystem.substituteBuffered(requested);
	}
}
