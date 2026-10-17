package navrelis.glintsmith.style;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

class PatternTest {
	private static ItemFacts item(String id, String... tags) {
		return new ItemFacts(id, Set.of(tags), Set.of(), Set.of(), false, false);
	}

	private static ItemFacts enchanted(Set<String> enchantments, Set<String> tags) {
		return new ItemFacts("minecraft:diamond_sword", Set.of(), enchantments, tags, !enchantments.isEmpty(), false);
	}

	private static boolean item(String pattern, ItemFacts facts) {
		Pattern p = Pattern.parse(pattern);
		assertNotNull(p, pattern);
		return p.matchesItem(facts);
	}

	@Test
	void exactId() {
		assertTrue(item("minecraft:diamond_sword", item("minecraft:diamond_sword")));
		assertFalse(item("minecraft:diamond_sword", item("minecraft:iron_sword")));
		assertFalse(item("minecraft:diamond_sword", item("mymod:diamond_sword")));
		assertTrue(item("MyMod:Thing", item("mymod:thing")));
	}

	@Test
	void defaultNamespaceIsMinecraft() {
		assertTrue(item("diamond_sword", item("minecraft:diamond_sword")));
		assertFalse(item("diamond_sword", item("mymod:diamond_sword")));
	}

	@Test
	void tags() {
		ItemFacts sword = item("minecraft:diamond_sword", "minecraft:swords", "c:tools");
		assertTrue(item("#minecraft:swords", sword));
		assertTrue(item("#swords", sword));
		assertTrue(item("#c:tools", sword));
		assertFalse(item("#minecraft:axes", sword));
		assertFalse(item("#minecraft:swords", item("minecraft:diamond_sword")));
	}

	@Test
	void namespace() {
		assertTrue(item("@mymod", item("mymod:anything")));
		assertTrue(item("@mymod", item("mymod:a/b")));
		assertFalse(item("@mymod", item("mymod2:anything")));
		assertFalse(item("@mymod", item("minecraft:mymod")));
	}

	@Test
	void globs() {
		assertTrue(item("*:*_sword", item("minecraft:iron_sword")));
		assertTrue(item("*:*_sword", item("mymod:ruby_sword")));
		assertFalse(item("*:*_sword", item("mymod:ruby_axe")));
		assertTrue(item("mymod:*", item("mymod:x")));
		assertFalse(item("mymod:*", item("other:x")));
		assertTrue(item("minecraft:*_pickaxe", item("minecraft:netherite_pickaxe")));
		assertTrue(item("minecraft:?ron_sword", item("minecraft:iron_sword")));
		assertFalse(item("minecraft:?ron_sword", item("minecraft:iiron_sword")));
		// no namespace in a glob defaults to minecraft
		assertTrue(item("*_sword", item("minecraft:iron_sword")));
		assertFalse(item("*_sword", item("mymod:iron_sword")));
		// dots are literal
		assertFalse(item("mymod:a.b", item("mymod:axb")));
		assertFalse(item("mymod:a.*", item("mymod:axb")));
	}

	@Test
	void star() {
		assertTrue(item("*", item("minecraft:stone")));
		assertTrue(item("*", item("mymod:whatever")));
	}

	@Test
	void negation() {
		Pattern p = Pattern.parse("!minecraft:diamond_sword");
		assertNotNull(p);
		assertTrue(p.negated());
		assertFalse(p.matchesItem(item("minecraft:diamond_sword")));
		assertTrue(p.matchesItem(item("minecraft:iron_sword")));
		assertFalse(Pattern.parse("!#minecraft:swords").matchesItem(item("a:b", "minecraft:swords")));
		assertFalse(Pattern.parse("! @mymod").matchesItem(item("mymod:x")));
	}

	@Test
	void invalidPatternsReturnNull() {
		for (String bad : new String[] {null, "", "  ", "!", "#", "@", "a:b:c", "minecraft:", ":x", "mine craft:x",
			"bad$char", "#bad!", "@bad:ns", "#a*", "!!x", "UPPER CASE", "a:b*:c"}) {
			assertNull(Pattern.parse(bad), String.valueOf(bad));
		}
	}

	@Test
	void enchantmentMatching() {
		ItemFacts facts = enchanted(Set.of("minecraft:sharpness", "mymod:zap"), Set.of("minecraft:curse", "c:weapon"));
		assertTrue(Pattern.parse("minecraft:sharpness").matchesEnchantments(facts));
		assertTrue(Pattern.parse("sharpness").matchesEnchantments(facts));
		assertFalse(Pattern.parse("minecraft:mending").matchesEnchantments(facts));
		assertTrue(Pattern.parse("@mymod").matchesEnchantments(facts));
		assertTrue(Pattern.parse("mymod:z*").matchesEnchantments(facts));
		assertTrue(Pattern.parse("#c:weapon").matchesEnchantments(facts));
		assertFalse(Pattern.parse("#minecraft:treasure").matchesEnchantments(facts));
		assertTrue(Pattern.parse("*").matchesEnchantments(facts));
		assertFalse(Pattern.parse("*").matchesEnchantments(enchanted(Set.of(), Set.of())));
		assertFalse(Pattern.parse("!minecraft:sharpness").matchesEnchantments(facts));
		assertTrue(Pattern.parse("!minecraft:mending").matchesEnchantments(facts));
	}

	@Test
	void listCombinesPositiveAndNegative() {
		List<Pattern> list = List.of(Pattern.parse("#minecraft:swords"), Pattern.parse("!minecraft:wooden_sword"));
		ItemFacts iron = item("minecraft:iron_sword", "minecraft:swords");
		ItemFacts wooden = item("minecraft:wooden_sword", "minecraft:swords");
		ItemFacts axe = item("minecraft:iron_axe", "minecraft:axes");
		assertTrue(Pattern.matchesList(list, p -> p.matchesItem(iron)));
		assertFalse(Pattern.matchesList(list, p -> p.matchesItem(wooden)));
		assertFalse(Pattern.matchesList(list, p -> p.matchesItem(axe)));
		// only negatives: everything but
		List<Pattern> onlyNot = List.of(Pattern.parse("!minecraft:wooden_sword"));
		assertTrue(Pattern.matchesList(onlyNot, p -> p.matchesItem(iron)));
		assertFalse(Pattern.matchesList(onlyNot, p -> p.matchesItem(wooden)));
		// several positives: any
		List<Pattern> any = List.of(Pattern.parse("minecraft:iron_sword"), Pattern.parse("minecraft:iron_axe"));
		assertTrue(Pattern.matchesList(any, p -> p.matchesItem(axe)));
		assertTrue(Pattern.matchesList(List.of(), p -> false));
	}
}
