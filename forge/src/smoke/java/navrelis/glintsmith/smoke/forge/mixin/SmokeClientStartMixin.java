package navrelis.glintsmith.smoke.forge.mixin;

import navrelis.glintsmith.smoke.SmokeRunner;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Starts the smoke run when the client has started: the same point as the lifecycle mixin of the main mod
 * ({@code MinecraftLifecycleMixin}, which is where Fabric API fires CLIENT_STARTED).
 */
@Mixin(Minecraft.class)
abstract class SmokeClientStartMixin {
	@Inject(
		method = "run",
		at = @At(
			value = "FIELD",
			target = "Lnet/minecraft/client/Minecraft;gameThread:Ljava/lang/Thread;",
			shift = At.Shift.AFTER,
			ordinal = 0
		)
	)
	private void glintsmithSmoke$onClientStarted(CallbackInfo ci) {
		SmokeRunner.init();
		SmokeRunner.configureOptions((Minecraft) (Object) this);
	}
}
