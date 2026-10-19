package navrelis.glintsmith.smoke.forge;

import navrelis.glintsmith.smoke.SmokeRunner;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Forge wiring of the smoke test (dev-only mod, never in the released jar): everything else is in
 * {@code common/src/smoke}. {@code SmokeRunner.init()} and {@code configureOptions} are called by
 * {@link navrelis.glintsmith.smoke.forge.mixin.SmokeClientStartMixin} at the start of the main loop (Forge has no client
 * started event). The mod is only ever put on a client.
 */
@Mod("glintsmith_smoke")
public final class SmokeMod {
	public SmokeMod() {
		MinecraftForge.EVENT_BUS.addListener(SmokeMod::onClientTickEnd);
	}

	private static void onClientTickEnd(TickEvent.ClientTickEvent.Post event) {
		SmokeRunner.tick(Minecraft.getInstance());
	}
}
