package navrelis.glintsmith.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.gson.JsonParseException;
import navrelis.glintsmith.style.ColorMode;
import navrelis.glintsmith.style.ColorStyle;
import navrelis.glintsmith.style.GlintSettings;
import navrelis.glintsmith.style.OutlineSettings;
import navrelis.glintsmith.style.OutlineTarget;
import navrelis.glintsmith.style.Rule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigTest {
	@TempDir
	Path dir;

	private Path file() {
		return dir.resolve("glintsmith.json");
	}

	// ---- model ----

	@Test
	void defaultsMatchTheSpec() {
		GlintsmithConfig c = new GlintsmithConfig().sanitize();
		assertEquals(1, c.version);
		assertTrue(c.outline.enabled);
		assertEquals(OutlineCondition.ENCHANTED, c.outline.condition);
		assertTrue(c.outline.targets.firstPerson);
		assertTrue(c.outline.targets.thirdPerson);
		assertFalse(c.outline.targets.armor);
		assertTrue(c.outline.targets.dropped);
		assertTrue(c.outline.targets.itemFrame);
		assertFalse(c.outline.targets.gui);
		assertFalse(c.outline.targets.other);
		assertFalse(c.outline.onlyLocalPlayer);
		assertFalse(c.outline.disableWithShaders);
		assertTrue(c.outline.handWithShaderPack);
		assertTrue(c.outline.scaleWithResolution);
		assertEquals(0.0f, c.outline.fadeStart);
		assertEquals(0.0f, c.outline.fadeEnd);
		assertTrue(c.warnings().isEmpty());
		assertEquals(ColorMode.STATIC, c.outline.color.mode);
		assertEquals(List.of("#D088FC"), c.outline.color.colors);
		assertEquals(2.0f, c.outline.thickness);
		assertEquals(0.0f, c.outline.softness);
		assertEquals(1.0f, c.outline.opacity);
		assertTrue(c.glint.enabled);
		assertEquals(10.0f, c.glint.item.rotation);
		assertNull(c.glint.armor.texture);
		assertNull(c.glint.gui.tint);
		assertEquals(GapFixMode.AUTO, c.gapFix.mode);
		assertEquals(0.0001f, c.gapFix.sideIndent);
		assertEquals(0.005f, c.gapFix.sideExpansion);
		assertTrue(c.durabilityAlert.enabled);
		assertEquals(0.10f, c.durabilityAlert.threshold);
		assertTrue(c.enchantmentColors.isEmpty());
		assertEquals(2, c.rules.size());
		Rule totem = c.rules.get(0);
		assertEquals("Totem of Undying", totem.name);
		assertEquals(List.of("minecraft:totem_of_undying"), totem.items);
		assertEquals(Rule.Condition.ANY, totem.enchantedCondition);
		assertEquals(Boolean.TRUE, totem.outline.enabled);
		assertEquals(List.of("#FFF3A5"), totem.outline.color.colors);
		Rule books = c.rules.get(1);
		assertEquals(List.of("minecraft:enchanted_book"), books.items);
		assertEquals(Boolean.FALSE, books.outline.enabled);
		assertTrue(books.enabled);
		assertNull(totem.error);
		assertNull(books.error);
	}

	@Test
	void targetsMapping() {
		GlintsmithConfig.Targets t = new GlintsmithConfig.Targets();
		assertTrue(t.allows(OutlineTarget.FIRST_PERSON));
		assertFalse(t.allows(OutlineTarget.GUI));
		assertFalse(t.allows(OutlineTarget.OTHER));
		t.gui = true;
		t.dropped = false;
		t.other = true;
		assertTrue(t.allows(OutlineTarget.GUI));
		assertFalse(t.allows(OutlineTarget.DROPPED));
		assertTrue(t.allows(OutlineTarget.OTHER));
	}

	@Test
	void outlineThicknessIsClampedToSixteen() {
		GlintsmithConfig c = new GlintsmithConfig();
		c.outline.thickness = 16.5f;
		c.sanitize();
		assertEquals(16.0f, c.outline.thickness);
		c.outline.thickness = 16.0f;
		c.sanitize();
		assertEquals(16.0f, c.outline.thickness);
		c.outline.thickness = 12.0f;
		c.sanitize();
		assertEquals(12.0f, c.outline.thickness);
	}

	@Test
	void fadeRangeIsClamped() {
		GlintsmithConfig c = new GlintsmithConfig();
		c.outline.fadeStart = -5.0f;
		c.outline.fadeEnd = 1000.0f;
		c.sanitize();
		assertEquals(0.0f, c.outline.fadeStart);
		assertEquals(256.0f, c.outline.fadeEnd);

		c.outline.fadeStart = Float.NaN;
		c.outline.fadeEnd = Float.NaN;
		c.sanitize();
		assertEquals(0.0f, c.outline.fadeStart);
		assertEquals(0.0f, c.outline.fadeEnd);

		c.outline.fadeStart = 300.0f;
		c.outline.fadeEnd = 20.0f;
		c.sanitize();
		assertEquals(255.0f, c.outline.fadeStart);
		assertEquals(256.0f, c.outline.fadeEnd);
	}

	@Test
	void fadeEndIsRepairedToBeGreaterThanFadeStart() {
		GlintsmithConfig c = new GlintsmithConfig();
		c.outline.fadeStart = 10.0f;
		c.outline.fadeEnd = 5.0f;
		c.sanitize();
		assertEquals(10.0f, c.outline.fadeStart);
		assertEquals(11.0f, c.outline.fadeEnd);

		c.outline.fadeStart = 10.0f;
		c.outline.fadeEnd = 10.0f;
		c.sanitize();
		assertEquals(10.0f, c.outline.fadeStart);
		assertEquals(11.0f, c.outline.fadeEnd);

		c.outline.fadeStart = 256.0f;
		c.outline.fadeEnd = 256.0f;
		c.sanitize();
		assertEquals(255.0f, c.outline.fadeStart);
		assertEquals(256.0f, c.outline.fadeEnd);

		c.outline.fadeStart = 20.0f;
		c.outline.fadeEnd = 40.0f;
		c.sanitize();
		assertEquals(20.0f, c.outline.fadeStart);
		assertEquals(40.0f, c.outline.fadeEnd);

		// 0 / 0 stays off
		c.outline.fadeStart = 0.0f;
		c.outline.fadeEnd = 0.0f;
		c.sanitize();
		assertEquals(0.0f, c.outline.fadeStart);
		assertEquals(0.0f, c.outline.fadeEnd);

		// fadeEnd 0 means off whatever fadeStart is
		c.outline.fadeStart = 30.0f;
		c.outline.fadeEnd = 0.0f;
		c.sanitize();
		assertEquals(30.0f, c.outline.fadeStart);
		assertEquals(0.0f, c.outline.fadeEnd);
	}

	@Test
	void categoryWithInvalidTextureIsResetWithOneWarning() {
		GlintsmithConfig c = new GlintsmithConfig();
		c.glint.armor.texture = "preset:nope";
		c.glint.item.texture = "preset:fire";
		c.glint.gui.texture = "file:glow.png";
		c.sanitize();
		assertNull(c.glint.armor.texture);
		assertEquals("preset:fire", c.glint.item.texture);
		assertEquals("file:glow.png", c.glint.gui.texture);
		assertEquals(1, c.warnings().size());
		assertTrue(c.warnings().get(0).contains("glint.armor.texture"), c.warnings().get(0));

		// a later sanitize starts over
		c.sanitize();
		assertTrue(c.warnings().isEmpty());
	}

	@Test
	void blankCategoryTextureBecomesNullWithoutWarning() {
		GlintsmithConfig c = new GlintsmithConfig();
		c.glint.entity.texture = "   ";
		c.sanitize();
		assertNull(c.glint.entity.texture);
		assertTrue(c.warnings().isEmpty());
	}

	@Test
	void warningsAreNeverNullAndNeverSerialised() {
		GlintsmithConfig parsed = ConfigManager.fromJson("{\"glint\": {\"armor\": {\"texture\": \"file:../x.png\"}}}");
		assertNotNull(parsed.warnings(), "before sanitize");
		parsed.sanitize();
		assertEquals(1, parsed.warnings().size());
		assertNull(parsed.glint.armor.texture);
		assertFalse(ConfigManager.toJson(parsed).contains("warnings"));

		GlintsmithConfig copy = parsed.copy();
		assertNotNull(copy.warnings());
		assertTrue(copy.warnings().isEmpty(), "the copy is already repaired");
		assertFalse(ConfigManager.toJson(copy).contains("warnings"));
	}

	@Test
	void loadLogsAndExposesWarnings() throws IOException {
		Files.writeString(file(), "{\"glint\": {\"gui\": {\"texture\": \"bogus\"}}}");
		ConfigManager m = ConfigManager.forPath(file());
		GlintsmithConfig c = m.load();
		assertNull(c.glint.gui.texture);
		assertEquals(1, m.warnings().size());
		assertTrue(m.warnings().get(0).contains("glint.gui.texture"));
		assertNull(m.lastError());
	}

	@Test
	void ruleWithInvalidGlintTextureIsFlaggedButKept() {
		GlintsmithConfig c = new GlintsmithConfig();
		Rule bad = new Rule();
		bad.items.add("minecraft:stick");
		bad.glint = new GlintSettings();
		bad.glint.texture = "file:../secret.png";
		Rule good = new Rule();
		good.items.add("minecraft:stick");
		good.glint = new GlintSettings();
		good.glint.texture = "preset:aurora";
		c.rules.add(bad);
		c.rules.add(good);
		c.sanitize();
		assertEquals(4, c.rules.size());
		assertNotNull(bad.error);
		assertEquals("file:../secret.png", bad.glint.texture);
		assertNull(good.error);

		bad.glint.texture = "file:secret.png";
		c.sanitize();
		assertNull(bad.error);
	}

	@Test
	void defaultsRoundTripThroughJson() {
		GlintsmithConfig original = new GlintsmithConfig().sanitize();
		String json = ConfigManager.toJson(original);
		GlintsmithConfig parsed = ConfigManager.fromJson(json).sanitize();
		assertEquals(json, ConfigManager.toJson(parsed));
		assertEquals(original.rules.size(), parsed.rules.size());
		// transient state must not leak into the file
		assertFalse(json.contains("error"));
		assertFalse(json.contains("itemPatterns"));
		assertFalse(json.contains("warnings"));
		// nulls (inherit) are omitted
		assertFalse(json.contains("null"));
	}

	@Test
	void customisedConfigRoundTrips() {
		GlintsmithConfig c = new GlintsmithConfig();
		c.outline.condition = OutlineCondition.ALL;
		c.outline.color = ColorStyle.rainbow(2.5f);
		c.glint.armor.texture = "file:fire.png";
		c.glint.armor.tint = ColorStyle.of("#FF0000");
		c.gapFix.mode = GapFixMode.CUSTOM;
		c.enchantmentColors.put("mymod:zap", "#112233");
		Rule r = new Rule();
		r.name = "x";
		r.items.add("#minecraft:swords");
		r.targets.add(OutlineTarget.GUI);
		r.enchantedCondition = Rule.Condition.ENCHANTED;
		r.glint = new GlintSettings();
		r.glint.scale = 2.0f;
		c.rules.add(r);
		c.sanitize();

		String json = ConfigManager.toJson(c);
		GlintsmithConfig parsed = ConfigManager.fromJson(json).sanitize();
		assertEquals(json, ConfigManager.toJson(parsed));
		assertEquals(OutlineCondition.ALL, parsed.outline.condition);
		assertEquals(ColorMode.RAINBOW, parsed.outline.color.mode);
		assertEquals(GapFixMode.CUSTOM, parsed.gapFix.mode);
		assertEquals("#112233", parsed.enchantmentColors.get("mymod:zap"));
		Rule parsedRule = parsed.rules.get(2);
		assertEquals(List.of(OutlineTarget.GUI), parsedRule.targets);
		assertEquals(2.0f, parsedRule.glint.scale);
		assertNull(parsedRule.glint.speed);
	}

	@Test
	void copyIsDeepAndEqual() {
		GlintsmithConfig c = new GlintsmithConfig().sanitize();
		GlintsmithConfig copy = c.copy();
		assertNotSame(c, copy);
		assertEquals(ConfigManager.toJson(c), ConfigManager.toJson(copy));
		copy.rules.get(0).items.add("minecraft:stick");
		copy.outline.color.colors.add("#000000");
		assertEquals(1, c.rules.get(0).items.size());
		assertEquals(1, c.outline.color.colors.size());
	}

	@Test
	void sanitizeClampsEveryRange() {
		GlintsmithConfig c = new GlintsmithConfig();
		c.outline.thickness = 100.0f;
		c.outline.softness = -1.0f;
		c.outline.opacity = 7.0f;
		c.glint.item.speed = 99.0f;
		c.glint.item.scale = 0.0f;
		c.glint.armor.strength = 3.0f;
		c.glint.entity.rotation = 400.0f;
		c.glint.gui.rotation = -400.0f;
		c.glint.gui.texture = "  ";
		c.gapFix.uvShrinkMultiplier = 5.0f;
		c.gapFix.sideExpansion = -5.0f;
		c.gapFix.sideIndent = 5.0f;
		c.durabilityAlert.threshold = -2.0f;
		c.durabilityAlert.color = "nope";
		c.sanitize();

		assertEquals(16.0f, c.outline.thickness);
		assertEquals(0.0f, c.outline.softness);
		assertEquals(1.0f, c.outline.opacity);
		assertEquals(10.0f, c.glint.item.speed);
		assertEquals(0.1f, c.glint.item.scale);
		assertEquals(1.0f, c.glint.armor.strength);
		assertEquals(180.0f, c.glint.entity.rotation);
		assertEquals(-180.0f, c.glint.gui.rotation);
		assertNull(c.glint.gui.texture);
		assertEquals(1.0f, c.gapFix.uvShrinkMultiplier);
		assertEquals(-0.1f, c.gapFix.sideExpansion);
		assertEquals(0.1f, c.gapFix.sideIndent);
		assertEquals(0.0f, c.durabilityAlert.threshold);
		assertEquals("#FF2A2A", c.durabilityAlert.color);

		c.outline.thickness = 0.0f;
		c.outline.opacity = Float.NaN;
		c.sanitize();
		assertEquals(0.5f, c.outline.thickness);
		assertEquals(1.0f, c.outline.opacity);
	}

	@Test
	void sanitizeReplacesNullSectionsAndDropsJunk() {
		GlintsmithConfig c = new GlintsmithConfig();
		c.outline = null;
		c.glint = null;
		c.gapFix = null;
		c.durabilityAlert = null;
		c.enchantmentColors = null;
		c.rules = null;
		c.sanitize();
		assertNotNull(c.outline);
		assertNotNull(c.outline.targets);
		assertNotNull(c.glint.item);
		assertNotNull(c.gapFix);
		assertNotNull(c.durabilityAlert);
		assertTrue(c.enchantmentColors.isEmpty());
		assertTrue(c.rules.isEmpty());

		c.enchantmentColors = new java.util.LinkedHashMap<>();
		c.enchantmentColors.put("mymod:ok", "#123456");
		c.enchantmentColors.put("mymod:bad", "purple");
		c.enchantmentColors.put(null, "#123456");
		c.rules = new ArrayList<>();
		c.rules.add(null);
		Rule nulls = new Rule();
		nulls.items = null;
		nulls.targets = null;
		nulls.enchantedCondition = null;
		nulls.name = null;
		c.rules.add(nulls);
		c.sanitize();
		assertEquals(Map.of("mymod:ok", "#123456"), c.enchantmentColors);
		assertEquals(1, c.rules.size());
		assertNull(c.rules.get(0).error);
		assertEquals(Rule.Condition.ANY, c.rules.get(0).enchantedCondition);
	}

	@Test
	void sanitizeFlagsInvalidRulesButKeepsThem() {
		GlintsmithConfig c = new GlintsmithConfig();
		Rule bad = new Rule();
		bad.items.add("not a pattern!");
		bad.excludeItems.add("also:bad:id");
		bad.enchantments.add("#");
		bad.outline = new OutlineSettings();
		bad.outline.thickness = 50.0f;
		c.rules.add(bad);
		c.sanitize();
		assertEquals(3, c.rules.size());
		assertNotNull(bad.error);
		assertTrue(bad.error.contains("not a pattern!"));
		assertEquals(List.of("not a pattern!"), bad.items);
		assertEquals(16.0f, bad.outline.thickness);
		// a later sanitize clears the error once fixed
		bad.items.clear();
		bad.excludeItems.clear();
		bad.enchantments.clear();
		c.sanitize();
		assertNull(bad.error);
	}

	@Test
	void unknownEnumValuesAndMissingFieldsFallBack() {
		GlintsmithConfig c = ConfigManager.fromJson("""
			{"outline": {"condition": "WHATEVER", "thickness": 3}, "gapFix": null, "extra": 1}
			""").sanitize();
		assertEquals(OutlineCondition.ENCHANTED, c.outline.condition);
		assertEquals(3.0f, c.outline.thickness);
		assertNotNull(c.gapFix);
		assertEquals(2, c.rules.size(), "missing rules keep the defaults");
		GlintsmithConfig empty = ConfigManager.fromJson("{\"rules\": []}").sanitize();
		assertTrue(empty.rules.isEmpty(), "an explicit empty list stays empty");
	}

	@Test
	void strictParsing() {
		for (String bad : new String[] {"{", "[]", "not json", "null", "{} trailing", "{'single': 1}", "{\"outline\": 5}"}) {
			assertThrows(RuntimeException.class, () -> ConfigManager.fromJson(bad), bad);
		}
		assertThrows(JsonParseException.class, () -> ConfigManager.fromJson("{ broken"));
	}

	// ---- manager ----

	@Test
	void firstLoadWritesDefaults() throws IOException {
		ConfigManager m = ConfigManager.forPath(file());
		GlintsmithConfig c = m.load();
		assertTrue(Files.exists(file()));
		assertNull(m.lastError());
		assertSame(c, m.get());
		assertEquals(ConfigManager.toJson(new GlintsmithConfig().sanitize()), Files.readString(file()));
		assertFalse(Files.exists(dir.resolve("glintsmith.json.tmp")));
	}

	@Test
	void loadReadsAndSanitizesTheFile() throws IOException {
		Files.writeString(file(), "{\"outline\": {\"thickness\": 999, \"color\": {\"colors\": [\"zzz\"]}}}");
		ConfigManager m = ConfigManager.forPath(file());
		GlintsmithConfig c = m.load();
		assertEquals(16.0f, c.outline.thickness);
		assertEquals(List.of("#D088FC"), c.outline.color.colors);
		assertNull(m.lastError());
	}

	@Test
	void brokenJsonKeepsPreviousConfigAndSetsLastError() throws IOException {
		ConfigManager m = ConfigManager.forPath(file());
		m.load();
		GlintsmithConfig edited = m.get().copy();
		edited.outline.thickness = 5.0f;
		assertTrue(m.save(edited));

		Files.writeString(file(), "{ this is : not json");
		GlintsmithConfig kept = m.load();
		assertEquals(5.0f, kept.outline.thickness);
		assertNotNull(m.lastError());
		assertEquals(5.0f, m.get().outline.thickness);
		assertEquals("{ this is : not json", Files.readString(file()), "the broken file is left for the user to fix");

		Files.writeString(file(), "");
		m.load();
		assertNotNull(m.lastError());
		assertEquals(5.0f, m.get().outline.thickness);

		// fixed file clears the error
		Files.writeString(file(), "{\"outline\": {\"thickness\": 4}}");
		m.load();
		assertNull(m.lastError());
		assertEquals(4.0f, m.get().outline.thickness);
	}

	@Test
	void brokenJsonOnFirstLoadUsesDefaults() throws IOException {
		Files.writeString(file(), "garbage");
		ConfigManager m = ConfigManager.forPath(file());
		GlintsmithConfig c = m.load();
		assertNotNull(m.lastError());
		assertEquals(2.0f, c.outline.thickness);
		assertEquals(2, c.rules.size());
	}

	@Test
	void saveSanitizesWritesAndNotifies() throws IOException {
		ConfigManager m = ConfigManager.forPath(file());
		m.load();
		List<GlintsmithConfig> seen = new ArrayList<>();
		m.addListener(seen::add);

		GlintsmithConfig edited = new GlintsmithConfig();
		edited.outline.thickness = 100.0f;
		assertTrue(m.save(edited));
		assertEquals(1, seen.size());
		assertEquals(16.0f, seen.get(0).outline.thickness);
		assertSame(seen.get(0), m.get());
		assertNotSame(edited, m.get());
		assertEquals(100.0f, edited.outline.thickness, "the caller's object is not modified");

		ConfigManager reread = ConfigManager.forPath(file());
		assertEquals(16.0f, reread.load().outline.thickness);
	}

	@Test
	void listenerExceptionsAreContained() {
		ConfigManager m = ConfigManager.forPath(file());
		AtomicInteger calls = new AtomicInteger();
		m.addListener(c -> {
			throw new IllegalStateException("boom");
		});
		m.addListener(c -> calls.incrementAndGet());
		m.load();
		m.save(new GlintsmithConfig());
		assertEquals(2, calls.get());
	}

	@Test
	void removedListenerIsNotCalled() {
		ConfigManager m = ConfigManager.forPath(file());
		AtomicInteger calls = new AtomicInteger();
		java.util.function.Consumer<GlintsmithConfig> l = c -> calls.incrementAndGet();
		m.addListener(l);
		m.load();
		m.removeListener(l);
		m.load();
		assertEquals(1, calls.get());
	}

	@Test
	void saveIntoMissingFolderCreatesIt() {
		Path nested = dir.resolve("a").resolve("b").resolve("glintsmith.json");
		ConfigManager m = ConfigManager.forPath(nested);
		assertTrue(m.save(new GlintsmithConfig()));
		assertTrue(Files.exists(nested));
	}

	@Test
	void tickReloadsExternalChangesOnlyEveryTwentyTicksAndIgnoresOwnSaves() throws IOException {
		ConfigManager m = ConfigManager.forPath(file());
		m.load();
		AtomicInteger notifications = new AtomicInteger();
		m.addListener(c -> notifications.incrementAndGet());

		// our own save does not trigger a reload
		GlintsmithConfig edited = m.get().copy();
		edited.outline.thickness = 3.0f;
		m.save(edited);
		assertEquals(1, notifications.get());
		for (int i = 0; i < 100; i++) {
			m.tick();
		}
		assertEquals(1, notifications.get());

		// external change
		Files.writeString(file(), "{\"outline\": {\"thickness\": 6}}");
		Files.setLastModifiedTime(file(), FileTime.fromMillis(System.currentTimeMillis() + 5000));
		for (int i = 0; i < 19; i++) {
			m.tick();
		}
		assertEquals(3.0f, m.get().outline.thickness, "not checked before the 20th tick");
		m.tick();
		assertEquals(6.0f, m.get().outline.thickness);
		assertEquals(2, notifications.get());

		// unchanged afterwards
		for (int i = 0; i < 100; i++) {
			m.tick();
		}
		assertEquals(2, notifications.get());
	}

	@Test
	void brokenExternalEditIsReportedOnceAndRetriedOnlyWhenTheFileChanges() throws IOException {
		ConfigManager m = ConfigManager.forPath(file());
		m.load();
		Files.writeString(file(), "{ nope");
		Files.setLastModifiedTime(file(), FileTime.fromMillis(System.currentTimeMillis() + 5000));
		AtomicInteger notifications = new AtomicInteger();
		m.addListener(c -> notifications.incrementAndGet());
		for (int i = 0; i < 100; i++) {
			m.tick();
		}
		assertEquals(1, notifications.get());
		assertNotNull(m.lastError());
		assertEquals(2.0f, m.get().outline.thickness);
	}

	// ---- atlas-independent glint scale (2.0.0) ----

	@Test
	void atlasIndependentScaleIsOnByDefault() {
		assertTrue(new GlintsmithConfig().sanitize().glint.atlasIndependentScale);
		assertTrue(new GlintsmithConfig.Glint().atlasIndependentScale);
	}

	@Test
	void configsWithoutTheFieldGetItOn() throws IOException {
		// A 1.0.0 file: a full glint section, but no atlasIndependentScale.
		String old = "{\"version\": 1, \"glint\": {\"enabled\": true, \"item\": {\"texture\": \"preset:rainbow\", \"scale\": 0.13},"
			+ " \"armor\": {}, \"entity\": {}, \"gui\": {}}}";
		assertTrue(ConfigManager.fromJson(old).sanitize().glint.atlasIndependentScale);
		assertTrue(ConfigManager.fromJson("{}").sanitize().glint.atlasIndependentScale, "no glint section at all");
		assertTrue(ConfigManager.fromJson("{\"glint\": null}").sanitize().glint.atlasIndependentScale);

		Files.writeString(file(), old);
		ConfigManager m = ConfigManager.forPath(file());
		GlintsmithConfig loaded = m.load();
		assertTrue(loaded.glint.atlasIndependentScale);
		assertEquals(0.13f, loaded.glint.item.scale, 1e-6f, "other values are kept");
		assertTrue(m.save(loaded));
		assertTrue(Files.readString(file()).contains("\"atlasIndependentScale\": true"), "written back on the next save");
	}

	@Test
	void atlasIndependentScaleOffIsKept() {
		GlintsmithConfig c = new GlintsmithConfig();
		c.glint.atlasIndependentScale = false;
		GlintsmithConfig back = ConfigManager.fromJson(ConfigManager.toJson(c)).sanitize();
		assertFalse(back.glint.atlasIndependentScale);
		assertFalse(c.copy().glint.atlasIndependentScale);
		assertFalse(ConfigManager.fromJson("{\"glint\": {\"atlasIndependentScale\": false}}").sanitize().glint.atlasIndependentScale);
	}

	@Test
	void textureDirIsUnderTheConfigFolder() {
		ConfigManager m = ConfigManager.forPath(file());
		Path textures = m.textureDir();
		assertTrue(Files.isDirectory(textures));
		assertEquals(dir.resolve("glintsmith").resolve("glints").toAbsolutePath(), textures.toAbsolutePath());
	}
}
