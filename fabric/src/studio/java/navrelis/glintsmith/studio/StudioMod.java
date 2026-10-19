package navrelis.glintsmith.studio;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourceReloadListenerKeys;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.sounds.SoundSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point of the dev-only studio (docs/STUDIO.md). Does nothing unless {@code -Dglintsmith.studio.scene} is set,
 * which {@code ./gradlew runStudio -Pscene=...} does. With a scene it loads it, bootstraps a void world without any
 * input, renders the scene in {@link StudioScreen} and quits the game.
 */
public final class StudioMod implements ClientModInitializer {
	static final Logger LOGGER = LoggerFactory.getLogger("Glintsmith/Studio");
	private static final String PREFIX = "[Glintsmith Studio] ";
	/** Hard limit for a whole run; the process is halted when it is exceeded (a hung run must not stay open). */
	private static final long DEFAULT_TIMEOUT_SECONDS = 1800L;

	private static volatile int exitCode;
	private static volatile boolean failed;
	private static volatile int reloads;

	@Override
	public void onInitializeClient() {
		String spec = System.getProperty("glintsmith.studio.scene");
		if (spec == null || spec.isBlank()) {
			LOGGER.info(PREFIX + "No scene given (-Pscene=...), the studio stays idle.");
			return;
		}
		Scene scene;
		try {
			PipelineBridge.init();
			scene = Scene.load(spec);
		} catch (Throwable t) {
			fail("could not load scene '" + spec + "': " + t.getMessage(), t);
			// The client is not running yet; nothing to shut down cleanly.
			System.exit(exitCode);
			return;
		}
		Path out = outputDir(scene);
		LOGGER.info(PREFIX + "Scene '{}' ({}, {}x{} x{} supersample, {} fps) -> {}", scene.id, scene.type, scene.width,
			scene.height, scene.supersample, scene.fps, out);
		startWatchdog();

		ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new ReloadCounter());
		StudioBootstrap bootstrap = new StudioBootstrap(scene, out);
		ClientLifecycleEvents.CLIENT_STARTED.register(StudioMod::configureOptions);
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			try {
				bootstrap.tick(client);
			} catch (Throwable t) {
				fail("bootstrap failed: " + t, t);
				client.stop();
			}
		});
	}

	/** @return the exit code the game ends with (0 unless the run failed) */
	public static int exitCode() {
		return exitCode;
	}

	/** @return how many client resource reloads have completed so far */
	static int reloads() {
		return reloads;
	}

	static boolean failed() {
		return failed;
	}

	/** Logs one clear error line and makes the process exit non-zero. */
	static void fail(String message, Throwable t) {
		failed = true;
		exitCode = 1;
		LOGGER.error(PREFIX + "FAILED: " + message, t);
	}

	static void info(String message, Object... args) {
		LOGGER.info(PREFIX + message, args);
	}

	private static Path outputDir(Scene scene) {
		String override = System.getProperty("glintsmith.studio.out");
		Path base;
		if (override != null && !override.isBlank()) {
			base = Path.of(override);
		} else {
			base = repoDir().resolve("art").resolve("captures").resolve("studio");
		}
		if (scene.output != null) {
			Path custom = Path.of(scene.output);
			base = custom.isAbsolute() ? custom : repoDir().resolve(custom);
			return base;
		}
		return base.resolve(scene.id);
	}

	static Path repoDir() {
		String repo = System.getProperty("glintsmith.studio.repo");
		if (repo != null && !repo.isBlank()) {
			return Path.of(repo);
		}
		// runStudio runs in <repo>/run-studio
		Path run = FabricLoader.getInstance().getGameDir().toAbsolutePath().normalize();
		return run.getParent() == null ? run : run.getParent();
	}

	/**
	 * In-memory options for an unattended render. options.txt lives in run-studio, never in the player's game.
	 */
	private static void configureOptions(Minecraft client) {
		Options options = client.options;
		options.pauseOnLostFocus = false;
		options.onboardAccessibility = false;
		options.hideGui = true;
		options.tutorialStep = TutorialSteps.NONE;
		options.graphicsMode().set(GraphicsStatus.FANCY); // Fabulous would draw items into a separate target
		options.renderDistance().set(2);
		options.simulationDistance().set(5);
		options.framerateLimit().set(260);
		options.enableVsync().set(false);
		options.getSoundSourceOptionInstance(SoundSource.MASTER).set(0.0);
		options.realmsNotifications().set(false);
		client.getWindow().updateVsync(false);
	}

	private static void startWatchdog() {
		long seconds = Long.getLong("glintsmith.studio.timeout", DEFAULT_TIMEOUT_SECONDS);
		Thread watchdog = new Thread(() -> {
			try {
				Thread.sleep(seconds * 1000L);
			} catch (InterruptedException e) {
				return;
			}
			LOGGER.error(PREFIX + "FAILED: the run did not finish within {} s (-Dglintsmith.studio.timeout); halting.", seconds);
			Runtime.getRuntime().halt(3);
		}, "Glintsmith Studio watchdog");
		watchdog.setDaemon(true);
		watchdog.start();
	}

	/** Counts completed resource reloads (a gap fix change of a look triggers one). */
	private static final class ReloadCounter implements SimpleSynchronousResourceReloadListener {
		private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("glintsmith_studio", "reload_counter");

		@Override
		public ResourceLocation getFabricId() {
			return ID;
		}

		@Override
		public Collection<ResourceLocation> getFabricDependencies() {
			// Counted after the models are baked.
			return List.of(ResourceReloadListenerKeys.MODELS);
		}

		@Override
		public void onResourceManagerReload(ResourceManager manager) {
			reloads++;
		}
	}
}
