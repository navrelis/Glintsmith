package navrelis.glintsmith.modelfix;

import net.minecraft.Util;
import net.minecraft.core.Direction;
import org.joml.Vector3f;

/**
 * Fixes the hairline gaps (MC-73186) between the faces of flat generated item models.
 *
 * <p>Holds the active {@link Settings} and the pure math the mixins use, so it can be tested without a running game.
 */
public final class GapFix {
	/**
	 * Tunables of the fix.
	 *
	 * @param enabled            master switch; when false vanilla behaviour is untouched
	 * @param uvShrinkMultiplier factor applied to vanilla's UV shrink on the blocks atlas (1 = vanilla, 0 = none)
	 * @param sideExpansion      how far side quads grow along both in-plane axes, in model units (1/16 block)
	 * @param sideIndent         how far side quads move towards the item centre, in model units
	 * @param preciseSpans       only merge contiguous edge pixels into one side quad (costs more quads on HD textures)
	 */
	public record Settings(boolean enabled, float uvShrinkMultiplier, float sideExpansion, float sideIndent, boolean preciseSpans) {
	}

	private static volatile Settings current = autoSettings();

	/** Set when a mixin handler threw; the fix then stays off for the rest of the session. */
	private static volatile boolean failed;

	private GapFix() {
	}

	/**
	 * Platform dependent defaults: macOS drivers bleed without UV shrink, so it keeps vanilla shrink and a larger indent.
	 * Elsewhere the side quads grow by 0.005 so they close the 1 px step-corner cracks of generated item models; 0.01 and
	 * above showed jagged slivers along the step edges in game.
	 */
	public static Settings autoSettings() {
		if (Util.getPlatform() == Util.OS.OSX) {
			return new Settings(true, 1.0F, 0.0F, 0.0099F, false);
		}
		return new Settings(true, 0.0F, 0.005F, 0.0001F, false);
	}

	/** Settings that reproduce vanilla behaviour exactly. */
	public static Settings vanilla() {
		return new Settings(false, 1.0F, 0.0F, 0.0F, false);
	}

	/** Currently active settings. */
	public static Settings current() {
		return current;
	}

	/** Stores new settings. Changes that affect baking only show after a resource reload, see {@link #requiresReload}. */
	public static void apply(Settings settings) {
		current = settings;
	}

	/** Whether switching from {@code a} to {@code b} changes baked models and so needs a resource reload. */
	public static boolean requiresReload(Settings a, Settings b) {
		return !a.equals(b);
	}

	/** Whether a mixin handler has failed and the fix has been switched off. */
	public static boolean hasFailed() {
		return failed;
	}

	/** Marks the fix as failed for this session. */
	public static void markFailed() {
		failed = true;
	}

	/** Settings the mixins should act on: {@link #vanilla()} once the fix has failed. */
	public static Settings effective() {
		return failed ? vanilla() : current;
	}

	/**
	 * Adjusts a single-face side element of a generated item.
	 *
	 * <p>The element moves {@code indent} towards the opaque pixel it borders, perpendicular to its face, and grows by
	 * {@code expansion} on both in-plane axes. UP and DOWN elements sit on the top and bottom edge of a pixel row, so
	 * inward is -y for UP and +y for DOWN. Vanilla keys a left edge (smaller x) as EAST and a right edge (larger x) as
	 * WEST, and builds both with {@code from.y > to.y} (the {@code 16 - y} flip in {@code createSideElements}).
	 * {@code FaceBakery} does not sort corners, so that flip turns the baked quads outwards: an EAST element bakes as a
	 * west-facing quad and its V runs top to bottom. Inward is therefore +x for EAST and -x for WEST, and the order of
	 * {@code from} and {@code to} must stay as it is on every axis. NORTH and SOUTH are the front and back faces and stay
	 * as they are.
	 */
	public static void adjustSideElement(Direction face, Vector3f from, Vector3f to, float expansion, float indent) {
		switch (face) {
			case UP -> {
				from.y -= indent;
				to.y -= indent;
				grow(from, to, 0, expansion);
				grow(from, to, 2, expansion);
			}
			case DOWN -> {
				from.y += indent;
				to.y += indent;
				grow(from, to, 0, expansion);
				grow(from, to, 2, expansion);
			}
			case EAST -> {
				from.x += indent;
				to.x += indent;
				grow(from, to, 1, expansion);
				grow(from, to, 2, expansion);
			}
			case WEST -> {
				from.x -= indent;
				to.x -= indent;
				grow(from, to, 1, expansion);
				grow(from, to, 2, expansion);
			}
			default -> {
			}
		}
	}

	/** The UV shrink ratio to use for a sprite: vanilla's, scaled by the multiplier on the blocks atlas only. */
	public static float shrinkRatio(float vanillaRatio, boolean isBlockAtlas, Settings settings) {
		if (!settings.enabled() || !isBlockAtlas) {
			return vanillaRatio;
		}
		return vanillaRatio * settings.uvShrinkMultiplier();
	}

	/**
	 * Moves the two ends of {@code axis} apart by {@code e} each and keeps their order, whichever end is larger. A
	 * negative {@code e} stops where the ends meet.
	 */
	private static void grow(Vector3f from, Vector3f to, int axis, float e) {
		float a = from.get(axis);
		float b = to.get(axis);
		float min = Math.min(a, b);
		float max = Math.max(a, b);
		float mid = (min + max) * 0.5F;
		float low = Math.min(min - e, mid);
		float high = Math.max(max + e, mid);
		from.setComponent(axis, a <= b ? low : high);
		to.setComponent(axis, a <= b ? high : low);
	}
}
