package navrelis.glintsmith.gui;

import net.fabricmc.loader.api.FabricLoader;

/**
 * Asks Iris whether a shader pack is in use. Iris is an optional, compile-only dependency: its classes are only
 * touched from {@link Holder}, which is never loaded when Iris is missing.
 */
public final class IrisInfo {
	private IrisInfo() {
	}

	/**
	 * @return {@code true} if Iris is loaded and a shader pack is active; {@code false} otherwise or on any error
	 */
	public static boolean shaderPackActive() {
		try {
			return FabricLoader.getInstance().isModLoaded("iris") && Holder.active();
		} catch (Throwable t) {
			return false;
		}
	}

	private static final class Holder {
		static boolean active() {
			return net.irisshaders.iris.api.v0.IrisApi.getInstance().isShaderPackInUse();
		}
	}
}
