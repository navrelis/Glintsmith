package navrelis.glintsmith;

import java.util.List;

import navrelis.glintsmith.config.ConfigManager;
import navrelis.glintsmith.config.GapFixMode;
import navrelis.glintsmith.config.GlintsmithConfig;
import navrelis.glintsmith.gui.GlintsmithKeys;
import navrelis.glintsmith.gui.GuiBootstrap;
import navrelis.glintsmith.gui.GuiHooks;
import navrelis.glintsmith.modelfix.GapFix;
import navrelis.glintsmith.platform.Services;
import navrelis.glintsmith.render.glint.GlintSystem;
import navrelis.glintsmith.render.glint.GlintTextures;
import navrelis.glintsmith.render.outline.OutlineSystem;
import navrelis.glintsmith.style.RuleEngine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.jetbrains.annotations.Nullable;

import static navrelis.glintsmith.Glintsmith.LOGGER;

/**
 * The loader-neutral client logic of Glintsmith: wires the config, the GUI, the two render systems, the gap fix and the
 * tag reload. A loader layer calls {@link #init()} from its client entry point and forwards the loader's events to the
 * other static methods; none of them has a loader type in its signature. What a loader must register:
 * <ul>
 * <li>the key bindings of {@link GlintsmithKeys#mappings()},</li>
 * <li>the client command {@code navrelis.glintsmith.gui.GlintsmithCommands.build()},</li>
 * <li>the resource reload listeners of {@link #reloadListeners()},</li>
 * <li>the settings screen: the constructor {@code GlintsmithScreen(Screen parent)} is the factory.</li>
 * </ul>
 * The events are, in the order the Fabric layer registers them: client tick end, client started, client stopping, tags
 * loaded, world render start and world render last.
 */
public final class GlintsmithClient {
	/** Id of the mod that does the same job as the gap fix; the automatic mode then stays out of its way. */
	private static final String MODELFIX_MOD_ID = "modelfix";

	/** Set once the client has started; before that a gap fix change needs no resource reload. */
	private static boolean clientStarted;
	/** A gap fix change that still has to be applied by a resource reload. */
	private static boolean reloadPending;
	private static boolean modelfixLogged;
	private static boolean modelfixCustomLogged;

	private GlintsmithClient() {
	}

	/** A resource reload listener and the id the loader registers it under. */
	public record NamedReloadListener(ResourceLocation id, ResourceManagerReloadListener listener) {
	}

	/**
	 * Starts everything that does not need a loader event: the config, the GUI, the two render systems, the gap fix
	 * and the texture list. Call once from the client entry point, before the loader's registrations. Each part is
	 * guarded, so a failure in one cannot stop the others.
	 */
	public static void init() {
		ConfigManager config = ConfigManager.instance();

		guarded("GUI", GuiBootstrap::init);
		guarded("outline system", OutlineSystem::init);
		guarded("glint system", GlintSystem::init);
		guarded("gap fix", () -> initGapFix(config));
		guarded("glint texture list", () -> GuiHooks.texturePresets = GlintTextures::presetSpecs);

		LOGGER.info("Glintsmith initialised.");
	}

	/** End of every client tick: live config reload, a pending gap fix reload, the key bindings. */
	public static void onClientTickEnd(Minecraft client) {
		ConfigManager.instance().tick();
		applyPendingReload(client);
		GlintsmithKeys.onTick(client);
	}

	/** The client has started (the game finished its first resource load). */
	public static void onClientStarted(Minecraft client) {
		OutlineSystem.onClientStarted(client);
		GlintSystem.onClientStarted(client);
		clientStarted = true;
	}

	/** The client is about to stop. */
	public static void onClientStopping(Minecraft client) {
		OutlineSystem.onClientStopping(client);
	}

	/** The tags were (re)loaded: the rule engine forgets what it resolved from tags. */
	public static void onTagsLoaded() {
		RuleEngine.invalidate();
	}

	/**
	 * Start of the world render pass.
	 *
	 * @param consumers the vertex consumer provider the world is drawn into (the main buffer source), may be null
	 */
	public static void onWorldRenderStart(@Nullable MultiBufferSource consumers) {
		OutlineSystem.onWorldStart(consumers);
	}

	/** End of the world render pass (after everything else is drawn, before the hand and the GUI). */
	public static void onWorldRenderLast() {
		OutlineSystem.onWorldLast();
	}

	/**
	 * The client resource reload listeners, in the order they are to be registered: the outline shaders, then the glint
	 * textures. They have no dependencies on other listeners.
	 */
	public static List<NamedReloadListener> reloadListeners() {
		return List.of(
			new NamedReloadListener(OutlineSystem.SHADER_RELOAD_ID, OutlineSystem.shaderReloadListener()),
			new NamedReloadListener(GlintSystem.RELOAD_ID, GlintSystem.textureReloadListener()));
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
		manager.addListener(config -> onGapFixConfig(Minecraft.getInstance(), config));
	}

	/** The gap fix settings that a config asks for. */
	static GapFix.Settings gapFixSettings(GlintsmithConfig config) {
		GlintsmithConfig.GapFix fix = config.gapFix;
		if (!fix.enabled) {
			return GapFix.vanilla();
		}
		if (fix.mode == GapFixMode.CUSTOM) {
			if (Services.PLATFORM.isModLoaded(MODELFIX_MOD_ID) && !modelfixCustomLogged) {
				modelfixCustomLogged = true;
				LOGGER.info("The mod '{}' is installed: in the custom mode the precise spans setting stays inactive because that mod replaces the code it needs. "
					+ "The side expansion, side indent and UV shrink settings are applied on top of that mod's own adjustments, so the two stack. "
					+ "Use the automatic mode (it leaves the seams to that mod) or remove one of the two.", MODELFIX_MOD_ID);
			}
			return new GapFix.Settings(true, fix.uvShrinkMultiplier, fix.sideExpansion, fix.sideIndent, fix.preciseSpans);
		}
		if (Services.PLATFORM.isModLoaded(MODELFIX_MOD_ID)) {
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
