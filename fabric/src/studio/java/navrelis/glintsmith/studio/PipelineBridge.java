package navrelis.glintsmith.studio;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import navrelis.glintsmith.config.ConfigManager;
import navrelis.glintsmith.config.GlintsmithConfig;
import navrelis.glintsmith.render.outline.OutlineSystem;

/**
 * The few private parts of Glintsmith the studio needs, reached by reflection so that the released code stays
 * untouched (this class only exists in the dev-only studio source set):
 * <ul>
 * <li>{@link #applyConfig}: makes a config current in memory and notifies the config listeners exactly as a load
 * would (outline and glint runtime settings, rule cache, glint textures, gap fix), without writing the file.</li>
 * <li>{@link #openWorldPhase} / {@link #compositeWorldPhase}: open and composite the outline system's WORLD phase
 * around studio geometry drawn outside the level render (armor, held items and 3D display contexts), the same phase
 * the level render opens at {@code WorldRenderEvents.START} and composites at the water-mask flush.</li>
 * </ul>
 * Render thread only.
 */
final class PipelineBridge {
	private static Field configCurrent;
	private static Method notifyListeners;
	private static Field worldPhase;
	private static Field phaseOpen;
	private static Method phaseDiscard;
	private static Method openPhase;
	private static Method compositeWorld;

	private PipelineBridge() {
	}

	static void init() throws ReflectiveOperationException {
		configCurrent = ConfigManager.class.getDeclaredField("current");
		configCurrent.setAccessible(true);
		notifyListeners = ConfigManager.class.getDeclaredMethod("notifyListeners", GlintsmithConfig.class);
		notifyListeners.setAccessible(true);

		Class<?> phaseState = Class.forName("navrelis.glintsmith.render.outline.PhaseState");
		worldPhase = OutlineSystem.class.getDeclaredField("world");
		worldPhase.setAccessible(true);
		phaseOpen = phaseState.getDeclaredField("open");
		phaseOpen.setAccessible(true);
		phaseDiscard = phaseState.getDeclaredMethod("discard");
		phaseDiscard.setAccessible(true);
		openPhase = OutlineSystem.class.getDeclaredMethod("open", phaseState);
		openPhase.setAccessible(true);
		compositeWorld = OutlineSystem.class.getDeclaredMethod("compositeWorld", phaseState);
		compositeWorld.setAccessible(true);
	}

	/**
	 * Makes {@code config} the current config of {@link ConfigManager#instance()} and runs the listeners. The file on
	 * disk is not written.
	 */
	static void applyConfig(GlintsmithConfig config) throws ReflectiveOperationException {
		ConfigManager manager = ConfigManager.instance();
		configCurrent.set(manager, config);
		notifyListeners.invoke(manager, config);
	}

	/**
	 * Opens the WORLD outline phase, as the level render does at its start.
	 *
	 * @return false if world outlines are off for the current look (nothing is captured then)
	 */
	static boolean openWorldPhase() throws ReflectiveOperationException {
		Object phase = worldPhase.get(null);
		if (phase == null || !OutlineSystem.settings().worldPhaseAllowed(false)) {
			return false;
		}
		if (phaseOpen.getBoolean(phase)) {
			// Left open by the level render (no composite point reached); it holds nothing of ours.
			compositeWorld.invoke(null, phase);
		}
		phaseDiscard.invoke(phase);
		openPhase.invoke(null, phase);
		return true;
	}

	/**
	 * Closes the WORLD phase and composites its outlines onto the main target. Call after the geometry was flushed.
	 */
	static void compositeWorldPhase() throws ReflectiveOperationException {
		Object phase = worldPhase.get(null);
		if (phase != null && phaseOpen.getBoolean(phase)) {
			compositeWorld.invoke(null, phase);
		}
	}
}
