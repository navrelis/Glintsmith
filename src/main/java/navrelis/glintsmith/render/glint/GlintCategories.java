package navrelis.glintsmith.render.glint;

import navrelis.glintsmith.style.GlintCategory;
import navrelis.glintsmith.style.OutlineTarget;
import org.jetbrains.annotations.Nullable;

/**
 * The category decision table (design 5.3). Pure.
 */
final class GlintCategories {
	private GlintCategories() {
	}

	/**
	 * Which glint layer a requested vanilla kind belongs to, given the innermost context frame.
	 * <pre>
	 * GUI ancestor                          GUI
	 * armor_entity_glint, armor ancestor    ARMOR
	 * entity glints, BEWLR ancestor         ENTITY
	 * otherwise                             ITEM
	 * </pre>
	 */
	static GlintCategory decide(GlintKind kind, @Nullable GlintContext.Frame frame) {
		if (frame != null && frame.guiAncestor) {
			return GlintCategory.GUI;
		}
		if (kind == GlintKind.ARMOR_ENTITY_GLINT || (frame != null && frame.armorAncestor)) {
			return GlintCategory.ARMOR;
		}
		if (kind == GlintKind.ENTITY_GLINT || kind == GlintKind.ENTITY_GLINT_DIRECT || (frame != null && frame.bewlrAncestor)) {
			return GlintCategory.ENTITY;
		}
		return GlintCategory.ITEM;
	}

	/**
	 * The rule target used to look up the rules of a frame drawn as {@code category}.
	 */
	static OutlineTarget targetFor(GlintCategory category, GlintContext.Frame frame) {
		return switch (category) {
			case GUI -> OutlineTarget.GUI;
			case ARMOR -> OutlineTarget.ARMOR;
			default -> GlintContext.targetOf(frame.context, GlintContext.inHands());
		};
	}
}
