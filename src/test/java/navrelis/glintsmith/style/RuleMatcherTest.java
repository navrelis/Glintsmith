package navrelis.glintsmith.style;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import navrelis.glintsmith.config.GlintsmithConfig;
import navrelis.glintsmith.config.OutlineCondition;
import org.junit.jupiter.api.Test;

class RuleMatcherTest {
	private static final OutlineTarget HAND = OutlineTarget.FIRST_PERSON;

	private static GlintsmithConfig config() {
		GlintsmithConfig c = new GlintsmithConfig();
		c.rules = new ArrayList<>();
		return c.sanitize();
	}

	private static ItemFacts sword(boolean enchanted) {
		return new ItemFacts("minecraft:diamond_sword", Set.of("minecraft:swords"),
			enchanted ? Set.of("minecraft:sharpness") : Set.of(), Set.of(), enchanted, enchanted);
	}

	private static Rule rule(String name, String item) {
		Rule r = new Rule();
		r.name = name;
		if (item != null) {
			r.items.add(item);
		}
		return r;
	}

	private static ResolvedStyle resolve(ItemFacts f, GlintsmithConfig c) {
		return RuleMatcher.resolve(f, HAND, GlintCategory.ITEM, c.sanitize());
	}

	@Test
	void conditionEnchantedOutlinesOnlyEnchantedOrFoilItems() {
		GlintsmithConfig c = config();
		assertNotNull(resolve(sword(true), c).outline());
		assertNull(resolve(sword(false), c).outline());
		// foil without enchantments, e.g. an enchanted golden apple
		ItemFacts apple = new ItemFacts("minecraft:enchanted_golden_apple", Set.of(), Set.of(), Set.of(), false, true);
		assertNotNull(resolve(apple, c).outline());
	}

	@Test
	void conditionAllAndNone() {
		GlintsmithConfig c = config();
		c.outline.condition = OutlineCondition.ALL;
		assertNotNull(resolve(sword(false), c).outline());
		c.outline.condition = OutlineCondition.NONE;
		assertNull(resolve(sword(true), c).outline());
		// NONE plus an allowing rule is a whitelist
		Rule allow = rule("allow", "minecraft:diamond_sword");
		allow.outline = OutlineSettings.enabled(true);
		c.rules.add(allow);
		assertNotNull(resolve(sword(false), c).outline());
		assertNull(resolve(ItemFacts.plain("minecraft:stick"), c).outline());
	}

	@Test
	void masterSwitchAndTargetFlagsAreHardLimits() {
		GlintsmithConfig c = config();
		Rule allow = rule("all", "*");
		allow.outline = OutlineSettings.enabled(true);
		c.rules.add(allow);
		assertNotNull(resolve(sword(false), c).outline());
		assertNull(RuleMatcher.resolve(sword(true), OutlineTarget.GUI, GlintCategory.GUI, c).outline());
		c.outline.targets.gui = true;
		assertNotNull(RuleMatcher.resolve(sword(true), OutlineTarget.GUI, GlintCategory.GUI, c).outline());
		c.outline.enabled = false;
		assertNull(resolve(sword(true), c).outline());
	}

	@Test
	void ruleOrderLaterRulesOverrideEarlier() {
		GlintsmithConfig c = config();
		c.outline.condition = OutlineCondition.NONE;
		Rule allow = rule("allow swords", "#minecraft:swords");
		allow.outline = OutlineSettings.colored(ColorStyle.of("#FF0000"));
		Rule deny = rule("deny diamond", "minecraft:diamond_sword");
		deny.outline = OutlineSettings.enabled(false);
		c.rules.add(allow);
		c.rules.add(deny);
		assertNull(resolve(sword(true), c).outline());

		// swapped order: the allow wins
		c.rules.clear();
		c.rules.add(deny);
		c.rules.add(allow);
		OutlineLook look = resolve(sword(true), c).outline();
		assertNotNull(look);
		assertEquals(0xFFFF0000, look.color().evaluate(0, null));
	}

	@Test
	void rulesOverlayOnlyNonNullFields() {
		GlintsmithConfig c = config();
		c.outline.thickness = 3.0f;
		c.outline.opacity = 0.8f;
		Rule a = rule("colour", "minecraft:diamond_sword");
		a.outline = OutlineSettings.colored(ColorStyle.of("#00FF00"));
		Rule b = rule("thickness", "minecraft:diamond_sword");
		b.outline = new OutlineSettings();
		b.outline.thickness = 5.0f;
		b.outline.softness = 0.5f;
		c.rules.add(a);
		c.rules.add(b);
		OutlineLook look = resolve(sword(true), c).outline();
		assertEquals(0xFF00FF00, look.color().evaluate(0, null));
		assertEquals(5.0f, look.thickness());
		assertEquals(0.5f, look.softness());
		assertEquals(0.8f, look.opacity());
	}

	@Test
	void resolvedValuesAreClamped() {
		GlintsmithConfig c = config();
		Rule r = rule("wild", "*");
		r.outline = new OutlineSettings();
		r.outline.thickness = 100.0f;
		r.outline.opacity = -4.0f;
		c.rules.add(r);
		// bypass sanitize on purpose: RuleMatcher itself must be safe
		OutlineLook look = RuleMatcher.resolve(sword(true), HAND, GlintCategory.ITEM, c).outline();
		assertEquals(8.0f, look.thickness());
		assertEquals(0.0f, look.opacity());
	}

	@Test
	void targetsFilter() {
		GlintsmithConfig c = config();
		c.outline.condition = OutlineCondition.NONE;
		Rule r = rule("hand only", "minecraft:diamond_sword");
		r.targets.add(OutlineTarget.FIRST_PERSON);
		r.targets.add(OutlineTarget.THIRD_PERSON);
		r.outline = OutlineSettings.enabled(true);
		c.rules.add(r);
		c.sanitize();
		assertNotNull(RuleMatcher.resolve(sword(true), OutlineTarget.FIRST_PERSON, GlintCategory.ITEM, c).outline());
		assertNotNull(RuleMatcher.resolve(sword(true), OutlineTarget.THIRD_PERSON, GlintCategory.ITEM, c).outline());
		assertNull(RuleMatcher.resolve(sword(true), OutlineTarget.DROPPED, GlintCategory.ITEM, c).outline());
		assertNull(RuleMatcher.resolve(sword(true), OutlineTarget.ARMOR, GlintCategory.ARMOR, c).outline());
	}

	@Test
	void excludeItemsBlockAMatch() {
		GlintsmithConfig c = config();
		c.outline.condition = OutlineCondition.NONE;
		Rule r = rule("swords except wooden", "#minecraft:swords");
		r.excludeItems.add("minecraft:diamond_sword");
		r.outline = OutlineSettings.enabled(true);
		c.rules.add(r);
		assertNull(resolve(sword(true), c).outline());
		ItemFacts iron = new ItemFacts("minecraft:iron_sword", Set.of("minecraft:swords"), Set.of(), Set.of(), false, false);
		assertNotNull(resolve(iron, c).outline());
	}

	@Test
	void enchantmentPatternsMatchAnyPresentEnchantment() {
		GlintsmithConfig c = config();
		c.outline.condition = OutlineCondition.NONE;
		Rule r = new Rule();
		r.enchantments.add("minecraft:mending");
		r.enchantments.add("minecraft:sharpness");
		r.outline = OutlineSettings.enabled(true);
		c.rules.add(r);
		assertNotNull(resolve(sword(true), c).outline());

		ItemFacts fortune = new ItemFacts("minecraft:diamond_pickaxe", Set.of(), Set.of("minecraft:fortune"), Set.of(), true, true);
		assertNull(resolve(fortune, c).outline());
		assertNull(resolve(sword(false), c).outline());

		// enchantment tag
		c.rules.clear();
		Rule tagRule = new Rule();
		tagRule.enchantments.add("#minecraft:curse");
		tagRule.outline = OutlineSettings.enabled(true);
		c.rules.add(tagRule);
		ItemFacts cursed = new ItemFacts("minecraft:diamond_pickaxe", Set.of(), Set.of("minecraft:vanishing_curse"),
			Set.of("minecraft:curse"), true, true);
		assertNotNull(resolve(cursed, c).outline());
		assertNull(resolve(fortune, c).outline());
	}

	@Test
	void enchantedCondition() {
		GlintsmithConfig c = config();
		c.outline.condition = OutlineCondition.ALL;
		Rule notEnchanted = rule("plain only", "minecraft:diamond_sword");
		notEnchanted.enchantedCondition = Rule.Condition.NOT_ENCHANTED;
		notEnchanted.outline = OutlineSettings.enabled(false);
		c.rules.add(notEnchanted);
		assertNull(resolve(sword(false), c).outline());
		assertNotNull(resolve(sword(true), c).outline());

		c.rules.clear();
		Rule enchantedOnly = rule("enchanted only", "minecraft:diamond_sword");
		enchantedOnly.enchantedCondition = Rule.Condition.ENCHANTED;
		enchantedOnly.outline = OutlineSettings.enabled(false);
		c.rules.add(enchantedOnly);
		assertNotNull(resolve(sword(false), c).outline());
		assertNull(resolve(sword(true), c).outline());
	}

	@Test
	void invalidAndDisabledRulesAreSkipped() {
		GlintsmithConfig c = config();
		c.outline.condition = OutlineCondition.NONE;
		Rule broken = rule("broken", "minecraft:diamond_sword");
		broken.items.add("a:b:c");
		broken.outline = OutlineSettings.enabled(true);
		Rule off = rule("off", "minecraft:diamond_sword");
		off.enabled = false;
		off.outline = OutlineSettings.enabled(true);
		c.rules.add(broken);
		c.rules.add(off);
		c.sanitize();
		assertNotNull(broken.error);
		assertTrue(broken.error.contains("a:b:c"));
		assertNull(off.error);
		assertNull(resolve(sword(true), c).outline());
		// the broken rule is kept in the config
		assertEquals(2, c.rules.size());
		assertEquals(List.of("minecraft:diamond_sword", "a:b:c"), c.rules.get(0).items);
	}

	@Test
	void ruleWithoutConstraintsMatchesEverything() {
		GlintsmithConfig c = config();
		c.outline.condition = OutlineCondition.NONE;
		Rule r = new Rule();
		r.outline = OutlineSettings.enabled(true);
		c.rules.add(r);
		assertNotNull(resolve(ItemFacts.plain("any:thing"), c).outline());
	}

	@Test
	void defaultRulesOutlineTotemsAndSkipBooks() {
		GlintsmithConfig c = new GlintsmithConfig().sanitize();
		OutlineLook totem = resolve(ItemFacts.plain("minecraft:totem_of_undying"), c).outline();
		assertNotNull(totem);
		assertEquals(0xFFFFF3A5, totem.color().evaluate(0, null));
		ItemFacts book = new ItemFacts("minecraft:enchanted_book", Set.of(), Set.of("minecraft:mending"), Set.of(), true, true);
		assertNull(resolve(book, c).outline());
		assertNotNull(resolve(sword(true), c).outline());
	}

	// ---- glint ----

	@Test
	void vanillaGlintResolvesToNull() {
		GlintsmithConfig c = config();
		c.outline.enabled = false;
		assertSame(ResolvedStyle.NONE, resolve(sword(true), c));
		assertNull(resolve(sword(true), c).glint());
	}

	@Test
	void categoryDefaultsApplyPerCategory() {
		GlintsmithConfig c = config();
		c.glint.armor.speed = 2.0f;
		c.glint.armor.tint = ColorStyle.of("#00FF00");
		c.glint.item.texture = "glintsmith:rainbow";
		GlintLook armor = RuleMatcher.resolve(sword(true), OutlineTarget.ARMOR, GlintCategory.ARMOR, c.sanitize()).glint();
		assertNotNull(armor);
		assertEquals(2.0f, armor.speed());
		assertNull(armor.texture());
		assertEquals(0xFF00FF00, armor.tint().evaluate(0, null));
		GlintLook item = resolve(sword(true), c).glint();
		assertEquals("glintsmith:rainbow", item.texture());
		assertEquals(1.0f, item.speed());
		assertNull(item.tint());
		assertNull(RuleMatcher.resolve(sword(true), OutlineTarget.GUI, GlintCategory.GUI, c).glint());
	}

	@Test
	void glintRulesMergeWithCategoryDefaults() {
		GlintsmithConfig c = config();
		c.glint.item.speed = 3.0f;
		c.glint.item.scale = 2.0f;
		Rule r = rule("sword glint", "minecraft:diamond_sword");
		r.glint = new GlintSettings();
		r.glint.scale = 0.5f;
		r.glint.texture = "config:fire.png";
		r.glint.rotation = -45.0f;
		c.rules.add(r);
		GlintLook look = resolve(sword(true), c).glint();
		assertEquals(3.0f, look.speed());
		assertEquals(0.5f, look.scale());
		assertEquals(-45.0f, look.rotation());
		assertEquals("config:fire.png", look.texture());
		assertTrue(look.enabled());

		// another item keeps the category defaults
		ItemFacts stick = new ItemFacts("minecraft:stick", Set.of(), Set.of(), Set.of(), false, true);
		GlintLook other = resolve(stick, c).glint();
		assertEquals(2.0f, other.scale());
		assertNull(other.texture());
	}

	@Test
	void glintCanBeHiddenAndTextureResetToVanilla() {
		GlintsmithConfig c = config();
		c.glint.item.texture = "glintsmith:rainbow";
		Rule hide = rule("hide on swords", "#minecraft:swords");
		hide.glint = new GlintSettings();
		hide.glint.enabled = false;
		Rule vanilla = rule("vanilla books", "minecraft:enchanted_book");
		vanilla.glint = new GlintSettings();
		vanilla.glint.texture = GlintSettings.VANILLA_TEXTURE;
		c.rules.add(hide);
		c.rules.add(vanilla);
		GlintLook hidden = resolve(sword(true), c).glint();
		assertFalse(hidden.enabled());
		ItemFacts book = new ItemFacts("minecraft:enchanted_book", Set.of(), Set.of("minecraft:mending"), Set.of(), true, true);
		assertNull(resolve(book, c).glint());
	}

	@Test
	void glintMasterSwitchOffMeansUntouched() {
		GlintsmithConfig c = config();
		c.glint.item.speed = 5.0f;
		c.glint.enabled = false;
		assertNull(resolve(sword(true), c).glint());
	}

	@Test
	void lookKeyIsStableAndDistinguishesLooks() {
		GlintsmithConfig c = config();
		c.glint.item.speed = 2.0f;
		GlintLook a = resolve(sword(true), c).glint();
		GlintLook b = resolve(sword(true), c).glint();
		assertEquals(a, b);
		assertEquals(a.lookKey(), b.lookKey());
		c.glint.item.tint = ColorStyle.rainbow(1.0f);
		GlintLook tinted = resolve(sword(true), c).glint();
		assertNotEquals(a, tinted);
		assertNotEquals(a.lookKey(), tinted.lookKey());
		c.glint.item.tint = ColorStyle.rainbow(1.0f);
		assertEquals(tinted.lookKey(), resolve(sword(true), c).glint().lookKey());
	}
}
