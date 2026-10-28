package navrelis.glintsmith.smoke;

import java.lang.reflect.Method;

import com.google.gson.JsonObject;

/**
 * Asks Iris, reflectively (no build dependency), whether a shader pack is in use. Only used when the run was started with
 * {@code -Dglintsmith.smoke.shaders=true} (scripts/smoke.ps1 -Shaders). Every failure is remembered in {@link #error()}
 * instead of being thrown, and counts as "not in use": unlike {@code IrisCompat.queryShaderPack()} this never assumes a pack.
 */
final class ShaderProbe {
	private Object api;
	private Method inUseMethod;
	private Method packNameMethod;
	private boolean irisLoaded;
	private String error;

	ShaderProbe() {
		try {
			Class<?> apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
			api = apiClass.getMethod("getInstance").invoke(null);
			inUseMethod = apiClass.getMethod("isShaderPackInUse");
			irisLoaded = true;
		} catch (ClassNotFoundException e) {
			error = "Iris API not found: " + e;
		} catch (Throwable t) {
			error = "Iris API unavailable: " + t;
		}
		try {
			packNameMethod = Class.forName("net.irisshaders.iris.Iris").getMethod("getCurrentPackName");
		} catch (Throwable t) {
			error = (error == null ? "" : error + "; ") + "Iris.getCurrentPackName unavailable: " + t;
		}
	}

	boolean irisLoaded() {
		return irisLoaded;
	}

	/** {@code IrisApi.isShaderPackInUse()}; false when Iris is missing or the call fails. */
	boolean inUse() {
		if (inUseMethod == null) {
			return false;
		}
		try {
			return (Boolean) inUseMethod.invoke(api);
		} catch (Throwable t) {
			error = "isShaderPackInUse failed: " + t;
			return false;
		}
	}

	/** {@code Iris.getCurrentPackName()}, or null. */
	String packName() {
		if (packNameMethod == null) {
			return null;
		}
		try {
			Object name = packNameMethod.invoke(null);
			return name == null ? null : name.toString();
		} catch (Throwable t) {
			error = "getCurrentPackName failed: " + t;
			return null;
		}
	}

	String error() {
		return error;
	}

	/** Snapshot for smoke-result.json: {@code irisLoaded}, {@code packName}, {@code inUse} and the reflection error, if any. */
	JsonObject snapshot() {
		JsonObject info = new JsonObject();
		info.addProperty("irisLoaded", irisLoaded);
		info.addProperty("packName", packName());
		info.addProperty("inUse", inUse());
		if (error != null) {
			info.addProperty("error", error);
		}
		return info;
	}
}
