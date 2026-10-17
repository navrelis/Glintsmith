package navrelis.glintsmith.studio.mixin;

import navrelis.glintsmith.render.glint.GlintSystem;
import navrelis.glintsmith.studio.StudioClock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Studio clock for the frame time of the glint system (animated glint tints).
 */
@Mixin(GlintSystem.class)
public abstract class GlintSystemClockMixin {
	@Redirect(method = "onFrameBegin", at = @At(value = "INVOKE", target = "Lnet/minecraft/Util;getMillis()J"))
	private static long glintsmithStudio$millis() {
		return StudioClock.millis();
	}
}
