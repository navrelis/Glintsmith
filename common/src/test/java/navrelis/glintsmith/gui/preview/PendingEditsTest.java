package navrelis.glintsmith.gui.preview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import navrelis.glintsmith.config.ConfigManager;
import navrelis.glintsmith.config.GlintsmithConfig;
import navrelis.glintsmith.style.ColorStyle;
import navrelis.glintsmith.style.OutlineSettings;
import navrelis.glintsmith.style.Rule;
import org.junit.jupiter.api.Test;

class PendingEditsTest {
	private final Rule first = rule("first");
	private final Rule second = rule("second");
	private final Rule third = rule("third");
	private final GlintsmithConfig cfg = config();
	private final List<Rule> originalList = cfg.rules;
	private final String baseline = ConfigManager.toJson(cfg);

	private GlintsmithConfig config() {
		GlintsmithConfig c = new GlintsmithConfig();
		c.rules = new ArrayList<>(List.of(first, second, third));
		return c.sanitize();
	}

	private static Rule rule(String name) {
		Rule r = new Rule();
		r.name = name;
		return r;
	}

	private void assertRestored() {
		assertSame(originalList, cfg.rules, "the old list object is back");
		assertEquals(List.of(first, second, third), cfg.rules);
		assertEquals(baseline, ConfigManager.toJson(cfg));
	}

	@Test
	void replacesTheOriginalAtItsIndex() {
		Rule edited = rule("edited");
		boolean[] ran = {false};
		PendingEdits.withRule(cfg, second, edited, () -> {
			ran[0] = true;
			assertEquals(3, cfg.rules.size());
			assertSame(first, cfg.rules.get(0));
			assertSame(edited, cfg.rules.get(1));
			assertSame(third, cfg.rules.get(2));
			assertNotSame(originalList, cfg.rules);
			assertTrue(ConfigManager.toJson(cfg).contains("edited"));
		});
		assertTrue(ran[0]);
		assertRestored();
	}

	@Test
	void appendsARuleThatIsNotInTheList() {
		Rule edited = rule("added");
		PendingEdits.withRule(cfg, rule("elsewhere"), edited, () -> {
			assertEquals(4, cfg.rules.size());
			assertSame(edited, cfg.rules.get(3));
		});
		assertRestored();
		PendingEdits.withRule(cfg, null, edited, () -> assertSame(edited, cfg.rules.get(3)));
		assertRestored();
	}

	@Test
	void theOriginalIsFoundByIdentityNotByContent() {
		Rule twin = rule("second");
		PendingEdits.withRule(cfg, twin, rule("edited"), () -> assertEquals(4, cfg.rules.size()));
		assertRestored();
	}

	@Test
	void restoresWhenTheActionThrows() {
		assertThrows(IllegalStateException.class, () -> PendingEdits.withRule(cfg, first, rule("edited"), () -> {
			throw new IllegalStateException("boom");
		}));
		assertRestored();
	}

	@Test
	void withValueSetsAndRestores() {
		Rule edited = rule("edited");
		edited.outline = OutlineSettings.enabled(true);
		edited.outline.color = ColorStyle.of("#112233");
		ColorStyle before = edited.outline.color;
		ColorStyle pending = ColorStyle.of("#445566");
		PendingEdits.withValue(() -> edited.outline.color, c -> edited.outline.color = c, pending,
			() -> assertSame(pending, edited.outline.color));
		assertSame(before, edited.outline.color);
		assertThrows(IllegalStateException.class,
			() -> PendingEdits.withValue(() -> edited.outline.color, c -> edited.outline.color = c, pending, () -> {
				throw new IllegalStateException("boom");
			}));
		assertSame(before, edited.outline.color);
	}

	@Test
	void nestedEditsAreUndoneInnermostFirst() {
		Rule edited = rule("edited");
		edited.outline = OutlineSettings.enabled(true);
		edited.outline.color = ColorStyle.of("#112233");
		ColorStyle pending = ColorStyle.of("#AABBCC");
		List<String> order = new ArrayList<>();

		assertThrows(IllegalStateException.class, () -> PendingEdits.withRule(cfg, second, edited, () -> {
			order.add("rule in");
			assertTrue(ConfigManager.toJson(cfg).contains("#112233"));
			PendingEdits.withValue(() -> edited.outline.color, c -> {
				order.add(c == pending ? "set pending" : "restore value");
				edited.outline.color = c;
			}, pending, () -> {
				String json = ConfigManager.toJson(cfg);
				assertTrue(json.contains("#AABBCC"), "the colour edit is visible inside");
				assertFalse(json.contains("#112233"));
				order.add("action");
				throw new IllegalStateException("boom");
			});
		}));

		assertEquals(List.of("rule in", "set pending", "action", "restore value"), order);
		assertRestored();
		assertEquals("#112233", edited.outline.color.colors.get(0));
	}
}
