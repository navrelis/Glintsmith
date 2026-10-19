package navrelis.glintsmith.neoforge;

import navrelis.glintsmith.GlintsmithClient;
import navrelis.glintsmith.gui.GlintsmithCommands;
import navrelis.glintsmith.gui.GlintsmithKeys;
import navrelis.glintsmith.gui.GlintsmithScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TagsUpdatedEvent;

/**
 * NeoForge client entry point of Glintsmith: starts {@link GlintsmithClient} and forwards the NeoForge events,
 * resource reload listeners, key mappings and the client command to it.
 *
 * <p>Client started, client stopping and the two world render hooks have no NeoForge event at the point Fabric API
 * fires them, so they come from {@link navrelis.glintsmith.neoforge.mixin.MinecraftLifecycleMixin} and
 * {@link navrelis.glintsmith.neoforge.mixin.LevelRendererHooksMixin}.
 */
@Mod(value = "glintsmith", dist = Dist.CLIENT)
public final class GlintsmithNeoForge {
	public GlintsmithNeoForge(IEventBus modBus, ModContainer container) {
		GlintsmithClient.init();

		container.registerExtensionPoint(IConfigScreenFactory.class,
			(modContainer, parent) -> new GlintsmithScreen(parent));

		modBus.addListener(GlintsmithNeoForge::onRegisterKeyMappings);
		modBus.addListener(GlintsmithNeoForge::onRegisterReloadListeners);

		NeoForge.EVENT_BUS.addListener(GlintsmithNeoForge::onClientTickEnd);
		NeoForge.EVENT_BUS.addListener(GlintsmithNeoForge::onRegisterClientCommands);
		NeoForge.EVENT_BUS.addListener(GlintsmithNeoForge::onTagsUpdated);
	}

	private static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
		for (KeyMapping mapping : GlintsmithKeys.mappings()) {
			event.register(mapping);
		}
	}

	private static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
		for (GlintsmithClient.NamedReloadListener entry : GlintsmithClient.reloadListeners()) {
			event.registerReloadListener(entry.listener());
		}
	}

	private static void onClientTickEnd(ClientTickEvent.Post event) {
		GlintsmithClient.onClientTickEnd(Minecraft.getInstance());
	}

	private static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
		event.getDispatcher().register(GlintsmithCommands.<CommandSourceStack>build());
	}

	private static void onTagsUpdated(TagsUpdatedEvent event) {
		GlintsmithClient.onTagsLoaded();
	}
}
