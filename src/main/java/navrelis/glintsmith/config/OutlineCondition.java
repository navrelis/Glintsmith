package navrelis.glintsmith.config;

/**
 * Which items get an outline before the rules are applied.
 */
public enum OutlineCondition {
	/** Items with enchantments or the vanilla glint. */
	ENCHANTED,
	/** Every item. */
	ALL,
	/** No item; only rules can switch outlines on. */
	NONE
}
