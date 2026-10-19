package navrelis.glintsmith.mixin.outline;

import navrelis.glintsmith.render.outline.OutlineSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * O7: frame begin and end of the outline renderer (O7a, O7b), and the end of level rendering (O7c), where the
 * first-person outline is composited under a shader pack.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererOutlineMixin {
	@Inject(method = "render(Lnet/minecraft/client/DeltaTracker;Z)V", at = @At("HEAD"), require = 0)
	private void glintsmith$outlineFrameBegin(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
		OutlineSystem.onFrameBegin();
	}

	@Inject(method = "render(Lnet/minecraft/client/DeltaTracker;Z)V", at = @At("RETURN"), require = 0)
	private void glintsmith$outlineFrameEnd(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
		OutlineSystem.onFrameEnd();
	}

	@Inject(method = "renderLevel(Lnet/minecraft/client/DeltaTracker;)V", at = @At("RETURN"), require = 0)
	private void glintsmith$outlineFrameLevel(DeltaTracker deltaTracker, CallbackInfo ci) {
		OutlineSystem.onLevelRendered();
	}
}
