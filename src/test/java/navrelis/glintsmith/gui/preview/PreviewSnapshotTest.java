package navrelis.glintsmith.gui.preview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import navrelis.glintsmith.config.GlintsmithConfig;
import navrelis.glintsmith.style.OutlineSettings;
import navrelis.glintsmith.style.Rule;
import org.junit.jupiter.api.Test;

class PreviewSnapshotTest {
	private static GlintsmithConfig config() {
		return new GlintsmithConfig().sanitize();
	}

	@Test
	void sameJsonGivesTheSameInstance() {
		PreviewSnapshot snapshot = new PreviewSnapshot();
		GlintsmithConfig live = config();
		GlintsmithConfig first = snapshot.update(live);
		assertSame(first, snapshot.update(live));
		assertSame(first, snapshot.update(live.copy()), "equal content in another object");
	}

	@Test
	void aChangedValueGivesANewInstance() {
		PreviewSnapshot snapshot = new PreviewSnapshot();
		GlintsmithConfig live = config();
		live.outline.thickness = 4.0f;
		GlintsmithConfig before = snapshot.update(live);
		live.outline.thickness = 7.0f;
		GlintsmithConfig after = snapshot.update(live);
		assertNotSame(before, after);
		assertEquals(7.0f, after.outline.thickness);
		assertEquals(4.0f, before.outline.thickness);
		assertSame(after, snapshot.update(live));
	}

	@Test
	void theSnapshotIsDetachedFromTheLiveConfig() {
		PreviewSnapshot snapshot = new PreviewSnapshot();
		GlintsmithConfig live = config();
		Rule rule = new Rule();
		rule.name = "mine";
		live.rules.add(rule);
		GlintsmithConfig copy = snapshot.update(live);
		assertNotSame(live, copy);
		assertNotSame(live.rules, copy.rules);
		assertNotSame(live.outline, copy.outline);
		assertNotSame(rule, copy.rules.get(copy.rules.size() - 1));

		live.outline.opacity = 0.5f;
		rule.name = "changed";
		rule.outline = OutlineSettings.enabled(false);
		assertEquals(0.25f, copy.outline.opacity);
		Rule copied = copy.rules.get(copy.rules.size() - 1);
		assertEquals("mine", copied.name);
		assertEquals(null, copied.outline);
	}

	@Test
	void theSnapshotIsSanitised() {
		PreviewSnapshot snapshot = new PreviewSnapshot();
		GlintsmithConfig live = config();
		live.outline.thickness = 99.0f;
		live.outline.opacity = 5.0f;
		live.durabilityAlert.color = "not a colour";
		live.outline.color = null;
		Rule broken = new Rule();
		broken.items.add("bad$char");
		live.rules.add(broken);

		GlintsmithConfig copy = snapshot.update(live);
		assertEquals(OutlineSettings.MAX_THICKNESS, copy.outline.thickness);
		assertEquals(1.0f, copy.outline.opacity);
		assertEquals("#FF2A2A", copy.durabilityAlert.color);
		assertNotNull(copy.outline.color);
		Rule sanitised = copy.rules.get(copy.rules.size() - 1);
		assertNotNull(sanitised.error, "the patterns are compiled and the broken one is marked");
	}

	@Test
	void aMissingSectionIsFilledIn() {
		PreviewSnapshot snapshot = new PreviewSnapshot();
		GlintsmithConfig live = config();
		live.outline = null;
		live.rules = null;
		GlintsmithConfig copy = snapshot.update(live);
		assertNotNull(copy.outline);
		assertNotNull(copy.rules);
	}
}
