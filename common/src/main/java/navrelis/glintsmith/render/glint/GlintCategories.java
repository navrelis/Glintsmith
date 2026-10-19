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
	 * Whether the glint UVs of a requested kind are the item's block-atlas UVs, i.e. whether the atlas factor
	 * ({@link GlintAtlasScale}) applies (design 5.4.1). True only for the item glint kinds ({@code glint},
	 * {@code glint_translucent}) requested directly below {@code ItemRenderer.render} (the innermost frame is an item
	 * frame), which draws the baked model's quads with their block-atlas UVs. False for:
	 * <ul>
	 * <li>no frame: the drawer and its UVs are unknown;</li>
	 * <li>an armor or BEWLR innermost frame: armor and elytra models use the 0..1 UVs of their own textures, block
	 * entity item renderers use entity textures or other atlases (shield patterns, banners, chests);</li>
	 * <li>the entity glint kinds: entity models (thrown trident, armor) with their own texture UVs;</li>
	 * <li>compasses and clocks with foil ({@code decalFoil}): vanilla draws their glint through a
	 * {@code SheetedDecalTextureGenerator}, which derives the UVs from the position, not from the atlas.</li>
	 * </ul>
	 * The category does not matter: an item drawn in the GUI or below a BEWLR still has block-atlas UVs.
	 */
	static boolean blockAtlasUvs(GlintKind kind, @Nullable GlintContext.Frame frame, boolean decalFoil) {
		return frame != null && frame.kind == GlintContext.ITEM_FRAME && !decalFoil
			&& (kind == GlintKind.GLINT || kind == GlintKind.GLINT_TRANSLUCENT);
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
