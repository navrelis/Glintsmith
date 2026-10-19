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
 * the vanilla glint, and frees them once drawn. G5c also does the flush of G5b for subclasses of the main source that
 * override the public {@code endBatch(RenderType)} without calling it (ImmediatelyFast, see G9): they still draw
 * through the private {@code endBatch(RenderType, BufferBuilder)}.
 * <p>
 * G11: ImmediatelyFast's non-batching GUI source is a plain instance of this class, so G5a also sees its requests; they
 * go to {@link GlintSystem#substituteGui} instead.
 */
@Mixin(MultiBufferSource.BufferSource.class)
public abstract class BufferSourceGlintMixin {
	@ModifyVariable(method = "getBuffer(Lnet/minecraft/client/renderer/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;",
		at = @At("HEAD"), argsOnly = true, require = 0)
	private RenderType glintsmith$substituteGlint(RenderType requested) {
		MultiBufferSource.BufferSource self = (MultiBufferSource.BufferSource) (Object) this;
		return GlintSystem.isGuiSource(self) ? GlintSystem.substituteGui(self, requested) : GlintSystem.substitute(self, requested);
	}

	@Inject(method = "endBatch(Lnet/minecraft/client/renderer/RenderType;)V", at = @At("TAIL"), require = 0)
	private void glintsmith$flushGlintSlots(RenderType type, CallbackInfo ci) {
		GlintSystem.afterEndBatch((MultiBufferSource.BufferSource) (Object) this, type);
	}

	@Inject(method = "endBatch(Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/vertex/BufferBuilder;)V",
		at = @At("TAIL"), require = 0)
	private void glintsmith$releaseGlintSlot(RenderType type, BufferBuilder builder, CallbackInfo ci) {
		GlintSystem.onBatchDrawn((MultiBufferSource.BufferSource) (Object) this, type);
	}
}
