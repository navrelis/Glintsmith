package navrelis.glintsmith.render.outline.compat;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Optional Iris integration of the outline renderer. Iris is a compile-only dependency: its API is only touched from
 * {@link Api}, which is loaded only when the {@code iris} mod is present, so nothing breaks without it.
 * <p>
 * Any error from the Iris API counts as "a shader pack is in use" (the safe side: world outlines off) and is logged
 * once. No Sodium API is needed.
 */
public final class IrisCompat {
	private static final Logger LOGGER = LoggerFactory.getLogger("Glintsmith/Outline");
	private static final boolean LOADED = detect();

	private static boolean packInUse;
	private static boolean warned;

	private IrisCompat() {
	}

	private static boolean detect() {
		try {
			return FabricLoader.getInstance().isModLoaded("iris");
		} catch (Throwable t) {
			return false;
		}
	}

	/**
	 * @return whether Iris is installed
	 */
	public static boolean loaded() {
		return LOADED;
	}

	/**
	 * @return whether a shader pack was in use at the start of this frame (see {@link #refreshPerFrame()})
	 */
	public static boolean shaderPackInUse() {
		return packInUse;
	}

	/**
	 * Asks Iris right now whether a shader pack is in use. False without Iris; true on any Iris error.
	 */
	public static boolean queryShaderPack() {
		if (!LOADED) {
			return false;
		}
		try {
			return Api.shaderPackInUse();
		} catch (Throwable t) {
			warnOnce(t);
			return true;
		}
	}

	/**
	 * @return whether Iris is rendering its shadow pass right now; false without Iris or on errors
	 */
	public static boolean shadowPass() {
		if (!LOADED) {
			return false;
		}
		try {
			return Api.shadowPass();
		} catch (Throwable t) {
			warnOnce(t);
			return false;
		}
	}

	/**
	 * Caches {@link #queryShaderPack()} for the frame. Call once at frame begin.
	 */
	public static void refreshPerFrame() {
		packInUse = queryShaderPack();
	}

	private static void warnOnce(Throwable t) {
		if (!warned) {
			warned = true;
			LOGGER.warn("[Glintsmith] The Iris API failed; treating shaders as active for outlines: {}", String.valueOf(t));
		}
	}

	/** Only loaded when Iris is present. */
	private static final class Api {
		private static final net.irisshaders.iris.api.v0.IrisApi INSTANCE = net.irisshaders.iris.api.v0.IrisApi.getInstance();

		static boolean shaderPackInUse() {
			return INSTANCE.isShaderPackInUse();
		}

		static boolean shadowPass() {
			return INSTANCE.isRenderingShadowPass();
		}
	}
}
