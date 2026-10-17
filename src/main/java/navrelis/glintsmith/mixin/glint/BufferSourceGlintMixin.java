package navrelis.glintsmith.mixin.glint;

import com.mojang.blaze3d.vertex.BufferBuilder;
import navrelis.glintsmith.render.glint.GlintSystem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * G5a to G5c: swaps the vanilla glint render types of the main buffer source for pooled ones, draws them right after
 * the vanilla glint, and frees them once drawn.
 */
@Mixin(MultiBufferSource.BufferSource.class)
public abstract class BufferSourceGlintMixin {
	@ModifyVariable(method = "getBuffer(Lnet/minecraft/client/renderer/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;",
		at = @At("HEAD"), argsOnly = true, require = 0)
	private RenderType glintsmith$substituteGlint(RenderType requested) {
		return GlintSystem.substitute((MultiBufferSource.BufferSource) (Object) this, requested);
	}

	@Inject(method = "endBatch(Lnet/minecraft/client/renderer/RenderType;)V", at = @At("TAIL"), require = 0)
	private void glintsmith$flushGlintSlots(RenderType type, CallbackInfo ci) {
		GlintSystem.afterEndBatch((MultiBufferSource.BufferSource) (Object) this, type);
	}

	@Inject(method = "endBatch(Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/vertex/BufferBuilder;)V",
		at = @At("TAIL"), require = 0)
	private void glintsmith$releaseGlintSlot(RenderType type, BufferBuilder builder, CallbackInfo ci) {
		GlintSystem.onBatchDrawn(type);
	}
}
