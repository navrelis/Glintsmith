package navrelis.glintsmith;

import navrelis.glintsmith.config.ConfigManager;
import navrelis.glintsmith.config.GapFixMode;
import navrelis.glintsmith.config.GlintsmithConfig;
import navrelis.glintsmith.gui.GuiBootstrap;
import navrelis.glintsmith.gui.GuiHooks;
import navrelis.glintsmith.modelfix.GapFix;
import navrelis.glintsmith.render.glint.GlintSystem;
import navrelis.glintsmith.render.glint.GlintTextures;
import navrelis.glintsmith.render.outline.OutlineSystem;
import navrelis.glintsmith.style.RuleEngine;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client entry point of Glintsmith. Wires the config, the GUI, the two render systems, the gap fix and the tag reload.
 */
public final class Glintsmith implements ClientModInitializer {
	public static final String MOD_ID = "glintsmith";

	public static final Logger LOGGER = LoggerFactory.getLogger("Glintsmith");

	/** Id of the mod that does the same job as the gap fix; the automatic mode then stays out of its way. */
	private static final String MODELFIX_MOD_ID = "modelfix";

	/** Set once the client has started; before that a gap fix change needs no resource reload. */
	private static boolean clientStarted;
	/** A gap fix change that still has to be applied by a resource reload. */
	private static boolean reloadPending;
	private static boolean modelfixLogged;

	@Override
	public void onInitializeClient() {
		ConfigManager config = ConfigManager.instance();
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			config.tick();
			applyPendingReload(client);
		});

		guarded("GUI", GuiBootstrap::init);
		guarded("outline system", OutlineSystem::init);
		guarded("glint system", GlintSystem::init);
		guarded("gap fix", () -> initGapFix(config));
		guarded("glint texture list", () -> GuiHooks.texturePresets = GlintTextures::presetSpecs);
		guarded("tag reload", () -> CommonLifecycleEvents.TAGS_LOADED.register((registries, client) -> RuleEngine.invalidate()));

		LOGGER.info("Glintsmith initialised.");
	}

	/** Runs one part of the start-up so that a failure in it cannot stop the others. */
	private static void guarded(String what, Runnable step) {
		try {
			step.run();
		} catch (Throwable t) {
			LOGGER.error("Glintsmith: {} failed to start and stays off.", what, t);
		}
	}

	// ---------------------------------------------------------------- gap fix

	private static void initGapFix(ConfigManager manager) {
		GapFix.apply(gapFixSettings(manager.get()));
		ClientLifecycleEvents.CLIENT_STARTED.register(client -> clientStarted = true);
		manager.addListener(config -> onGapFixConfig(Minecraft.getInstance(), config));
	}

	/** The gap fix settings that a config asks for. */
	static GapFix.Settings gapFixSettings(GlintsmithConfig config) {
		GlintsmithConfig.GapFix fix = config.gapFix;
		if (!fix.enabled) {
			return GapFix.vanilla();
		}
		if (fix.mode == GapFixMode.CUSTOM) {
			return new GapFix.Settings(true, fix.uvShrinkMultiplier, fix.sideExpansion, fix.sideIndent, fix.preciseSpans);
		}
		if (FabricLoader.getInstance().isModLoaded(MODELFIX_MOD_ID)) {
			if (!modelfixLogged) {
				modelfixLogged = true;
				LOGGER.info("The mod '{}' is installed: the automatic gap fix stays off. Use the custom mode to override.",
					MODELFIX_MOD_ID);
			}
			return GapFix.vanilla();
		}
		return GapFix.autoSettings();
	}

	private static void onGapFixConfig(Minecraft client, GlintsmithConfig config) {
		GapFix.Settings old = GapFix.current();
		GapFix.Settings next = gapFixSettings(config);
		GapFix.apply(next);
		if (!GapFix.requiresReload(old, next) || !clientStarted) {
			return;
		}
		client.execute(() -> {
			reloadPending = true;
			applyPendingReload(client);
		});
	}

	/** Reloads the resources for a pending gap fix change, but never while the game is still loading them. */
	private static void applyPendingReload(Minecraft client) {
		if (!reloadPending || !clientStarted || client.getOverlay() != null) {
			return;
		}
		reloadPending = false;
		client.reloadResourcePacks();
	}
}
