package navrelis.glintsmith.forge;

import java.nio.file.Path;

import navrelis.glintsmith.platform.Platform;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.loading.LoadingModList;

/**
 * The {@link Platform} service of the Forge build. Registered in
 * {@code META-INF/services/navrelis.glintsmith.platform.Platform}.
 */
public final class ForgePlatform implements Platform {
	@Override
	public String loaderName() {
		return "forge";
	}

	@Override
	public Path configDir() {
		return FMLPaths.CONFIGDIR.get();
	}

	/**
	 * Works from the very first moment: the mixin plugin asks while the mixin configs load, long before
	 * {@link ModList} exists, so until then the list of discovered mods ({@link LoadingModList}) answers.
	 */
	@Override
	public boolean isModLoaded(String modId) {
		ModList loaded = ModList.get();
		if (loaded != null) {
			return loaded.isLoaded(modId);
		}
		LoadingModList loading = FMLLoader.getLoadingModList();
		return loading != null && loading.getModFileById(modId) != null;
	}
}
