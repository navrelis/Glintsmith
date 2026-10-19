package navrelis.glintsmith.mixin.outline;

import navrelis.glintsmith.render.outline.OutlineSystem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * O6: finds the world composite point, the water-mask flush of the main buffer source, and ignores it while the
 * whole source is being flushed by {@code endBatch()}.
 */
@Mixin(MultiBufferSource.BufferSource.class)
public abstract class BufferSourceOutlineMixin {
	@Inject(method = "endBatch()V", at = @At("HEAD"), require = 0)
	private void glintsmith$outlineFlushHead(CallbackInfo ci) {
		OutlineSystem.onFullFlush((MultiBufferSource.BufferSource) (Object) this, true);
	}

	@Inject(method = "endBatch()V", at = @At("TAIL"), require = 0)
	private void glintsmith$outlineFlushTail(CallbackInfo ci) {
		OutlineSystem.onFullFlush((MultiBufferSource.BufferSource) (Object) this, false);
	}

	@Inject(method = "endBatch(Lnet/minecraft/client/renderer/RenderType;)V", at = @At("TAIL"), require = 0)
	private void glintsmith$outlineFlushType(RenderType type, CallbackInfo ci) {
		OutlineSystem.onEndBatch((MultiBufferSource.BufferSource) (Object) this, type);
	}
}
