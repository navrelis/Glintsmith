package navrelis.glintsmith.forge;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * Forge entry point of Glintsmith. The mod is client-only: on a dedicated server nothing happens, and the client
 * classes (which are only referenced from {@link GlintsmithForgeClient}) are never loaded.
 */
@Mod("glintsmith")
public final class GlintsmithForge {
	public GlintsmithForge(FMLJavaModLoadingContext context) {
		if (FMLEnvironment.dist == Dist.CLIENT) {
			GlintsmithForgeClient.init(context);
		}
	}
}
