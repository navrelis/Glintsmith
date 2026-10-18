package navrelis.glintsmith.render.glint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import navrelis.glintsmith.config.GlintsmithConfig;
import navrelis.glintsmith.style.ColorStyle;
import navrelis.glintsmith.style.GlintCategory;
import navrelis.glintsmith.style.GlintLook;
import navrelis.glintsmith.style.GlintSettings;
import navrelis.glintsmith.style.ItemFacts;
import navrelis.glintsmith.style.OutlineTarget;
import navrelis.glintsmith.style.Rule;
import navrelis.glintsmith.style.RuleMatcher;

class GlintRuntimeSettingsTest {
	private static GlintsmithConfig defaults() {
		return new GlintsmithConfig().sanitize();
	}

	private static GlintsmithConfig.GlintCategorySettings category(GlintsmithConfig config, GlintCategory c) {
		return config.glint.forCategory(c);
	}

	@Test
	void defaultConfigIsNotCustom() {
		GlintRuntimeSettings s = GlintRuntimeSettings.from(defaults());
		assertFalse(s.anyCustom());
		assertFalse(GlintRuntimeSettings.VANILLA.anyCustom());
		for (GlintCategory c : GlintCategory.values()) {
			assertTrue(s.get(c).isDefault(), c.name());
			GlintLook look = s.get(c).categoryLook();
			assertTrue(look.enabled());
			assertNull(look.texture());
			assertNull(look.tint());
			assertEquals(1.0f, look.speed());
			assertEquals(1.0f, look.scale());
			assertEquals(1.0f, look.strength());
			assertEquals(10.0f, look.rotation());
		}
	}

	@Test
	void defaultRulesDoNotMakeItCustom() {
		GlintsmithConfig config = defaults();
		assertFalse(config.rules.isEmpty());
		assertFalse(GlintRuntimeSettings.from(config).anyCustom());
	}

	@Test
	void everySingleNonDefaultCategoryFieldMakesItCustom() {
		record Change(String name, Consumer<GlintsmithConfig.GlintCategorySettings> apply) {
		}
		Change[] changes = {
			new Change("disabled", c -> c.enabled = false),
			new Change("texture", c -> c.texture = "preset:rainbow"),
			new Change("file texture", c -> c.texture = "file:mine.png"),
			new Change("tint", c -> c.tint = ColorStyle.of("#FF0000")),
			new Change("speed", c -> c.speed = 2.0f),
			new Change("slow", c -> c.speed = 0.0f),
			new Change("scale", c -> c.scale = 2.0f),
			new Change("strength", c -> c.strength = 0.5f),
			new Change("rotation", c -> c.rotation = 0.0f),
		};
		for (GlintCategory category : GlintCategory.values()) {
			for (Change change : changes) {
				GlintsmithConfig config = defaults();
				change.apply().accept(category(config, category));
				config.sanitize();
				assertTrue(GlintRuntimeSettings.from(config).anyCustom(), category + " " + change.name());
			}
		}
	}

	@Test
	void vanillaTextureSpellingsAreStillDefault() {
		for (String spec : new String[] {null, "vanilla", "default"}) {
			GlintsmithConfig config = defaults();
			config.glint.item.texture = spec;
			config.sanitize();
			GlintRuntimeSettings s = GlintRuntimeSettings.from(config);
			assertFalse(s.anyCustom(), String.valueOf(spec));
			assertNull(s.item().texture(), "normalised to null: " + spec);
		}
	}

	@Test
	void masterSwitchOffMeansNotCustom() {
		GlintsmithConfig config = defaults();
		config.glint.item.tint = ColorStyle.of("#00FF00");
		config.glint.gui.enabled = false;
		Rule rule = new Rule();
		rule.glint = new GlintSettings();
		rule.glint.speed = 3.0f;
		config.rules.add(rule);
		config.sanitize();
		assertTrue(GlintRuntimeSettings.from(config).anyCustom());

		config.glint.enabled = false;
		assertFalse(GlintRuntimeSettings.from(config).anyCustom());
	}

	@Test
	void aRuleWithAGlintOverrideMakesItCustom() {
		GlintsmithConfig config = defaults();
		Rule rule = new Rule();
		rule.name = "tinted swords";
		rule.items.add("minecraft:diamond_sword");
		rule.glint = new GlintSettings();
		rule.glint.tint = ColorStyle.of("#FF8800");
		config.rules.add(rule);
		config.sanitize();
		assertTrue(GlintRuntimeSettings.from(config).anyCustom());
	}

	@Test
	void everyKindOfRuleOverrideCounts() {
		for (int field = 0; field < 7; field++) {
			GlintsmithConfig config = defaults();
			Rule rule = new Rule();
			rule.glint = new GlintSettings();
			switch (field) {
				case 0 -> rule.glint.enabled = false;
				case 1 -> rule.glint.texture = "preset:aurora";
				case 2 -> rule.glint.tint = ColorStyle.of("#123456");
				case 3 -> rule.glint.speed = 2.0f;
				case 4 -> rule.glint.scale = 2.0f;
				case 5 -> rule.glint.strength = 0.1f;
				default -> rule.glint.rotation = 45.0f;
			}
			config.rules.add(rule);
			config.sanitize();
			assertTrue(GlintRuntimeSettings.from(config).anyCustom(), "rule field " + field);
		}
	}

	@Test
	void brokenAndDisabledRulesAreIgnored() {
		GlintsmithConfig config = defaults();
		Rule broken = new Rule();
		broken.items.add("minecraft:[unclosed");
		broken.glint = new GlintSettings();
		broken.glint.speed = 2.0f;
		Rule disabled = new Rule();
		disabled.enabled = false;
		disabled.glint = new GlintSettings();
		disabled.glint.speed = 2.0f;
		Rule outlineOnly = new Rule();
		outlineOnly.name = "outline only";
		config.rules.add(broken);
		config.rules.add(disabled);
		config.rules.add(outlineOnly);
		config.sanitize();
		assertNotNull(broken.error, "the pattern should be invalid for this test to mean anything");
		assertFalse(GlintRuntimeSettings.from(config).anyCustom());
	}

	@Test
	void categoryLookCarriesTheCompiledTint() {
		GlintsmithConfig config = defaults();
		config.glint.armor.tint = ColorStyle.of("#336699");
		config.glint.armor.texture = "preset:frost";
		config.glint.armor.speed = 2.5f;
		config.glint.armor.scale = 3.0f;
		config.glint.armor.strength = 0.25f;
		config.glint.armor.rotation = -40.0f;
		config.sanitize();
		GlintRuntimeSettings s = GlintRuntimeSettings.from(config);
		GlintLook look = s.armor().categoryLook();
		assertTrue(look.enabled());
		assertEquals("preset:frost", look.texture());
		assertNotNull(look.tint());
		assertEquals(0xFF336699, look.tint().evaluate(0.0, null));
		assertNotNull(look.tintStyle());
		assertEquals(2.5f, look.speed());
		assertEquals(3.0f, look.scale());
		assertEquals(0.25f, look.strength());
		assertEquals(-40.0f, look.rotation());
		assertTrue(s.anyCustom());
		assertTrue(s.get(GlintCategory.ITEM).isDefault());
		assertFalse(s.get(GlintCategory.ARMOR).isDefault());
		assertEquals(s.armor(), s.get(GlintCategory.ARMOR));
	}

	@Test
	void disabledCategoryKeepsItsLookDisabled() {
		GlintsmithConfig config = defaults();
		config.glint.gui.enabled = false;
		GlintRuntimeSettings s = GlintRuntimeSettings.from(config);
		assertFalse(s.gui().categoryLook().enabled());
		assertTrue(s.item().categoryLook().enabled());
	}

	@Test
	void settingsAreIndependentOfLaterConfigEdits() {
		GlintsmithConfig config = defaults();
		config.glint.item.tint = ColorStyle.of("#112233");
		GlintRuntimeSettings s = GlintRuntimeSettings.from(config);
		config.glint.item.tint.colors.set(0, "#FFFFFF");
		assertEquals(0xFF112233, s.item().categoryLook().tint().evaluate(0.0, null));
	}

	@Test
	void nullConfigIsVanilla() {
		assertFalse(GlintRuntimeSettings.from(null).anyCustom());
		GlintsmithConfig config = defaults();
		config.glint = null;
		assertFalse(GlintRuntimeSettings.from(config).anyCustom());
	}

	// ---- what the renderer relies on: GlintLook arrives fully merged (category defaults, then every matching rule)

	private static final ItemFacts SWORD = new ItemFacts("minecraft:diamond_sword", Set.of("minecraft:swords"), Set.of(), Set.of(), false, true);
	private static final ItemFacts STICK = new ItemFacts("minecraft:stick", Set.of(), Set.of(), Set.of(), false, true);

	private static GlintLook lookOf(ItemFacts facts, GlintCategory category, GlintsmithConfig config) {
		return RuleMatcher.resolve(facts, OutlineTarget.OTHER, category, config.sanitize()).glint();
	}

	private static Rule swordRule(Consumer<GlintSettings> glint) {
		Rule rule = new Rule();
		rule.name = "sword glint";
		rule.items.add("minecraft:diamond_sword");
		rule.glint = new GlintSettings();
		glint.accept(rule.glint);
		return rule;
	}

	@Test
	void ruleLooksAreMergedOverTheCategoryDefaultsIncludingRotation() {
		GlintsmithConfig config = defaults();
		config.glint.item.rotation = 40.0f;
		config.glint.item.speed = 2.0f;
		config.glint.item.scale = 3.0f;
		config.rules.add(swordRule(g -> {
			g.texture = "preset:fire";
			g.tint = ColorStyle.of("#FF8800");
		}));

		GlintLook sword = lookOf(SWORD, GlintCategory.ITEM, config);
		assertNotNull(sword);
		assertTrue(sword.enabled());
		assertEquals("preset:fire", sword.texture());
		assertNotNull(sword.tint());
		assertEquals(40.0f, sword.rotation(), "rotation is inherited from the category");
		assertEquals(2.0f, sword.speed());
		assertEquals(3.0f, sword.scale());
		assertEquals(1.0f, sword.strength());
	}

	@Test
	void aRuleCanOverrideTheCategoryRotationAndDisableTheGlint() {
		GlintsmithConfig config = defaults();
		config.glint.item.rotation = 40.0f;
		config.rules.add(swordRule(g -> g.rotation = -90.0f));
		assertEquals(-90.0f, lookOf(SWORD, GlintCategory.ITEM, config).rotation());
		assertEquals(40.0f, lookOf(STICK, GlintCategory.ITEM, config).rotation());

		GlintsmithConfig off = defaults();
		off.glint.item.speed = 2.0f;
		off.rules.add(swordRule(g -> g.enabled = false));
		assertFalse(lookOf(SWORD, GlintCategory.ITEM, off).enabled());
		assertTrue(lookOf(STICK, GlintCategory.ITEM, off).enabled());
	}

	@Test
	void anItemWithoutARuleGetsExactlyTheCategoryLook() {
		GlintsmithConfig config = defaults();
		config.glint.armor.texture = "preset:frost";
		config.glint.armor.tint = ColorStyle.of("#336699");
		config.glint.armor.speed = 2.5f;
		config.glint.armor.scale = 0.5f;
		config.glint.armor.strength = 0.4f;
		config.glint.armor.rotation = 100.0f;
		GlintRuntimeSettings s = GlintRuntimeSettings.from(config.sanitize());

		GlintLook viaRules = lookOf(STICK, GlintCategory.ARMOR, config);
		GlintLook category = s.armor().categoryLook();
		assertEquals(category.enabled(), viaRules.enabled());
		assertEquals(category.texture(), viaRules.texture());
		assertEquals(category.speed(), viaRules.speed());
		assertEquals(category.scale(), viaRules.scale());
		assertEquals(category.strength(), viaRules.strength());
		assertEquals(category.rotation(), viaRules.rotation());
		assertEquals(category.tint().evaluate(0.0, null), viaRules.tint().evaluate(0.0, null));
	}

	@Test
	void masterSwitchOffAndVanillaCategoriesGiveNoLook() {
		GlintsmithConfig config = defaults();
		config.glint.item.speed = 2.0f;
		config.rules.add(swordRule(g -> g.tint = ColorStyle.of("#FF0000")));
		config.glint.enabled = false;
		for (GlintCategory c : GlintCategory.values()) {
			assertNull(lookOf(SWORD, c, config), c + " must stay vanilla with the master switch off");
		}
		assertFalse(GlintRuntimeSettings.from(config).anyCustom());

		// everything at the defaults and no rule glint: nothing to do either
		assertNull(lookOf(SWORD, GlintCategory.ITEM, defaults()));
	}

	@Test
	void aDisabledCategoryHidesTheGlintOfEveryItem() {
		GlintsmithConfig config = defaults();
		config.glint.gui.enabled = false;
		GlintLook look = lookOf(STICK, GlintCategory.GUI, config);
		assertNotNull(look);
		assertFalse(look.enabled());
		assertNull(lookOf(STICK, GlintCategory.ITEM, config), "the other categories stay vanilla");
		assertTrue(GlintRuntimeSettings.from(config).anyCustom());
	}

	@Test
	void atlasScaleFollowsTheConfigAndNeverMakesTheGlintCustom() {
		GlintsmithConfig config = defaults();
		GlintRuntimeSettings on = GlintRuntimeSettings.from(config);
		assertTrue(on.atlasScale(), "on by default");
		assertFalse(on.anyCustom(), "the atlas factor alone keeps everything vanilla");
		assertTrue(GlintRuntimeSettings.VANILLA.atlasScale());
		assertTrue(GlintRuntimeSettings.from(null).atlasScale());

		config.glint.atlasIndependentScale = false;
		GlintRuntimeSettings off = GlintRuntimeSettings.from(config);
		assertFalse(off.atlasScale());
		assertFalse(off.anyCustom());

		config.glint.item.texture = "preset:rainbow";
		assertTrue(GlintRuntimeSettings.from(config).anyCustom());
		assertFalse(GlintRuntimeSettings.from(config).atlasScale());
	}
}
