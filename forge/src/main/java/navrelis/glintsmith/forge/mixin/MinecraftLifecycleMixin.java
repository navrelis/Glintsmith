package navrelis.glintsmith.forge.mixin;

import navrelis.glintsmith.GlintsmithClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fires the client started and client stopping hooks at the same points as the Fabric API events
 * {@code ClientLifecycleEvents.CLIENT_STARTED} / {@code CLIENT_STOPPING} (fabric-lifecycle-events-v1
 * {@code MinecraftClientMixin}, translated to Mojang names). Forge has no event at these points: its
 * FMLClientSetupEvent fires during mod loading, before the game loop, and nothing fires when the client stops.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftLifecycleMixin {
	/** Fabric: after "stop" (Mojang: destroy) has logged its first message (the "Stopping!" line). */
	@Inject(method = "destroy", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;info(Ljava/lang/String;)V", shift = At.Shift.AFTER, remap = false), require = 1)
	private void glintsmith$onStopping(CallbackInfo ci) {
		GlintsmithClient.onClientStopping((Minecraft) (Object) this);
	}

	/** Fabric: after the thread field (Mojang: gameThread) is set at the start of "run". */
	@Inject(method = "run", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;gameThread:Ljava/lang/Thread;", shift = At.Shift.AFTER, ordinal = 0), require = 1)
	private void glintsmith$onStarted(CallbackInfo ci) {
		GlintsmithClient.onClientStarted((Minecraft) (Object) this);
	}
}
