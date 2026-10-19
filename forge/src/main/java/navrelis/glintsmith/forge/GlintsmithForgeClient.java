package navrelis.glintsmith.forge;

import navrelis.glintsmith.GlintsmithClient;
import navrelis.glintsmith.gui.GlintsmithCommands;
import navrelis.glintsmith.gui.GlintsmithKeys;
import navrelis.glintsmith.gui.GlintsmithScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * Client side of the Forge layer: starts {@link GlintsmithClient} and forwards the Forge events, the key mappings, the
 * resource reload listeners, the client command and the config screen to it. The client started / client stopping and
 * the world render start / last hooks are mixins ({@code navrelis.glintsmith.forge.mixin}), see the notes there.
 */
final class GlintsmithForgeClient {
	private GlintsmithForgeClient() {
	}

	static void init(FMLJavaModLoadingContext context) {
		GlintsmithClient.init();

		IEventBus modBus = context.getModEventBus();
		modBus.addListener(GlintsmithForgeClient::registerKeyMappings);
		modBus.addListener(GlintsmithForgeClient::registerReloadListeners);

		MinecraftForge.EVENT_BUS.addListener(GlintsmithForgeClient::onClientTick);
		MinecraftForge.EVENT_BUS.addListener(GlintsmithForgeClient::registerCommands);
		MinecraftForge.EVENT_BUS.addListener(GlintsmithForgeClient::onTagsUpdated);

		// The "Config" button of the Mods list.
		context.registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
			() -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> new GlintsmithScreen(parent)));
	}

	private static void registerKeyMappings(RegisterKeyMappingsEvent event) {
		for (KeyMapping mapping : GlintsmithKeys.mappings()) {
			event.register(mapping);
		}
	}

	private static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
		for (GlintsmithClient.NamedReloadListener entry : GlintsmithClient.reloadListeners()) {
			event.registerReloadListener(entry.listener());
		}
	}

	private static void onClientTick(TickEvent.ClientTickEvent.Post event) {
		GlintsmithClient.onClientTickEnd(Minecraft.getInstance());
	}

	private static void registerCommands(RegisterClientCommandsEvent event) {
		event.getDispatcher().register(GlintsmithCommands.<CommandSourceStack>build());
	}

	private static void onTagsUpdated(TagsUpdatedEvent event) {
		GlintsmithClient.onTagsLoaded();
	}
}
