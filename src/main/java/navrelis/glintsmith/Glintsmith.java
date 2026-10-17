package navrelis.glintsmith;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client entry point of Glintsmith.
 */
public final class Glintsmith implements ClientModInitializer {
	public static final String MOD_ID = "glintsmith";

	public static final Logger LOGGER = LoggerFactory.getLogger("Glintsmith");

	static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitializeClient() {
		LOGGER.info("Glintsmith initialised.");
	}
}
