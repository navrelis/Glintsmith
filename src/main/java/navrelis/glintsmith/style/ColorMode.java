package navrelis.glintsmith.style;

/**
 * How a {@link ColorStyle} produces its colour over time.
 */
public enum ColorMode {
	/** The first colour, unchanged. */
	STATIC,
	/** Hue cycles through the whole spectrum, one cycle per 4 seconds at speed 1. */
	RAINBOW,
	/** One colour: brightness pulses. Two or more colours: eases between the first two. */
	PULSE,
	/** Cycles smoothly through all colours in order and loops back to the first. */
	GRADIENT,
	/** Like {@link #GRADIENT}, but over the colours of the enchantments on the stack. */
	ENCHANTMENT
}
