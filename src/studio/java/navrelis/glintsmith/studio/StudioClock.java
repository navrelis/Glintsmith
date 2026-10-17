package navrelis.glintsmith.studio;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;

/**
 * The deterministic clock of a studio run. While it is active, every animation time source of the glint and outline
 * pipeline (Glintsmith's frame times and glint scroll, the vanilla glint scroll) reads {@link #millis()} instead of
 * {@code Util.getMillis()} on the render thread. Everything else in the game (ticks, the loading overlay, the
 * integrated server) keeps the real clock, so the game behaves normally while the studio waits for it.
 */
public final class StudioClock {
	private static volatile boolean active;
	private static volatile long millis;

	private StudioClock() {
	}

	/**
	 * Sets the studio time; the pipeline reads it from the next frame start on.
	 */
	public static void set(double seconds) {
		millis = Math.round(seconds * 1000.0);
		active = true;
	}

	/** Back to the real clock. */
	public static void release() {
		active = false;
	}

	/**
	 * @return the studio time in milliseconds while active (render thread), otherwise {@code Util.getMillis()}
	 */
	public static long millis() {
		return active && RenderSystem.isOnRenderThread() ? millis : Util.getMillis();
	}
}
