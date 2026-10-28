package navrelis.glintsmith.render.outline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import navrelis.glintsmith.config.GlintsmithConfig;
import navrelis.glintsmith.style.OutlineTarget;
import net.minecraft.world.item.ItemDisplayContext;
import org.junit.jupiter.api.Test;

class OutlineRuntimeSettingsTest {
	private static GlintsmithConfig config() {
		return new GlintsmithConfig().sanitize();
	}

	@Test
	void missingConfigIsDisabled() {
		assertSame(OutlineRuntimeSettings.DISABLED, OutlineRuntimeSettings.from(null));
		GlintsmithConfig config = config();
		config.outline = null;
		assertSame(OutlineRuntimeSettings.DISABLED, OutlineRuntimeSettings.from(config));
		assertFalse(OutlineRuntimeSettings.DISABLED.accepts(OutlinePhase.WORLD, OutlineTarget.DROPPED));
	}

	@Test
	void readsEveryField() {
		GlintsmithConfig config = config();
		GlintsmithConfig.Outline o = config.outline;
		o.targets.firstPerson = false;
		o.targets.thirdPerson = true;
		o.targets.armor = false;
		o.targets.dropped = true;
		o.targets.itemFrame = false;
		o.targets.gui = true;
		o.targets.other = true;
		o.onlyLocalPlayer = true;
		o.disableWithShaders = true;
		o.handWithShaderPack = false;
		o.worldWithShaderPack = false;
		o.scaleWithResolution = false;
		o.proportional = false;
		o.minThickness = 2.5f;
		o.fadeStart = 10.0f;
		o.fadeEnd = 20.0f;
		OutlineRuntimeSettings s = OutlineRuntimeSettings.from(config);
		assertTrue(s.enabled());
		assertFalse(s.firstPerson());
		assertTrue(s.thirdPerson());
		assertFalse(s.armor());
		assertTrue(s.dropped());
		assertFalse(s.itemFrame());
		assertTrue(s.gui());
		assertTrue(s.other());
		assertTrue(s.onlyLocalPlayer());
		assertTrue(s.disableWithShaders());
		assertFalse(s.handWithShaderPack());
		assertFalse(s.worldWithShaderPack());
		assertTrue(OutlineRuntimeSettings.from(config()).worldWithShaderPack(), "on by default");
		assertFalse(OutlineRuntimeSettings.DISABLED.worldWithShaderPack());
		assertFalse(s.scaleWithResolution());
		assertFalse(s.proportional());
		assertEquals(2.5f, s.minThickness());
		assertTrue(OutlineRuntimeSettings.from(config()).proportional(), "on by default");
		assertFalse(OutlineRuntimeSettings.DISABLED.proportional());
		assertEquals(10.0f, s.fadeStart());
		assertEquals(20.0f, s.fadeEnd());
		assertTrue(s.fadeActive());
	}

	@Test
	void defaultsMatchTheConfigDefaults() {
		OutlineRuntimeSettings s = OutlineRuntimeSettings.from(config());
		assertTrue(s.enabled());
		assertTrue(s.firstPerson());
		assertTrue(s.armor());
		assertTrue(s.gui());
		assertFalse(s.other(), "Targets.other defaults to off");
		assertFalse(s.fadeActive());
		assertTrue(s.anyWorld());
	}

	@Test
	void acceptsRespectsPhaseTargetAndMasterSwitch() {
		GlintsmithConfig config = config();
		config.outline.targets.gui = true;
		OutlineRuntimeSettings s = OutlineRuntimeSettings.from(config);
		assertTrue(s.accepts(OutlinePhase.WORLD, OutlineTarget.DROPPED));
		assertTrue(s.accepts(OutlinePhase.HAND, OutlineTarget.FIRST_PERSON));
		assertTrue(s.accepts(OutlinePhase.GUI, OutlineTarget.GUI));
		assertFalse(s.accepts(OutlinePhase.WORLD, OutlineTarget.GUI), "wrong phase");
		assertFalse(s.accepts(OutlinePhase.WORLD, OutlineTarget.OTHER), "other is off by default");
		assertFalse(s.accepts(OutlinePhase.NONE, OutlineTarget.DROPPED));

		config.outline.targets.dropped = false;
		assertFalse(OutlineRuntimeSettings.from(config).accepts(OutlinePhase.WORLD, OutlineTarget.DROPPED));

		config.outline.enabled = false;
		OutlineRuntimeSettings off = OutlineRuntimeSettings.from(config);
		assertFalse(off.accepts(OutlinePhase.HAND, OutlineTarget.FIRST_PERSON));
		assertFalse(off.worldPhaseAllowed(false));
		assertFalse(off.handPhaseAllowed(false));
		assertFalse(off.guiPhaseAllowed(false));
	}

	@Test
	void onlyLocalPlayerCoversHeldItemsAndArmor() {
		GlintsmithConfig config = config();
		config.outline.targets.other = true;
		config.outline.targets.armor = true;
		config.outline.onlyLocalPlayer = true;
		OutlineRuntimeSettings s = OutlineRuntimeSettings.from(config);
		assertFalse(s.accepts(OutlinePhase.WORLD, OutlineTarget.THIRD_PERSON, false));
		assertTrue(s.accepts(OutlinePhase.WORLD, OutlineTarget.THIRD_PERSON, true));
		assertFalse(s.accepts(OutlinePhase.WORLD, OutlineTarget.ARMOR, false));
		assertTrue(s.accepts(OutlinePhase.WORLD, OutlineTarget.ARMOR, true));
		// Not held or worn: unaffected.
		assertTrue(s.accepts(OutlinePhase.WORLD, OutlineTarget.DROPPED, false));
		assertTrue(s.accepts(OutlinePhase.WORLD, OutlineTarget.ITEM_FRAME, false));
		assertTrue(s.accepts(OutlinePhase.WORLD, OutlineTarget.OTHER, false));
		assertTrue(s.accepts(OutlinePhase.HAND, OutlineTarget.FIRST_PERSON, false));

		config.outline.onlyLocalPlayer = false;
		OutlineRuntimeSettings everyone = OutlineRuntimeSettings.from(config);
		assertTrue(everyone.accepts(OutlinePhase.WORLD, OutlineTarget.THIRD_PERSON, false));
		assertTrue(everyone.accepts(OutlinePhase.WORLD, OutlineTarget.ARMOR, false));
	}

	@Test
	void aThirdPersonContextInTheHandPhaseFollowsTheFirstPersonSwitch() {
		// Punchy-style render: held item drawn inside renderHandsWithItems with THIRD_PERSON_RIGHT_HAND.
		OutlineTarget target = OutlineContexts.forPhase(OutlinePhase.HAND, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND);
		GlintsmithConfig config = config();
		config.outline.targets.thirdPerson = false;
		config.outline.onlyLocalPlayer = true;
		OutlineRuntimeSettings s = OutlineRuntimeSettings.from(config);
		assertTrue(s.handPhaseAllowed(false));
		assertTrue(s.accepts(OutlinePhase.HAND, target, true));
		assertTrue(s.accepts(OutlinePhase.HAND, target, false), "the first-person view is never holder-restricted");

		config.outline.targets.thirdPerson = true;
		config.outline.targets.firstPerson = false;
		OutlineRuntimeSettings noFirstPerson = OutlineRuntimeSettings.from(config);
		assertFalse(noFirstPerson.handPhaseAllowed(false));
		assertFalse(noFirstPerson.accepts(OutlinePhase.HAND, target, true));
	}

	@Test
	void shaderPackRules() {
		GlintsmithConfig config = config();
		config.outline.targets.gui = true;
		OutlineRuntimeSettings s = OutlineRuntimeSettings.from(config);
		assertTrue(s.worldPhaseAllowed(false));
		assertFalse(s.worldPhaseAllowed(true), "without Iris's depth world outlines are off under a shader pack");
		assertTrue(s.worldPhaseAllowed(true, true), "with Iris's depth they stay on by default");
		assertTrue(s.handPhaseAllowed(true), "hand outlines stay on by default");
		assertTrue(s.guiPhaseAllowed(true), "GUI outlines work under a shader pack");

		config.outline.handWithShaderPack = false;
		OutlineRuntimeSettings noHand = OutlineRuntimeSettings.from(config);
		assertFalse(noHand.handPhaseAllowed(true));
		assertTrue(noHand.handPhaseAllowed(false));

		config.outline.handWithShaderPack = true;
		config.outline.disableWithShaders = true;
		OutlineRuntimeSettings none = OutlineRuntimeSettings.from(config);
		assertFalse(none.worldPhaseAllowed(true));
		assertFalse(none.worldPhaseAllowed(true, true), "disableWithShaders wins over the depth");
		assertFalse(none.handPhaseAllowed(true));
		assertFalse(none.guiPhaseAllowed(true));
		assertTrue(none.worldPhaseAllowed(false), "only applies while a pack is active");
		assertTrue(none.worldPhaseAllowed(false, false));
		assertTrue(none.handPhaseAllowed(false));
		assertTrue(none.guiPhaseAllowed(false));
	}

	@Test
	void worldWithShaderPackGatesOnlyThePackCase() {
		GlintsmithConfig config = config();
		config.outline.worldWithShaderPack = false;
		OutlineRuntimeSettings s = OutlineRuntimeSettings.from(config);
		assertFalse(s.worldPhaseAllowed(true, true), "switched off: no world outlines under a pack");
		assertTrue(s.worldPhaseAllowed(false, false), "without a pack the switch does not matter");
		assertTrue(s.worldPhaseAllowed(false, true));
		assertTrue(s.handPhaseAllowed(true), "the hand has its own switch");

		config.outline.worldWithShaderPack = true;
		OutlineRuntimeSettings on = OutlineRuntimeSettings.from(config);
		assertTrue(on.worldPhaseAllowed(true, true));
		assertFalse(on.worldPhaseAllowed(true, false), "Iris's depth missing: off, as before the switch existed");

		config.outline.enabled = false;
		assertFalse(OutlineRuntimeSettings.from(config).worldPhaseAllowed(true, true));
		assertFalse(OutlineRuntimeSettings.from(config).worldPhaseAllowed(false, true));
		config.outline.enabled = true;
		GlintsmithConfig.Targets t = config.outline.targets;
		t.thirdPerson = false;
		t.armor = false;
		t.dropped = false;
		t.itemFrame = false;
		t.other = false;
		assertFalse(OutlineRuntimeSettings.from(config).worldPhaseAllowed(true, true), "no world target");
	}

	@Test
	void shaderPackNoticeTellsWhichOutlinesStayOn() {
		GlintsmithConfig config = config();
		OutlineRuntimeSettings s = OutlineRuntimeSettings.from(config);
		assertEquals("[Glintsmith] A shader pack is active: world outlines use Iris's depth and are drawn after the pack's "
			+ "final pass, first-person outlines stay on, GUI outlines stay on.", s.shaderPackNotice(true));
		assertEquals("[Glintsmith] A shader pack is active: world outlines are off (Iris's depth texture is not available), "
			+ "first-person outlines stay on, GUI outlines stay on.", s.shaderPackNotice(false));

		config.outline.worldWithShaderPack = false;
		config.outline.handWithShaderPack = false;
		config.outline.targets.gui = false;
		assertEquals("[Glintsmith] A shader pack is active: world outlines are off (worldWithShaderPack), first-person "
			+ "outlines are off (handWithShaderPack).", OutlineRuntimeSettings.from(config).shaderPackNotice(true));

		config.outline.disableWithShaders = true;
		assertEquals("[Glintsmith] A shader pack is active: outlines are off (disableWithShaders).",
			OutlineRuntimeSettings.from(config).shaderPackNotice(true));
		assertEquals(null, OutlineRuntimeSettings.DISABLED.shaderPackNotice(true), "outlines off: nothing to say");
	}

	@Test
	void worldPhaseNeedsAWorldTarget() {
		GlintsmithConfig config = config();
		GlintsmithConfig.Targets t = config.outline.targets;
		t.thirdPerson = false;
		t.armor = false;
		t.dropped = false;
		t.itemFrame = false;
		t.other = false;
		assertFalse(OutlineRuntimeSettings.from(config).worldPhaseAllowed(false));
		t.other = true;
		assertTrue(OutlineRuntimeSettings.from(config).worldPhaseAllowed(false));
	}

	@Test
	void thicknessScaling() {
		GlintsmithConfig config = config();
		config.outline.scaleWithResolution = true;
		OutlineRuntimeSettings s = OutlineRuntimeSettings.from(config);
		assertEquals(1.0f, s.thicknessScale(1080));
		assertEquals(2.0f, s.thicknessScale(2160));
		assertEquals(1.0f, s.thicknessScale(0), "minimised window");

		config.outline.scaleWithResolution = false;
		OutlineRuntimeSettings fixed = OutlineRuntimeSettings.from(config);
		assertEquals(1.0f, fixed.thicknessScale(2160));
	}

	@Test
	void scaledThicknessIsStillCappedByTheStyleTable() {
		GlintsmithConfig config = config();
		config.outline.scaleWithResolution = true;
		OutlineRuntimeSettings s = OutlineRuntimeSettings.from(config);
		float scaled = 16.0f * s.thicknessScale(4320); // 8K: x4
		StyleTable table = new StyleTable();
		table.indexFor(0xFFFFFFFF, scaled, 1.0f);
		assertTrue(table.maxRadius() <= 32);
	}

	@Test
	void fadeNeedsEndAfterStart() {
		GlintsmithConfig config = config();
		config.outline.fadeStart = 0.0f;
		config.outline.fadeEnd = 0.0f;
		assertFalse(OutlineRuntimeSettings.from(config).fadeActive());
		config.outline.fadeStart = 0.0f;
		config.outline.fadeEnd = 32.0f;
		assertTrue(OutlineRuntimeSettings.from(config).fadeActive());
		config.outline.fadeStart = -5.0f;
		config.outline.fadeEnd = Float.NaN;
		OutlineRuntimeSettings broken = OutlineRuntimeSettings.from(config);
		assertEquals(0.0f, broken.fadeStart());
		assertEquals(0.0f, broken.fadeEnd());
		assertFalse(broken.fadeActive());
	}
}
