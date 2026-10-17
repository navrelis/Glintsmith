package navrelis.glintsmith.style;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class GlintSettingsTest {
	private static void valid(String spec) {
		assertNull(GlintSettings.validateTexture(spec), spec);
	}

	private static void invalid(String spec) {
		assertNotNull(GlintSettings.validateTexture(spec), spec);
	}

	@Test
	void vanillaSpecsAreValid() {
		valid(null);
		valid("vanilla");
		valid("default");
		assertTrue(GlintSettings.isVanillaTexture(null));
		assertTrue(GlintSettings.isVanillaTexture("vanilla"));
		assertTrue(GlintSettings.isVanillaTexture("default"));
		assertFalse(GlintSettings.isVanillaTexture("preset:gold"));
		assertFalse(GlintSettings.isVanillaTexture(""));
	}

	@Test
	void everyPresetIsValid() {
		assertEquals(List.of("neutral", "rainbow", "aurora", "fire", "frost", "sparkle", "gold", "void"), GlintSettings.PRESETS);
		for (String name : GlintSettings.PRESETS) {
			valid("preset:" + name);
		}
		invalid("preset:nope");
		invalid("preset:");
		invalid("preset:Fire");
	}

	@Test
	void filesMustBePlainLowerCasePngNames() {
		valid("file:glow.png");
		valid("file:my-glint_2.png");
		invalid("file:../x.png");
		invalid("file:a/b.png");
		invalid("file:a\\b.png");
		invalid("file:Upper.png");
		invalid("file:x.jpg");
		invalid("file:");
		invalid("file:.png.x");
		invalid("file:..png");
	}

	@Test
	void otherSpecsMustBeResourceLocations() {
		valid("minecraft:textures/misc/enchanted_glint_item.png");
		valid("mymod:glint");
		invalid("a:b:c");
		invalid("");
		invalid("   ");
		invalid("no_namespace");
		invalid("Mod:Upper");
		invalid("mod:a/../b");
		invalid(":path");
		invalid("mod:");
	}

	@Test
	void errorMessagesNameTheProblem() {
		assertTrue(GlintSettings.validateTexture("preset:nope").contains("nope"));
		assertTrue(GlintSettings.validateTexture("file:../x.png").contains("../x.png"));
		assertTrue(GlintSettings.validateTexture("no_namespace").contains("no_namespace"));
	}
}
