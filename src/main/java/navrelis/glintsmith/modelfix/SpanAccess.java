package navrelis.glintsmith.modelfix;

/**
 * Read access to the bounds of the package-private {@code ItemModelGenerator.Span}, implemented on it by a mixin.
 */
public interface SpanAccess {
	/** First pixel covered by the span. */
	int glintsmith$min();

	/** Last pixel covered by the span. */
	int glintsmith$max();
}
