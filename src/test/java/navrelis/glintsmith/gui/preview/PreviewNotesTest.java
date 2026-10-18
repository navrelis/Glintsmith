package navrelis.glintsmith.gui.preview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import navrelis.glintsmith.config.GlintsmithConfig;
import navrelis.glintsmith.config.OutlineCondition;
import navrelis.glintsmith.config.TestConfigs;
import navrelis.glintsmith.style.GlintSettings;
import navrelis.glintsmith.style.ItemFacts;
import navrelis.glintsmith.style.Rule;
import org.junit.jupiter.api.Test;

class PreviewNotesTest {
	private static final ItemFacts ENCHANTED = new ItemFacts("minecraft:diamond_sword", Set.of("minecraft:swords"),
		Set.of("minecraft:sharpness"), Set.of(), true, true);
	private static final ItemFacts PLAIN = new ItemFacts("minecraft:diamond_sword", Set.of("minecraft:swords"),
		Set.of(), Set.of(), false, false);

	private static GlintsmithConfig config() {
		GlintsmithConfig c = TestConfigs.vanilla();
		c.rules = new ArrayList<>();
		return c.sanitize();
	}

	/** A config whose rules give every item a custom glint. */
	private static GlintsmithConfig withGlintRule() {
		GlintsmithConfig c = config();
		Rule rule = new Rule();
		rule.glint = new GlintSettings();
		rule.glint.speed = 2.0f;
		c.rules.add(rule);
		return c.sanitize();
	}

	private static List<String> keys(GlintsmithConfig snap, ItemFacts facts, boolean map, boolean shader, boolean noWorld,
		boolean emptyHand, boolean failsafe) {
		return PreviewNotes.keys(snap, facts, map, shader, noWorld, emptyHand, failsafe);
	}

	private static List<String> keys(GlintsmithConfig snap, ItemFacts facts) {
		return keys(snap, facts, false, false, false, false, false);
	}

	@Test
	void anEnchantedItemUnderDefaultsHasNoNotes() {
		assertEquals(List.of(), keys(config(), ENCHANTED));
	}

	@Test
	void theMasterSwitchComesFirstAndHidesTheOtherOutlineReasons() {
		GlintsmithConfig snap = config();
		snap.outline.enabled = false;
		snap.outline.targets.firstPerson = false;
		assertEquals(List.of(PreviewNotes.OUTLINES_OFF), keys(snap, ENCHANTED));
		assertEquals(List.of(PreviewNotes.OUTLINES_OFF), keys(snap, PLAIN, true, true, false, false, false),
			"neither the map nor the shader pack note adds to it");
	}

	@Test
	void firstPersonOff() {
		GlintsmithConfig snap = config();
		snap.outline.targets.firstPerson = false;
		assertEquals(List.of(PreviewNotes.FIRST_PERSON_OFF), keys(snap, PLAIN));
	}

	@Test
	void noOutlineForTheItem() {
		assertEquals(List.of(PreviewNotes.NO_OUTLINE), keys(config(), PLAIN));
		GlintsmithConfig none = config();
		none.outline.condition = OutlineCondition.NONE;
		assertEquals(List.of(PreviewNotes.NO_OUTLINE), keys(none, ENCHANTED));
	}

	@Test
	void mapsHaveNoOutlineEvenWhenTheRulesWouldGiveOne() {
		GlintsmithConfig snap = config();
		snap.outline.condition = OutlineCondition.ALL;
		assertEquals(List.of(PreviewNotes.MAP), keys(snap, PLAIN, true, false, false, false, false));
		assertEquals(List.of(PreviewNotes.NO_OUTLINE), keys(config(), PLAIN, true, false, false, false, false),
			"the item has no outline anyway, that reason wins");
	}

	@Test
	void shaderPackNotes() {
		GlintsmithConfig snap = config();
		assertEquals(List.of(), keys(snap, ENCHANTED, false, true, false, false, false),
			"the default keeps hand outlines under a shader pack");
		snap.outline.disableWithShaders = true;
		assertEquals(List.of(PreviewNotes.SHADER_HAND_OFF), keys(snap, ENCHANTED, false, true, false, false, false));
		assertEquals(List.of(), keys(snap, ENCHANTED, false, false, false, false, false), "no pack, no note");
		snap.outline.disableWithShaders = false;
		snap.outline.handWithShaderPack = false;
		assertEquals(List.of(PreviewNotes.SHADER_HAND_OFF), keys(snap, ENCHANTED, false, true, false, false, false));
	}

	@Test
	void shaderPackKeepsTheVanillaGlint() {
		GlintsmithConfig snap = withGlintRule();
		assertEquals(List.of(PreviewNotes.SHADER_GLINT), keys(snap, ENCHANTED, false, true, false, false, false));
		assertEquals(List.of(), keys(snap, ENCHANTED, false, false, false, false, false));
		snap.glint.enabled = false;
		assertEquals(List.of(), keys(snap, ENCHANTED, false, true, false, false, false));
	}

	@Test
	void worldAndHandNotes() {
		assertEquals(List.of(PreviewNotes.NO_WORLD), keys(config(), ENCHANTED, false, false, true, false, false));
		assertEquals(List.of(PreviewNotes.EMPTY_HAND), keys(config(), ENCHANTED, false, false, false, true, false));
	}

	@Test
	void unavailableIsFirst() {
		assertEquals(List.of(PreviewNotes.UNAVAILABLE), keys(config(), ENCHANTED, false, false, false, false, true));
	}

	@Test
	void allNotesAreOrderedByPriority() {
		GlintsmithConfig snap = withGlintRule();
		snap.outline.disableWithShaders = true;
		assertEquals(List.of(PreviewNotes.UNAVAILABLE, PreviewNotes.SHADER_HAND_OFF, PreviewNotes.SHADER_GLINT,
			PreviewNotes.NO_WORLD, PreviewNotes.EMPTY_HAND), keys(snap, ENCHANTED, false, true, true, true, true));

		GlintsmithConfig off = withGlintRule();
		off.outline.enabled = false;
		assertEquals(List.of(PreviewNotes.UNAVAILABLE, PreviewNotes.OUTLINES_OFF, PreviewNotes.SHADER_GLINT,
			PreviewNotes.NO_WORLD, PreviewNotes.EMPTY_HAND), keys(off, ENCHANTED, true, true, true, true, true));
	}

	@Test
	void everyKeyIsATranslationKey() {
		for (String key : List.of(PreviewNotes.UNAVAILABLE, PreviewNotes.OUTLINES_OFF, PreviewNotes.FIRST_PERSON_OFF,
			PreviewNotes.NO_OUTLINE, PreviewNotes.MAP, PreviewNotes.SHADER_HAND_OFF, PreviewNotes.SHADER_GLINT,
			PreviewNotes.NO_WORLD, PreviewNotes.EMPTY_HAND)) {
			assertTrue(key.startsWith("glintsmith.preview.note."), key);
		}
	}
}
