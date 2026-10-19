package navrelis.glintsmith.style;

/**
 * Where an item is being drawn. Rules can restrict themselves to some of these.
 * (Not called RenderTarget on purpose: Mojang has a class of that name.)
 */
public enum OutlineTarget {
	FIRST_PERSON,
	THIRD_PERSON,
	ARMOR,
	DROPPED,
	ITEM_FRAME,
	GUI,
	OTHER
}
