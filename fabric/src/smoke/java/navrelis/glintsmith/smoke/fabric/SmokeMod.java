package navrelis.glintsmith.smoke.fabric;

import navrelis.glintsmith.smoke.SmokeRunner;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/**
 * Fabric wiring of the smoke test (dev-only mod, never in the released jar): everything else is in
 * {@code common/src/smoke}.
 */
public final class SmokeMod implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		SmokeRunner.init();
		ClientLifecycleEvents.CLIENT_STARTED.register(SmokeRunner::configureOptions);
		ClientTickEvents.END_CLIENT_TICK.register(SmokeRunner::tick);
	}
}
