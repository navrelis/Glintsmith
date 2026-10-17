package navrelis.glintsmith.style;

import java.util.Set;

/**
 * Everything the rule matcher needs to know about a stack, without any Minecraft types.
 *
 * @param itemId {@code namespace:path}
 * @param itemTags item tags as {@code namespace:path} (no leading {@code #})
 * @param enchantments enchantment ids on the stack (also stored enchantments of books)
 * @param enchantmentTags union of the tags of those enchantments, {@code namespace:path}
 * @param enchanted has at least one enchantment
 * @param hasFoil would show the vanilla glint (also true for e.g. golden apples)
 */
public record ItemFacts(
	String itemId,
	Set<String> itemTags,
	Set<String> enchantments,
	Set<String> enchantmentTags,
	boolean enchanted,
	boolean hasFoil
) {
	/**
	 * @return facts with {@code enchanted} derived from the enchantments
	 */
	public static ItemFacts of(String itemId, Set<String> itemTags, Set<String> enchantments,
		Set<String> enchantmentTags, boolean hasFoil) {
		return new ItemFacts(itemId, itemTags, enchantments, enchantmentTags, !enchantments.isEmpty(), hasFoil);
	}

	/**
	 * @return facts of a plain, unenchanted item without tags
	 */
	public static ItemFacts plain(String itemId) {
		return new ItemFacts(itemId, Set.of(), Set.of(), Set.of(), false, false);
	}
}
