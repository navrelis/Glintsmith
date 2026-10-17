package navrelis.glintsmith.render.glint;

import navrelis.glintsmith.style.GlintCategory;
import navrelis.glintsmith.style.GlintLook;
import navrelis.glintsmith.style.OutlineTarget;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * What is being drawn right now, as a fixed-capacity stack of frames. The item, armor and BEWLR hooks push a frame
 * around the vanilla call, so that {@link GlintSystem} knows the stack and display context when a glint render type is
 * requested somewhere below.
 * <p>
 * Render thread only. The frames are preallocated: pushing and popping allocate nothing. When more than
 * {@value #CAPACITY} frames are pushed the surplus pushes are ignored and counted, so {@link #pop()} stays balanced.
 */
public final class GlintContext {
	static final int CAPACITY = 16;

	static final byte ITEM_FRAME = 0;
	static final byte ARMOR_FRAME = 1;
	static final byte BEWLR_FRAME = 2;

	private static final Frame[] FRAMES = new Frame[CAPACITY];
	private static int depth;
	private static int overflow;
	/** Nesting depth of {@code ItemInHandRenderer.renderHandsWithItems}; above zero while the first-person hand pass runs. */
	private static int handsDepth;

	static {
		for (int i = 0; i < CAPACITY; i++) {
			FRAMES[i] = new Frame();
		}
	}

	private GlintContext() {
	}

	/**
	 * An item is rendered through {@code ItemRenderer.render}.
	 */
	public static void pushItem(@Nullable ItemStack stack, @Nullable ItemDisplayContext context) {
		push(ITEM_FRAME, stack, context);
	}

	/**
	 * A worn armor piece (or elytra) is rendered.
	 */
	public static void pushArmor(@Nullable ItemStack stack) {
		push(ARMOR_FRAME, stack, null);
	}

	/**
	 * A block entity item renderer (shield, trident, banner, mod renderers) is running.
	 */
	public static void pushBewlr(@Nullable ItemStack stack, @Nullable ItemDisplayContext context) {
		push(BEWLR_FRAME, stack, context);
	}

	/**
	 * The first-person hand pass ({@code ItemInHandRenderer.renderHandsWithItems}) starts. Independent of the outline
	 * system, so the glint classification does not depend on outlines being on.
	 */
	public static void beginHands() {
		handsDepth++;
	}

	/**
	 * The hand pass ends. An extra call is ignored.
	 */
	public static void endHands() {
		if (handsDepth > 0) {
			handsDepth--;
		}
	}

	/**
	 * Safety net for the start of a frame: no hand pass can be open there, so a leaked flag is cleared.
	 */
	public static void resetHands() {
		handsDepth = 0;
	}

	/**
	 * @return whether the first-person hand pass is running right now
	 */
	public static boolean inHands() {
		return handsDepth > 0;
	}

	/**
	 * Pops the frame of the matching push. Extra pops are ignored.
	 */
	public static void pop() {
		if (overflow > 0) {
			overflow--;
			return;
		}
		if (depth > 0) {
			Frame frame = FRAMES[--depth];
			frame.stack = null;
			frame.context = null;
		}
	}

	/**
	 * @return the innermost frame, or null outside of any hook
	 */
	@Nullable
	static Frame top() {
		return depth > 0 ? FRAMES[depth - 1] : null;
	}

	static int depth() {
		return depth;
	}

	/** Test hook: drops all frames. */
	static void reset() {
		while (depth > 0) {
			pop();
		}
		overflow = 0;
		handsDepth = 0;
	}

	private static void push(byte kind, @Nullable ItemStack stack, @Nullable ItemDisplayContext context) {
		if (depth >= CAPACITY) {
			overflow++;
			return;
		}
		Frame parent = depth > 0 ? FRAMES[depth - 1] : null;
		Frame f = FRAMES[depth++];
		f.kind = kind;
		f.stack = stack;
		f.context = context;
		f.guiAncestor = (parent != null && parent.guiAncestor) || context == ItemDisplayContext.GUI;
		f.armorAncestor = (parent != null && parent.armorAncestor) || kind == ARMOR_FRAME;
		f.bewlrAncestor = (parent != null && parent.bewlrAncestor) || kind == BEWLR_FRAME;
		f.resolvedMask = 0;
		f.looks[0] = null;
		f.looks[1] = null;
		f.looks[2] = null;
		f.looks[3] = null;
	}

	/**
	 * Display context to rule target, the same table as the outline system uses (duplicated on purpose).
	 */
	static OutlineTarget targetOf(@Nullable ItemDisplayContext context) {
		return targetOf(context, false);
	}

	/**
	 * Like {@link #targetOf(ItemDisplayContext)}, but inside the first-person hand pass the hand contexts (first and
	 * third person) both count as FIRST_PERSON: first-person animation mods (for example Punchy) cancel the vanilla arm
	 * and draw the held item with a THIRD_PERSON hand context from inside {@code renderHandsWithItems}. Same rule as
	 * the outline system's HAND phase. Other contexts keep their normal target.
	 */
	static OutlineTarget targetOf(@Nullable ItemDisplayContext context, boolean inHands) {
		if (context == null) {
			return OutlineTarget.OTHER;
		}
		return switch (context) {
			case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> OutlineTarget.FIRST_PERSON;
			case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND ->
				inHands ? OutlineTarget.FIRST_PERSON : OutlineTarget.THIRD_PERSON;
			case GROUND -> OutlineTarget.DROPPED;
			case FIXED -> OutlineTarget.ITEM_FRAME;
			case GUI -> OutlineTarget.GUI;
			case HEAD, NONE -> OutlineTarget.OTHER;
		};
	}

	/**
	 * One level of the stack.
	 */
	static final class Frame {
		/** The stack being drawn; may be empty, null in tests. */
		@Nullable
		ItemStack stack;
		/** Null for armor. */
		@Nullable
		ItemDisplayContext context;
		byte kind;
		boolean guiAncestor;
		boolean armorAncestor;
		boolean bewlrAncestor;
		/** Bit {@code 1 << category.ordinal()} set once that category's look is cached in {@link #looks}. */
		int resolvedMask;
		final GlintLook[] looks = new GlintLook[GlintCategory.values().length];

		boolean isResolved(GlintCategory category) {
			return (resolvedMask & (1 << category.ordinal())) != 0;
		}

		void cache(GlintCategory category, @Nullable GlintLook look) {
			looks[category.ordinal()] = look;
			resolvedMask |= 1 << category.ordinal();
		}
	}
}
