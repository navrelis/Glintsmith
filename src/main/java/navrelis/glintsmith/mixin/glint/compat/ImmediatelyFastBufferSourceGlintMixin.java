package navrelis.glintsmith.mixin.glint.compat;

import navrelis.glintsmith.render.glint.GlintSystem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * G9: the glint swap of G5a for ImmediatelyFast's {@code BatchableBufferSource}. ImmediatelyFast overwrites
 * {@code MultiBufferSource.immediateWithBuffers}, so {@code RenderBuffers.bufferSource()} (our main source) is an
 * instance of that subclass, and its {@code getBuffer} override never calls the vanilla {@code BufferSource.getBuffer}
 * (G5a). Without this hook every glint drawn into the main source keeps the vanilla look: the first-person hand, and
 * the world when Iris is not installed. {@link GlintSystem#substitute} decides with the same rules as G5a (main source
 * only); the subclass honours the fixed buffers the slot types are registered in, and draws them through the vanilla
 * private {@code endBatch(RenderType, BufferBuilder)}, so G5c releases them as usual.
 * <p>
 * ImmediatelyFast is optional: {@code @Pseudo} lets the target be absent, and the injector is {@code require = 0}. Not
 * remapped, for the same reason as G7: the class name is ImmediatelyFast's own, and {@code getBuffer} keeps its name in
 * every namespace (vanilla does not obfuscate it) while the descriptor does not; there is exactly one {@code getBuffer}
 * in the class.
 */
@Pseudo
@Mixin(targets = "net.raphimc.immediatelyfast.feature.core.BatchableBufferSource", remap = false)
public abstract class ImmediatelyFastBufferSourceGlintMixin {
	@ModifyVariable(method = "getBuffer", at = @At("HEAD"), argsOnly = true, require = 0, remap = false)
	private RenderType glintsmith$swapGlint(RenderType requested) {
		return GlintSystem.substitute((MultiBufferSource.BufferSource) (Object) this, requested);
	}
}
