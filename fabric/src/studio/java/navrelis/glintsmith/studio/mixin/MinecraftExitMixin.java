package navrelis.glintsmith.studio.mixin;

import navrelis.glintsmith.studio.StudioMod;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * The process exit code of a studio run: vanilla always exits with 0 after a clean shutdown; a failed run exits with
 * {@link StudioMod#exitCode()} instead.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftExitMixin {
	@Redirect(method = "destroy", at = @At(value = "INVOKE", target = "Ljava/lang/System;exit(I)V"))
	private void glintsmithStudio$exit(int status) {
		System.exit(status == 0 ? StudioMod.exitCode() : status);
	}
}
