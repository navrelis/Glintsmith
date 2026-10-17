package navrelis.glintsmith.render.outline;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

/**
 * Bootstraps the game registries once per test JVM. {@code RenderType}'s static initialiser reaches
 * {@code ItemRenderer} and through it {@code Items} and {@code Blocks}, which need bootstrapped registries.
 * Nothing here needs a GL context.
 */
final class GameBootstrap {
	private GameBootstrap() {
	}

	static void ensure() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap(); // idempotent
	}
}
