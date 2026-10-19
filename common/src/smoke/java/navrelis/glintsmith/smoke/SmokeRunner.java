package navrelis.glintsmith.smoke;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import navrelis.glintsmith.config.ConfigManager;
import navrelis.glintsmith.config.GlintsmithConfig;
import navrelis.glintsmith.gui.GlintsmithScreen;
import navrelis.glintsmith.platform.Services;
import net.minecraft.client.CameraType;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorPreset;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorPresets;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The automatic in-game smoke test (docs/TESTING.md). A loader wrapper (a dev-only mod, never in the released jar)
 * calls {@link #init()}, {@link #configureOptions(Minecraft)} once the client has started and {@link #tick(Minecraft)} at
 * the end of every client tick. From the title screen the runner creates a fresh superflat world, builds the test scene
 * ({@link SmokeScene}), takes the screenshots, writes {@code smoke-result.json} into the run folder and stops the game.
 * <p>
 * Every step is guarded: an exception is recorded as the failure of that step and the run goes on with the next one.
 * A watchdog thread ends a hung run. The Glintsmith config of the run comes from {@code config/glintsmith.json}, which the
 * run task replaces before every launch.
 */
public final class SmokeRunner {
	static final Logger LOGGER = LoggerFactory.getLogger("Glintsmith/Smoke");
	static final String PREFIX = "[Glintsmith Smoke] ";

	/** The static outline colour of glintsmith-smoke.json; the screenshots are checked for pixels of this colour. */
	private static final int OUTLINE_R = 0x00;
	private static final int OUTLINE_G = 0xFF;
	private static final int OUTLINE_B = 0x40;

	private static final int MENU_TIMEOUT_TICKS = 20 * 120;
	private static final int LOAD_TIMEOUT_TICKS = 20 * 120;
	private static final int STEP_TIMEOUT_TICKS = 20 * 30;
	private static final int SETTLE_AFTER_LOAD_TICKS = 40;
	private static final long DEFAULT_TIMEOUT_SECONDS = 420L;
	/**
	 * Minimum |cos(spin)| of the dropped item when a picture is taken: its flat side is turned at most about 30 degrees away
	 * from the camera (which looks along the z axis). The item spins with its age, once around in about 6 seconds.
	 */
	private static final double FACE_ON = 0.85;

	private static SmokeRunner instance;

	private enum Phase {
		MENU,
		LOADING,
		SETTLE,
		STEPS,
		DONE
	}

	@FunctionalInterface
	private interface Enter {
		void run(Minecraft client) throws Exception;
	}

	/** One unit of work: {@code enter} runs once, then the step waits {@code settle} ticks (and for {@code ready}) before it takes its screenshot. */
	private record Step(String name, boolean screenshot, int settle, Enter enter, Predicate<Minecraft> ready) {
	}

	private final Path runDir;
	private final Path shotDir;
	private final String worldName;
	private final SmokeResult result;
	private final SmokeScene scene = new SmokeScene();
	private final List<Step> steps = new ArrayList<>();
	private Phase phase = Phase.MENU;
	private int phaseTicks;
	private int stepIndex = -1;
	private int stepTicks;
	private long stepStartedNanos;
	private volatile boolean sceneFailed;
	private volatile String sceneError;

	private SmokeRunner(Path runDir) {
		this.runDir = runDir;
		this.shotDir = runDir.resolve("screenshots");
		this.worldName = "GlintsmithSmoke-" + System.currentTimeMillis();
		String minecraft = System.getProperty("glintsmith.smoke.minecraft", "unknown");
		String version = System.getProperty("glintsmith.smoke.version", "unknown");
		String loader;
		try {
			loader = Services.PLATFORM.loaderName();
		} catch (Throwable t) {
			loader = "unknown (" + t + ")";
		}
		this.result = new SmokeResult(runDir.resolve("smoke-result.json"), minecraft, loader, version);
		result.put("world", worldName);
		buildSteps();
	}

	/** Called once from the client entry point of the loader wrapper. */
	public static synchronized void init() {
		if (instance != null) {
			return;
		}
		Path runDir = Minecraft.getInstance().gameDirectory.toPath().toAbsolutePath().normalize();
		instance = new SmokeRunner(runDir);
		instance.startWatchdog();
		LOGGER.info("{}Smoke run '{}' in {}", PREFIX, instance.worldName, runDir);
	}

	/** Options of the unattended run, set in memory once the client has started (options.txt of the run folder is rewritten). */
	public static void configureOptions(Minecraft client) {
		try {
			var options = client.options;
			options.pauseOnLostFocus = false;
			options.onboardAccessibility = false;
			options.joinedFirstServer = true;
			options.skipMultiplayerWarning = true;
			options.hideGui = false;
			options.tutorialStep = TutorialSteps.NONE;
			options.graphicsMode().set(GraphicsStatus.FANCY);
			options.renderDistance().set(5);
			options.simulationDistance().set(5);
			options.guiScale().set(2);
			options.fov().set(70);
			options.bobView().set(false);
			options.framerateLimit().set(120);
			options.enableVsync().set(false);
			options.getSoundSourceOptionInstance(SoundSource.MASTER).set(0.0);
			options.realmsNotifications().set(false);
			options.setCameraType(CameraType.FIRST_PERSON);
			client.getWindow().updateVsync(false);
		} catch (Throwable t) {
			LOGGER.error("{}could not set the options of the run", PREFIX, t);
		}
	}

	/** Called at the end of every client tick. */
	public static void tick(Minecraft client) {
		SmokeRunner runner = instance;
		if (runner == null) {
			return;
		}
		try {
			runner.advance(client);
		} catch (Throwable t) {
			LOGGER.error("{}the run failed", PREFIX, t);
			runner.finish(client, "uncaught exception: " + t);
		}
	}

	// ------------------------------------------------------------------ steps

	private void buildSteps() {
		steps.add(new Step("config", false, 1, this::checkConfig, null));
		steps.add(new Step("scene", false, 20, this::buildScene, this::sceneReady));
		steps.add(new Step("01_first_person", true, 20, c -> pose(c, scene.firstPerson(), CameraType.FIRST_PERSON), this::droppedFaceOn));
		steps.add(new Step("05_dropped_closeup", true, 15, c -> pose(c, scene.droppedCloseUp(), CameraType.FIRST_PERSON), this::droppedFaceOn));
		steps.add(new Step("06_item_frame_closeup", true, 15, c -> pose(c, scene.frameCloseUp(), CameraType.FIRST_PERSON), null));
		steps.add(new Step("03_third_person_front", true, 35, c -> pose(c, scene.thirdPersonFront(), CameraType.THIRD_PERSON_FRONT), this::droppedFaceOn));
		// The creative player gets the creative inventory screen; the survival one shows the worn armor and the player
		steps.add(new Step("survival", false, 5, c -> {
			MinecraftServer server = c.getSingleplayerServer();
			if (server == null) {
				throw new IllegalStateException("no integrated server");
			}
			server.execute(() -> SmokeScene.setGameMode(server, GameType.SURVIVAL));
		}, c -> c.gameMode != null && c.gameMode.getPlayerMode() == GameType.SURVIVAL));
		steps.add(new Step("02_inventory", true, 20, c -> {
			pose(c, scene.firstPerson(), CameraType.FIRST_PERSON);
			c.setScreen(new InventoryScreen(c.player));
		}, null));
		steps.add(new Step("04_glintsmith_screen", true, 40, c -> c.setScreen(new GlintsmithScreen(null)), null));
	}

	/** The config of the run must be the smoke config, loaded without repairs. */
	private void checkConfig(Minecraft client) {
		ConfigManager manager = ConfigManager.instance();
		GlintsmithConfig config = manager.get();
		JsonObject info = new JsonObject();
		info.addProperty("file", manager.path().toString());
		info.addProperty("outlineEnabled", config.outline.enabled);
		info.addProperty("outlineThickness", config.outline.thickness);
		info.addProperty("outlineOpacity", config.outline.opacity);
		info.addProperty("glintItemTexture", String.valueOf(config.glint.item.texture));
		JsonArray warnings = new JsonArray();
		manager.warnings().forEach(warnings::add);
		info.add("warnings", warnings);
		if (manager.lastError() != null) {
			info.addProperty("lastError", manager.lastError());
		}
		result.put("config", info);
		if (!config.outline.enabled || config.outline.thickness != 6.0f || !"preset:rainbow".equals(config.glint.item.texture)) {
			throw new IllegalStateException("the smoke config (glintsmith-smoke.json) is not the active config: outline enabled="
				+ config.outline.enabled + ", thickness=" + config.outline.thickness + ", item glint=" + config.glint.item.texture);
		}
		if (manager.lastError() != null || !manager.warnings().isEmpty()) {
			throw new IllegalStateException("the config needed repairs: error=" + manager.lastError() + ", warnings=" + manager.warnings());
		}
	}

	private void buildScene(Minecraft client) {
		MinecraftServer server = client.getSingleplayerServer();
		if (server == null) {
			throw new IllegalStateException("no integrated server");
		}
		server.execute(() -> {
			try {
				scene.build(server);
			} catch (Throwable t) {
				LOGGER.error("{}the scene could not be built", PREFIX, t);
				sceneError = t.toString();
				sceneFailed = true;
			}
		});
	}

	/** The scene exists on the server, the client has received it and the chunk sections around are rendered. */
	private boolean sceneReady(Minecraft client) {
		if (sceneFailed) {
			throw new IllegalStateException("scene setup failed: " + sceneError);
		}
		if (!scene.built || client.level == null || client.player == null) {
			return false;
		}
		ItemEntity dropped = null;
		boolean frame = false;
		for (Entity entity : client.level.entitiesForRendering()) {
			if (entity instanceof ItemEntity item && item.getItem().is(Items.GOLDEN_SWORD)) {
				dropped = item;
			}
			frame |= entity instanceof ItemFrame itemFrame && itemFrame.getItem().is(Items.DIAMOND_PICKAXE);
		}
		if (dropped == null || !frame || !client.player.getMainHandItem().is(Items.NETHERITE_SWORD)
			|| !client.levelRenderer.hasRenderedAllSections()) {
			return false;
		}
		return true;
	}

	/** The dropped item shows its flat side to the camera (it spins; a picture of its edge would show nothing). */
	private boolean droppedFaceOn(Minecraft client) {
		if (client.level == null) {
			return false;
		}
		for (Entity entity : client.level.entitiesForRendering()) {
			if (entity instanceof ItemEntity item && item.getItem().is(Items.GOLDEN_SWORD)) {
				return Math.abs(Math.cos(item.getSpin(0.5f))) >= FACE_ON;
			}
		}
		return false;
	}

	private void pose(Minecraft client, SmokeScene.Pose pose, CameraType camera) {
		MinecraftServer server = client.getSingleplayerServer();
		if (server == null) {
			throw new IllegalStateException("no integrated server");
		}
		client.setScreen(null);
		client.options.setCameraType(camera);
		server.execute(() -> SmokeScene.teleport(server, pose));
	}

	// ------------------------------------------------------------------ state machine

	private void advance(Minecraft client) {
		if (phase == Phase.DONE) {
			return;
		}
		phaseTicks++;
		switch (phase) {
			case MENU -> {
				if (client.getOverlay() == null && client.level == null
					&& (client.screen instanceof TitleScreen || client.screen instanceof AccessibilityOnboardingScreen)) {
					client.options.onboardAccessibility = false;
					createWorld(client);
					phase = Phase.LOADING;
					phaseTicks = 0;
				} else if (phaseTicks > MENU_TIMEOUT_TICKS) {
					finish(client, "the title screen did not appear within " + MENU_TIMEOUT_TICKS / 20 + " s (screen: "
						+ (client.screen == null ? "none" : client.screen.getClass().getSimpleName()) + ")");
				}
			}
			case LOADING -> {
				if (client.level != null && client.player != null && client.screen == null && client.getSingleplayerServer() != null
					&& client.levelRenderer.hasRenderedAllSections()) {
					LOGGER.info("{}World '{}' loaded.", PREFIX, worldName);
					phase = Phase.SETTLE;
					phaseTicks = 0;
				} else if (client.level == null && client.screen instanceof TitleScreen && phaseTicks > 60) {
					finish(client, "the world could not be opened (back on the title screen)");
				} else if (phaseTicks > LOAD_TIMEOUT_TICKS) {
					finish(client, "the world did not load within " + LOAD_TIMEOUT_TICKS / 20 + " s");
				}
			}
			case SETTLE -> {
				clearNotifications(client);
				if (phaseTicks >= SETTLE_AFTER_LOAD_TICKS) {
					phase = Phase.STEPS;
					phaseTicks = 0;
				}
			}
			case STEPS -> {
				clearNotifications(client);
				advanceStep(client);
			}
			default -> {
			}
		}
	}

	/** Advancement and recipe toasts and their chat lines would cover the pictures; they are removed before every frame. */
	private static void clearNotifications(Minecraft client) {
		try {
			client.getToasts().clear();
			client.gui.getChat().clearMessages(false);
		} catch (Throwable ignored) {
			// cosmetic only
		}
	}

	private void advanceStep(Minecraft client) {
		if (stepIndex < 0 || stepIndex >= steps.size()) {
			if (stepIndex >= steps.size()) {
				finish(client, null);
				return;
			}
			startStep(client, 0);
			return;
		}
		Step step = steps.get(stepIndex);
		stepTicks++;
		if (stepTicks < step.settle()) {
			return;
		}
		try {
			if (step.ready() != null && !step.ready().test(client)) {
				if (stepTicks > STEP_TIMEOUT_TICKS) {
					throw new IllegalStateException("not ready after " + STEP_TIMEOUT_TICKS / 20 + " s");
				}
				return;
			}
			JsonObject extra = step.screenshot() ? capture(client, step.name()) : null;
			result.step(step.name(), true, null, elapsedMillis(), extra);
			LOGGER.info("{}step {} ok", PREFIX, step.name());
		} catch (Throwable t) {
			fail(step, t);
		}
		startStep(client, stepIndex + 1);
	}

	private void startStep(Minecraft client, int index) {
		stepIndex = index;
		stepTicks = 0;
		stepStartedNanos = System.nanoTime();
		if (index >= steps.size()) {
			return;
		}
		Step step = steps.get(index);
		try {
			step.enter().run(client);
		} catch (Throwable t) {
			fail(step, t);
			startStep(client, index + 1);
		}
	}

	private void fail(Step step, Throwable t) {
		LOGGER.error("{}step {} failed", PREFIX, step.name(), t);
		result.step(step.name(), false, t.toString(), elapsedMillis(), null);
	}

	private long elapsedMillis() {
		return (System.nanoTime() - stepStartedNanos) / 1_000_000L;
	}

	// ------------------------------------------------------------------ world

	private void createWorld(Minecraft client) {
		LOGGER.info("{}Creating the world '{}' (superflat, creative, peaceful).", PREFIX, worldName);
		GameRules rules = new GameRules();
		rules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
		rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, null);
		rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
		LevelSettings settings = new LevelSettings(worldName, GameType.CREATIVE, false, Difficulty.PEACEFUL, true, rules,
			WorldDataConfiguration.DEFAULT);
		WorldOptions options = new WorldOptions(0L, false, false);
		client.createWorldOpenFlows().createFreshLevel(worldName, settings, options, registries -> {
			FlatLevelGeneratorPreset preset = registries.registryOrThrow(Registries.FLAT_LEVEL_GENERATOR_PRESET)
				.getHolderOrThrow(FlatLevelGeneratorPresets.CLASSIC_FLAT).value();
			FlatLevelGeneratorSettings flat = preset.settings();
			// No structures: an empty override set instead of "all structure sets"
			flat = flat.withBiomeAndLayers(flat.getLayersInfo(), Optional.of(HolderSet.direct()), flat.getBiome());
			WorldDimensions dimensions = registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value()
				.createWorldDimensions();
			return dimensions.replaceOverworldGenerator(registries, new FlatLevelSource(flat));
		}, new TitleScreen());
	}

	// ------------------------------------------------------------------ screenshots

	/** Saves the main render target as {@code screenshots/<name>.png} and returns what the result file records about it. */
	private JsonObject capture(Minecraft client, String name) throws IOException {
		Files.createDirectories(shotDir);
		Path file = shotDir.resolve(name + ".png");
		JsonObject info = new JsonObject();
		try (NativeImage image = Screenshot.takeScreenshot(client.getMainRenderTarget())) {
			image.writeToFile(file);
			info.addProperty("screenshot", "screenshots/" + name + ".png");
			info.addProperty("width", image.getWidth());
			info.addProperty("height", image.getHeight());
			analyse(image, info);
		}
		return info;
	}

	/** Cheap statistics so a blank or outline-less picture is caught without looking at it. */
	private static void analyse(NativeImage image, JsonObject info) {
		Set<Integer> colors = new HashSet<>();
		long outlinePixels = 0;
		double luma = 0;
		long samples = 0;
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				int abgr = image.getPixelRGBA(x, y);
				int r = abgr & 0xFF;
				int g = (abgr >> 8) & 0xFF;
				int b = (abgr >> 16) & 0xFF;
				if (colors.size() < 100_000) {
					colors.add(abgr & 0xFFFFFF);
				}
				if (Math.abs(r - OUTLINE_R) <= 60 && Math.abs(g - OUTLINE_G) <= 35 && Math.abs(b - OUTLINE_B) <= 60) {
					outlinePixels++;
				}
				luma += 0.2126 * r + 0.7152 * g + 0.0722 * b;
				samples++;
			}
		}
		info.addProperty("distinctColors", colors.size());
		info.addProperty("outlinePixels", outlinePixels);
		info.addProperty("meanLuma", Math.round(luma / Math.max(1, samples) * 10.0) / 10.0);
	}

	// ------------------------------------------------------------------ end of the run

	private void finish(Minecraft client, String fatal) {
		if (phase == Phase.DONE) {
			return;
		}
		phase = Phase.DONE;
		if (fatal != null) {
			LOGGER.error("{}FAILED: {}", PREFIX, fatal);
		}
		result.finish(fatal);
		LOGGER.info("{}result written to {}, stopping the client.", PREFIX, runDir.resolve("smoke-result.json"));
		try {
			client.setScreen(null);
		} catch (Throwable ignored) {
			// the run is over; stopping the client below is what matters
		}
		client.stop();
	}

	private void startWatchdog() {
		long seconds = Long.getLong("glintsmith.smoke.timeout", DEFAULT_TIMEOUT_SECONDS);
		Thread watchdog = new Thread(() -> {
			try {
				Thread.sleep(seconds * 1000L);
			} catch (InterruptedException e) {
				return;
			}
			if (!result.isWritten()) {
				LOGGER.error("{}FAILED: the run did not finish within {} s (-Dglintsmith.smoke.timeout); halting.", PREFIX, seconds);
				result.finish("watchdog: the run did not finish within " + seconds + " s (phase " + phase + ", step "
					+ (stepIndex >= 0 && stepIndex < steps.size() ? steps.get(stepIndex).name() : "-") + ")");
			}
			Runtime.getRuntime().halt(3);
		}, "Glintsmith Smoke watchdog");
		watchdog.setDaemon(true);
		watchdog.start();
	}
}
