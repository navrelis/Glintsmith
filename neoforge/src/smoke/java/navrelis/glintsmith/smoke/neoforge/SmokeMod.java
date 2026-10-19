package navrelis.glintsmith.smoke.neoforge;

import navrelis.glintsmith.smoke.SmokeRunner;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * NeoForge wiring of the smoke test (dev-only mod, never in the released jar): everything else is in
 * {@code common/src/smoke}. {@code SmokeRunner.init()} and {@code configureOptions} are called by
 * {@link navrelis.glintsmith.smoke.neoforge.mixin.SmokeClientStartMixin} at the start of the main loop (NeoForge has no
 * client started event).
 */
@Mod(value = "glintsmith_smoke", dist = Dist.CLIENT)
public final class SmokeMod {
	public SmokeMod() {
		NeoForge.EVENT_BUS.addListener(SmokeMod::onClientTickEnd);
	}

	private static void onClientTickEnd(ClientTickEvent.Post event) {
		SmokeRunner.tick(Minecraft.getInstance());
	}
}
