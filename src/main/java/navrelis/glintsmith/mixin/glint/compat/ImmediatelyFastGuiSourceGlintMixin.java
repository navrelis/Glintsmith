package navrelis.glintsmith.mixin.glint.compat;

import navrelis.glintsmith.render.glint.GlintSystem;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * G11: learns which buffer source ImmediatelyFast gives {@code GuiGraphics}. ImmediatelyFast redirects the
 * {@code renderBuffers.bufferSource()} call of {@code GameRenderer.render} that builds the frame's {@code GuiGraphics}
 * to {@code BatchingBuffers.getNonBatchingEntityVertexConsumers()}, a plain vanilla {@code BufferSource} that it creates
 * once with its own copy of the main source's fixed buffers. GUI glints drawn into it pass G5a, which hands them to
 * {@link GlintSystem#substituteGui} once this hook has reported the instance.
 * <p>
 * ImmediatelyFast is optional: {@code @Pseudo} lets the target be absent, and the injector is {@code require = 0}. Not
 * remapped: the class and method are ImmediatelyFast's own (verified in the ImmediatelyFast 1.6.15+1.21.1 jar:
 * {@code public static class_4597$class_4598 getNonBatchingEntityVertexConsumers()}, the only method of that name);
 * the bare name is used because the return type's name differs between the dev environment and intermediary.
 */
@Pseudo
@Mixin(targets = "net.raphimc.immediatelyfast.feature.batching.BatchingBuffers", remap = false)
public abstract class ImmediatelyFastGuiSourceGlintMixin {
	@Inject(method = "getNonBatchingEntityVertexConsumers", at = @At("RETURN"), require = 0, remap = false)
	private static void glintsmith$captureGuiSource(CallbackInfoReturnable<MultiBufferSource.BufferSource> cir) {
		GlintSystem.onGuiSource(cir.getReturnValue());
	}
}
