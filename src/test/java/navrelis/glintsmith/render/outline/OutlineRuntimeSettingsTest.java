package navrelis.glintsmith.render.outline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import navrelis.glintsmith.config.GlintsmithConfig;
import navrelis.glintsmith.style.OutlineTarget;
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
		o.scaleWithResolution = false;
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
		assertFalse(s.scaleWithResolution());
		assertEquals(10.0f, s.fadeStart());
		assertEquals(20.0f, s.fadeEnd());
		assertTrue(s.fadeActive());
	}

	@Test
	void defaultsMatchTheConfigDefaults() {
		OutlineRuntimeSettings s = OutlineRuntimeSettings.from(config());
		assertTrue(s.enabled());
		assertTrue(s.firstPerson());
		assertFalse(s.gui());
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
	void shaderPackRules() {
		GlintsmithConfig config = config();
		config.outline.targets.gui = true;
		OutlineRuntimeSettings s = OutlineRuntimeSettings.from(config);
		assertTrue(s.worldPhaseAllowed(false));
		assertFalse(s.worldPhaseAllowed(true), "world outlines are off under a shader pack");
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
		assertFalse(none.handPhaseAllowed(true));
		assertFalse(none.guiPhaseAllowed(true));
		assertTrue(none.worldPhaseAllowed(false), "only applies while a pack is active");
		assertTrue(none.handPhaseAllowed(false));
		assertTrue(none.guiPhaseAllowed(false));
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
		assertEquals(1.0f, s.thicknessScale(OutlinePhase.WORLD, 1080, 3.0));
		assertEquals(2.0f, s.thicknessScale(OutlinePhase.WORLD, 2160, 3.0));
		assertEquals(2.0f, s.thicknessScale(OutlinePhase.HAND, 2160, 3.0));
		assertEquals(1.5f, s.thicknessScale(OutlinePhase.GUI, 2160, 3.0));
		assertEquals(0.5f, s.thicknessScale(OutlinePhase.GUI, 2160, 1.0));
		assertEquals(1.0f, s.thicknessScale(OutlinePhase.WORLD, 0, 3.0), "minimised window");
		assertEquals(1.0f, s.thicknessScale(OutlinePhase.GUI, 1080, 0.0));

		config.outline.scaleWithResolution = false;
		OutlineRuntimeSettings fixed = OutlineRuntimeSettings.from(config);
		assertEquals(1.0f, fixed.thicknessScale(OutlinePhase.WORLD, 2160, 3.0));
		assertEquals(1.0f, fixed.thicknessScale(OutlinePhase.GUI, 2160, 4.0));
	}

	@Test
	void scaledThicknessIsStillCappedByTheStyleTable() {
		GlintsmithConfig config = config();
		config.outline.scaleWithResolution = true;
		OutlineRuntimeSettings s = OutlineRuntimeSettings.from(config);
		float scaled = 16.0f * s.thicknessScale(OutlinePhase.WORLD, 4320, 1.0); // 8K: x4
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
