package navrelis.glintsmith.fabric;

import navrelis.glintsmith.GlintsmithClient;
import navrelis.glintsmith.gui.GlintsmithCommands;
import navrelis.glintsmith.gui.GlintsmithKeys;
import navrelis.glintsmith.render.outline.SodiumTeeVertexConsumer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.slf4j.LoggerFactory;

/**
 * Fabric client entry point of Glintsmith: starts {@link GlintsmithClient} and forwards the Fabric events,
 * resource reload listeners, key bindings and the client command to it.
 */
public final class GlintsmithFabric implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		GlintsmithClient.init();
		if (FabricLoader.getInstance().isModLoaded("sodium")) {
			installSodiumTees();
		}

		for (KeyMapping mapping : GlintsmithKeys.mappings()) {
			KeyBindingHelper.registerKeyBinding(mapping);
		}
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) ->
			dispatcher.register(GlintsmithCommands.<FabricClientCommandSource>build()));

		ClientTickEvents.END_CLIENT_TICK.register(GlintsmithClient::onClientTickEnd);
		ClientLifecycleEvents.CLIENT_STARTED.register(GlintsmithClient::onClientStarted);
		ClientLifecycleEvents.CLIENT_STOPPING.register(GlintsmithClient::onClientStopping);
		CommonLifecycleEvents.TAGS_LOADED.register((registries, client) -> GlintsmithClient.onTagsLoaded());
		WorldRenderEvents.START.register(context -> GlintsmithClient.onWorldRenderStart(context.consumers()));
		WorldRenderEvents.LAST.register(context -> GlintsmithClient.onWorldRenderLast());

		for (GlintsmithClient.NamedReloadListener entry : GlintsmithClient.reloadListeners()) {
			ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new Listener(entry.id(), entry.listener()));
		}
	}

	/**
	 * Outline tees that implement Sodium's vertex writer (no Sodium warning, Sodium's fast paths stay on). Its own
	 * method, so that the class (which needs Sodium's API) is only resolved when Sodium is loaded.
	 */
	private static void installSodiumTees() {
		try {
			SodiumTeeVertexConsumer.install();
		} catch (Throwable t) {
			// Another Sodium API: the plain tees stay (Sodium then logs one warning and uses its slow path).
			LoggerFactory.getLogger("Glintsmith/Outline").warn("[Glintsmith] Sodium vertex writer for outlines unavailable: {}", String.valueOf(t));
		}
	}

	/** Gives a vanilla reload listener the Fabric id that Fabric API needs to register it. */
	private record Listener(ResourceLocation id, ResourceManagerReloadListener delegate)
		implements SimpleSynchronousResourceReloadListener {
		@Override
		public ResourceLocation getFabricId() {
			return id;
		}

		@Override
		public void onResourceManagerReload(ResourceManager manager) {
			delegate.onResourceManagerReload(manager);
		}
	}
}
