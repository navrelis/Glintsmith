package navrelis.glintsmith.studio.mixin;

import navrelis.glintsmith.studio.StudioClock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Studio clock for the glint scroll of Glintsmith's glint slots (custom and vanilla-matrix fallback).
 */
@Mixin(targets = "navrelis.glintsmith.render.glint.GlintShards")
public abstract class GlintShardsClockMixin {
	@Redirect(method = {"setupTexturing", "vanillaTexturing"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/Util;getMillis()J"),
		require = 2)
	private static long glintsmithStudio$millis() {
		return StudioClock.millis();
	}
}
