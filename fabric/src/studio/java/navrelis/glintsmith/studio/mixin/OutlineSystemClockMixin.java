package navrelis.glintsmith.studio.mixin;

import navrelis.glintsmith.render.outline.OutlineSystem;
import navrelis.glintsmith.studio.StudioClock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Studio clock for the frame time of the outline system (animated outline colours).
 */
@Mixin(OutlineSystem.class)
public abstract class OutlineSystemClockMixin {
	@Redirect(method = {"onFrameBegin", "time"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/Util;getMillis()J"),
		require = 2)
	private static long glintsmithStudio$millis() {
		return StudioClock.millis();
	}
}
