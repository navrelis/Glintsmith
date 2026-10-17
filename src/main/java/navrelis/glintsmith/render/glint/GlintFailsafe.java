package navrelis.glintsmith.render.glint;

import navrelis.glintsmith.Glintsmith;

/**
 * Once anything in the glint system throws, glint customisation is off for the rest of the session and vanilla glint
 * is drawn. One ERROR line with the stack trace is logged; later failures are silent.
 */
public final class GlintFailsafe {
	private static volatile boolean tripped;

	private GlintFailsafe() {
	}

	/**
	 * @return true if glint customisation has been switched off for this session
	 */
	public static boolean tripped() {
		return tripped;
	}

	/**
	 * Switches glint customisation off for this session. Never throws.
	 *
	 * @param where short name of the failing entry point
	 */
	public static void trip(String where, Throwable t) {
		try {
			if (!tripped) {
				tripped = true;
				Glintsmith.LOGGER.error("Glint customisation failed in {}; using the vanilla glint for the rest of this session.", where, t);
			}
		} catch (Throwable ignored) {
			tripped = true;
		}
	}

	/** Test hook. */
	static void resetForTests() {
		tripped = false;
	}
}
