package navrelis.glintsmith.mixin.glint;

import navrelis.glintsmith.render.glint.GlintSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * G6: frame start for the glint slot pool.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererGlintMixin {
	@Inject(method = "render(Lnet/minecraft/client/DeltaTracker;Z)V", at = @At("HEAD"), require = 0)
	private void glintsmith$glintFrameBegin(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
		GlintSystem.onFrameBegin();
	}
}
