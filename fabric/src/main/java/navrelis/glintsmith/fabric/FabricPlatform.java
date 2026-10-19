package navrelis.glintsmith.fabric;

import java.nio.file.Path;

import navrelis.glintsmith.platform.Platform;
import net.fabricmc.loader.api.FabricLoader;

/**
 * The {@link Platform} service of the Fabric build (registered in
 * {@code META-INF/services/navrelis.glintsmith.platform.Platform}).
 */
public final class FabricPlatform implements Platform {
	@Override
	public String loaderName() {
		return "fabric";
	}

	@Override
	public Path configDir() {
		return FabricLoader.getInstance().getConfigDir();
	}

	@Override
	public boolean isModLoaded(String modId) {
		return FabricLoader.getInstance().isModLoaded(modId);
	}
}
