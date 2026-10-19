package navrelis.glintsmith.neoforge.mixin;

import navrelis.glintsmith.GlintsmithClient;
import navrelis.glintsmith.Glintsmith;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Client started and client stopping, at the same points Fabric API's {@code MinecraftClientMixin}
 * (fabric-lifecycle-events-v1) fires {@code ClientLifecycleEvents.CLIENT_STARTED} and {@code CLIENT_STOPPING}.
 * NeoForge has no event at these points: {@code GameShuttingDownEvent} fires from {@code Minecraft.stop()}, which is
 * the request to quit, before the shutdown, and nothing fires at the start of the main loop.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftLifecycleMixin {
	/** Fabric: after the first access of the thread field in {@code run()}, so the game thread is set already. */
	@Inject(
		method = "run",
		at = @At(
			value = "FIELD",
			target = "Lnet/minecraft/client/Minecraft;gameThread:Ljava/lang/Thread;",
			shift = At.Shift.AFTER,
			ordinal = 0
		)
	)
	private void glintsmith$onClientStarted(CallbackInfo ci) {
		Glintsmith.LOGGER.debug("Glintsmith: client started hook fired (MinecraftLifecycleMixin).");
		GlintsmithClient.onClientStarted((Minecraft) (Object) this);
	}

	/** Fabric: after the "Stopping!" log line (Yarn {@code MinecraftClient.stop()} is {@code destroy()} here). */
	@Inject(
		method = "destroy",
		at = @At(
			value = "INVOKE",
			target = "Lorg/slf4j/Logger;info(Ljava/lang/String;)V",
			shift = At.Shift.AFTER,
			remap = false
		)
	)
	private void glintsmith$onClientStopping(CallbackInfo ci) {
		Glintsmith.LOGGER.debug("Glintsmith: client stopping hook fired (MinecraftLifecycleMixin).");
		GlintsmithClient.onClientStopping((Minecraft) (Object) this);
	}
}
