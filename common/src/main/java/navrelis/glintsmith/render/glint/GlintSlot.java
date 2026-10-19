package navrelis.glintsmith.render.glint;

/**
 * One pooled glint render type together with the params its shards read at draw time.
 *
 * @param <T> the render type class ({@code RenderType} in the game, anything in tests)
 */
final class GlintSlot<T> {
	final GlintKind kind;
	/** 0..31 for a look slot, -1 for the kind's "off" type. */
	final int index;
	final GlintParams params = new GlintParams();
	T type;
	/** Handed out and not yet drawn. */
	boolean inUse;
	/** Added to the main source's fixed buffers. */
	boolean registered;
	/**
	 * Holds geometry in a buffer source that draws it later without passing G5c (Iris's
	 * {@code FullyBufferedMultiBufferSource}, G7). A pinned slot is not freed by {@link GlintSlotPool#release}, so its
	 * params cannot be handed to another look before that source has drawn it; it is freed at the next frame start.
	 */
	boolean pinned;
	/** Frame of the last acquire. */
	long frame;

	GlintSlot(GlintKind kind, int index) {
		this.kind = kind;
		this.index = index;
	}

	boolean isOff() {
		return index < 0;
	}

	// Shard bodies; they only read the params and never throw.

	void setupTexture() {
		GlintShards.setupTexture(this);
	}

	void setupTexturing() {
		GlintShards.setupTexturing(this);
	}

	void clearTexturing() {
		GlintShards.clearTexturing();
	}
}
