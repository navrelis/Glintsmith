package navrelis.glintsmith.render.outline;

import navrelis.glintsmith.style.GlintCategory;
import navrelis.glintsmith.style.OutlineTarget;
import net.minecraft.world.item.ItemDisplayContext;

/**
 * Maps vanilla display contexts to outline targets, per phase. Pure functions; duplicated on purpose in the glint
 * system (the two systems share no classes).
 */
public final class OutlineContexts {
	private OutlineContexts() {
	}

	/**
	 * The target of a display context, independent of the phase. Unknown contexts (for example added by other
	 * loaders) are {@link OutlineTarget#OTHER}.
	 */
	public static OutlineTarget targetOf(ItemDisplayContext context) {
		if (context == null) {
			return OutlineTarget.OTHER;
		}
		return switch (context) {
			case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> OutlineTarget.FIRST_PERSON;
			case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> OutlineTarget.THIRD_PERSON;
			case GROUND -> OutlineTarget.DROPPED;
			case FIXED -> OutlineTarget.ITEM_FRAME;
			case GUI -> OutlineTarget.GUI;
			default -> OutlineTarget.OTHER;
		};
	}

	/**
	 * The target of an item rendered while this phase is open, or null if the phase never outlines it.
	 * In the world every context that does not belong there (first person, GUI) counts as OTHER.
	 */
	public static OutlineTarget forPhase(OutlinePhase phase, ItemDisplayContext context) {
		if (phase == null) {
			return null;
		}
		OutlineTarget target = targetOf(context);
		return switch (phase) {
			case WORLD -> target == OutlineTarget.FIRST_PERSON || target == OutlineTarget.GUI ? OutlineTarget.OTHER : target;
			case HAND -> target == OutlineTarget.FIRST_PERSON ? target : null;
			case GUI -> target == OutlineTarget.GUI ? target : null;
			case NONE -> null;
		};
	}

	/**
	 * @return whether this phase captures this target at all
	 */
	public static boolean phaseAccepts(OutlinePhase phase, OutlineTarget target) {
		if (phase == null || target == null) {
			return false;
		}
		return switch (phase) {
			case WORLD -> target == OutlineTarget.THIRD_PERSON || target == OutlineTarget.ARMOR
				|| target == OutlineTarget.DROPPED || target == OutlineTarget.ITEM_FRAME || target == OutlineTarget.OTHER;
			case HAND -> target == OutlineTarget.FIRST_PERSON;
			case GUI -> target == OutlineTarget.GUI;
			case NONE -> false;
		};
	}

	/**
	 * The glint category passed to the rule engine; only the outline of the result is used.
	 */
	public static GlintCategory categoryOf(OutlineTarget target) {
		if (target == OutlineTarget.GUI) {
			return GlintCategory.GUI;
		}
		if (target == OutlineTarget.ARMOR) {
			return GlintCategory.ARMOR;
		}
		return GlintCategory.ITEM;
	}
}
