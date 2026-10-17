package navrelis.glintsmith.render.outline;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Session-wide kill switch of the outline renderer. The first error is logged once with its stack trace and every
 * entry point falls back to vanilla behaviour from then on.
 */
public final class OutlineFailsafe {
	private static final Logger LOGGER = LoggerFactory.getLogger("Glintsmith/Outline");

	private static volatile boolean tripped;
	private static volatile Runnable onTrip;
	private static int trips;

	private OutlineFailsafe() {
	}

	/**
	 * @return whether outlines are off for the rest of the session
	 */
	public static boolean tripped() {
		return tripped;
	}

	/**
	 * Turns outlines off for the session. Only the first call logs (one ERROR line with the stack trace) and runs
	 * the cleanup hook; later calls do nothing. Never throws.
	 *
	 * @param where short description of the failing entry point
	 * @param t the cause
	 */
	public static void trip(String where, Throwable t) {
		synchronized (OutlineFailsafe.class) {
			if (tripped) {
				return;
			}
			tripped = true;
			trips++;
		}
		try {
			LOGGER.error("[Glintsmith] Outline rendering disabled for this session after an error in {}", where, t);
		} catch (Throwable ignored) {
			// logging must never take the game down
		}
		Runnable hook = onTrip;
		if (hook != null) {
			try {
				hook.run();
			} catch (Throwable ignored) {
				// the cleanup is best effort
			}
		}
	}

	/** Cleanup that runs once on the first trip (abandons the mask buffers). */
	static void setOnTrip(Runnable hook) {
		onTrip = hook;
	}

	/** Number of trips that logged; at most 1 per session. For tests. */
	static int tripCount() {
		return trips;
	}

	/** Re-arms the failsafe. Tests only. */
	static void resetForTests() {
		synchronized (OutlineFailsafe.class) {
			tripped = false;
			trips = 0;
		}
	}
}
