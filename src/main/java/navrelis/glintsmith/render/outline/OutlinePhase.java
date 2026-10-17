package navrelis.glintsmith.render.outline;

/**
 * The render passes in which item geometry can be captured for outlines.
 */
public enum OutlinePhase {
	NONE,
	/** Entities, block entities and dropped items, composited at the water-mask flush. */
	WORLD,
	/**
	 * First-person hands, composited right after {@code renderHandsWithItems}. Held items drawn there with a first-
	 * or third-person hand context are outlined as FIRST_PERSON (see {@link OutlineContexts#forPhase}).
	 */
	HAND,
	/** One {@code GuiGraphics.renderItem} call, composited right after its own flush. */
	GUI
}
