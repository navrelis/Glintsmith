package navrelis.glintsmith.studio.mixin;

import navrelis.glintsmith.studio.StudioClock;
import net.minecraft.client.renderer.RenderStateShard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Studio clock for the vanilla glint scroll (glints that Glintsmith leaves vanilla).
 */
@Mixin(RenderStateShard.class)
public abstract class RenderStateShardClockMixin {
	@Redirect(method = "setupGlintTexturing", at = @At(value = "INVOKE", target = "Lnet/minecraft/Util;getMillis()J"))
	private static long glintsmithStudio$millis() {
		return StudioClock.millis();
	}
}
