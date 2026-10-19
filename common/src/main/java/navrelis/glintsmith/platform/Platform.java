package navrelis.glintsmith.platform;

import java.nio.file.Path;

/**
 * What the common code needs from the mod loader. One implementation per loader build, registered as a
 * {@link java.util.ServiceLoader} service ({@code META-INF/services/navrelis.glintsmith.platform.Platform}) and
 * reached through {@link Services#PLATFORM}.
 */
public interface Platform {
	/**
	 * @return the loader's short name, for logs: {@code fabric}, {@code neoforge} or {@code forge}
	 */
	String loaderName();

	/**
	 * @return the loader's config folder (the one that holds {@code glintsmith.json})
	 */
	Path configDir();

	/**
	 * @return whether the mod with this id is installed and loaded
	 */
	boolean isModLoaded(String modId);
}
