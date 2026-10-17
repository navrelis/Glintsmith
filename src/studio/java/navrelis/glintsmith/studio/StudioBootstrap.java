package navrelis.glintsmith.studio;

import java.nio.file.Path;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorPreset;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorPresets;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

/**
 * Gets from the title screen into a loaded void world without any input, then opens the {@link StudioScreen}. Runs
 * on the client tick. The world ({@code run-studio/saves/glintsmith_studio}) is (re)created by every run through the
 * "create world" path: void superflat, creative, peaceful, no daylight or weather cycle, no mob spawning. Creating it
 * again over the same folder only rewrites its level data; opening an existing world instead can stop at a
 * confirmation screen (for example the backup question when the set of mod data packs changed).
 * <p>
 * A loaded world is needed because enchantments are data driven in 1.21.1: enchanted stacks need the registries of a
 * loaded world.
 */
final class StudioBootstrap {
	static final String WORLD = "glintsmith_studio";
	private static final int MENU_TIMEOUT_TICKS = 20 * 120;
	private static final int LOAD_TIMEOUT_TICKS = 20 * 180;
	private static final int SETTLE_TICKS = 20;

	private enum State {
		MENU,
		LOADING,
		SETTLING,
		RUNNING
	}

	private final Scene scene;
	private final Path out;
	private State state = State.MENU;
	private int ticks;

	StudioBootstrap(Scene scene, Path out) {
		this.scene = scene;
		this.out = out;
	}

	void tick(Minecraft client) {
		if (state == State.RUNNING || StudioMod.failed()) {
			return;
		}
		ticks++;
		switch (state) {
			case MENU -> {
				if (client.getOverlay() == null && client.level == null
					&& (client.screen instanceof TitleScreen || client.screen instanceof AccessibilityOnboardingScreen)) {
					client.options.onboardAccessibility = false;
					startWorld(client);
					state = State.LOADING;
					ticks = 0;
				} else if (ticks > MENU_TIMEOUT_TICKS) {
					throw new IllegalStateException("the title screen did not appear within " + MENU_TIMEOUT_TICKS / 20 + " s (screen: "
						+ (client.screen == null ? "none" : client.screen.getClass().getSimpleName()) + ")");
				}
			}
			case LOADING -> {
				if (client.level != null && client.player != null && client.screen == null) {
					state = State.SETTLING;
					ticks = 0;
					client.options.hideGui = true;
					client.player.getAbilities().flying = true;
					StudioMod.info("World '{}' loaded.", WORLD);
				} else if (client.level == null && client.screen instanceof TitleScreen && ticks > 40) {
					throw new IllegalStateException("the studio world could not be opened (back on the title screen, see the log above)");
				} else if (ticks > LOAD_TIMEOUT_TICKS) {
					throw new IllegalStateException("the studio world did not load within " + LOAD_TIMEOUT_TICKS / 20 + " s");
				}
			}
			case SETTLING -> {
				if (ticks >= SETTLE_TICKS) {
					state = State.RUNNING;
					client.setScreen(new StudioScreen(scene, out));
				}
			}
			default -> {
			}
		}
	}

	private static void startWorld(Minecraft client) {
		StudioMod.info("Creating the studio world '{}' (void superflat).", WORLD);
		GameRules rules = new GameRules();
		rules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
		rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, null);
		rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
		LevelSettings settings = new LevelSettings(WORLD, GameType.CREATIVE, false, Difficulty.PEACEFUL, true, rules,
			WorldDataConfiguration.DEFAULT);
		WorldOptions options = new WorldOptions(0L, false, false);
		client.createWorldOpenFlows().createFreshLevel(WORLD, settings, options, registries -> {
			Holder<FlatLevelGeneratorPreset> preset = registries.registryOrThrow(Registries.FLAT_LEVEL_GENERATOR_PRESET)
				.getHolderOrThrow(FlatLevelGeneratorPresets.THE_VOID);
			WorldDimensions flat = registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value()
				.createWorldDimensions();
			return flat.replaceOverworldGenerator(registries, new FlatLevelSource(preset.value().settings()));
		}, new TitleScreen());
	}
}
