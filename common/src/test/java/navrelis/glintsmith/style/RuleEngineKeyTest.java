package navrelis.glintsmith.style;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Tests the cache keys of {@link RuleEngine} without touching the engine itself (which needs Minecraft and the
 * Fabric loader). The item and enchantment fields are left null; they compare with the same helpers as the rest.
 */
class RuleEngineKeyTest {
	private static RuleEngine.StyleKey style(boolean foil, OutlineTarget target, GlintCategory category) {
		RuleEngine.StyleKey key = new RuleEngine.StyleKey();
		key.set(null, null, null, foil, target, category);
		return key;
	}

	@Test
	void styleKeysCompareByEveryField() {
		RuleEngine.StyleKey a = style(true, OutlineTarget.GUI, GlintCategory.ITEM);
		RuleEngine.StyleKey same = style(true, OutlineTarget.GUI, GlintCategory.ITEM);
		assertEquals(a, same);
		assertEquals(a.hashCode(), same.hashCode());
		assertNotEquals(a, style(false, OutlineTarget.GUI, GlintCategory.ITEM));
		assertNotEquals(a, style(true, OutlineTarget.OTHER, GlintCategory.ITEM));
		assertNotEquals(a, style(true, OutlineTarget.GUI, GlintCategory.ARMOR));
		assertNotEquals(a, null);
		assertNotEquals(a, new RuleEngine.EnchantKey());
	}

	@Test
	void copyIsIndependentOfTheLookupKey() {
		RuleEngine.StyleKey lookup = style(true, OutlineTarget.GUI, GlintCategory.ITEM);
		RuleEngine.StyleKey stored = lookup.copy();
		assertNotSame(lookup, stored);
		assertEquals(lookup, stored);

		Map<RuleEngine.StyleKey, String> map = new HashMap<>();
		map.put(stored, "gui");
		lookup.set(null, null, null, false, OutlineTarget.ARMOR, GlintCategory.ARMOR);
		assertNull(map.get(lookup));
		lookup.set(null, null, null, true, OutlineTarget.GUI, GlintCategory.ITEM);
		assertEquals("gui", map.get(lookup));
		lookup.set(null, null, null, false, OutlineTarget.ARMOR, GlintCategory.ARMOR);
		assertEquals("gui", map.get(stored), "the stored key was not changed by refilling the lookup");
	}

	@Test
	void enchantKeysCopyAndCompare() {
		RuleEngine.EnchantKey lookup = new RuleEngine.EnchantKey();
		lookup.set(null, null);
		RuleEngine.EnchantKey stored = lookup.copy();
		assertNotSame(lookup, stored);
		assertEquals(lookup, stored);
		assertEquals(lookup.hashCode(), stored.hashCode());
		Map<RuleEngine.EnchantKey, int[]> map = new HashMap<>();
		int[] colors = {1, 2};
		map.put(stored, colors);
		assertSame(colors, map.get(lookup));
	}
}
