package navrelis.glintsmith.neoforge;

import java.nio.file.Path;

import navrelis.glintsmith.platform.Platform;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.fml.loading.LoadingModList;

/**
 * The {@link Platform} service of the NeoForge build (registered in
 * {@code META-INF/services/navrelis.glintsmith.platform.Platform}).
 *
 * <p>{@code isModLoaded} is asked by the mixin plugin while the mixin configs are loaded, which is before FML has
 * built the {@link ModList}; then the {@link LoadingModList} (mods found by the early loading stage) answers.
 */
public final class NeoForgePlatform implements Platform {
	@Override
	public String loaderName() {
		return "neoforge";
	}

	@Override
	public Path configDir() {
		return FMLPaths.CONFIGDIR.get();
	}

	@Override
	public boolean isModLoaded(String modId) {
		ModList modList = ModList.get();
		if (modList != null) {
			return modList.isLoaded(modId);
		}
		LoadingModList loading = LoadingModList.get();
		return loading != null && loading.getModFileById(modId) != null;
	}
}
