package navrelis.glintsmith.gui;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import navrelis.glintsmith.style.Colors;
import navrelis.glintsmith.style.GlintSettings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GuiHelpersTest {
	@Test
	void textureNames() {
		// empty means vanilla or inherit
		assertTrue(TextureNames.isValid(null));
		assertTrue(TextureNames.isValid(""));
		assertTrue(TextureNames.isValid("   "));
		assertTrue(TextureNames.isValid("vanilla"));
		assertTrue(TextureNames.isValid("default"));
		assertTrue(TextureNames.isValid(" vanilla "));
		for (String preset : GlintSettings.PRESETS) {
			assertTrue(TextureNames.isValid("preset:" + preset), preset);
		}
		assertFalse(TextureNames.isValid("preset:nope"));
		assertFalse(TextureNames.isValid("preset:"));
		assertTrue(TextureNames.isValid("file:my_glint-1.png"));
		assertFalse(TextureNames.isValid("file:My Glint.png"));
		assertFalse(TextureNames.isValid("file:Fire.png"));
		assertFalse(TextureNames.isValid("file:fire.jpg"));
		assertFalse(TextureNames.isValid("file:../fire.png"));
		assertFalse(TextureNames.isValid("file:sub/fire.png"));
		assertFalse(TextureNames.isValid("file:"));
		// a resource location needs a folder in its path; "config:fire.png" is no special form and is rejected
		assertFalse(TextureNames.isValid("config:fire.png"));
		assertFalse(TextureNames.isValid("glintsmith:rainbow"));
		assertTrue(TextureNames.isValid("mymod:textures/glint/x.png"));
		assertTrue(TextureNames.isValid("minecraft:textures/misc/enchanted_glint_item.png"));
		assertFalse(TextureNames.isValid("Glint"));
		assertFalse(TextureNames.isValid("rainbow"));
		assertFalse(TextureNames.isValid("a:b:c"));
		assertFalse(TextureNames.isValid("name with space"));
	}

	@Test
	void defaultTextureSpecsListVanillaPresetsAndFiles(@TempDir Path dir) throws IOException {
		Files.writeString(dir.resolve("zeta.png"), "x");
		Files.writeString(dir.resolve("alpha.png"), "x");
		Files.writeString(dir.resolve("my_glint-2.png"), "x");
		Files.writeString(dir.resolve("Upper.png"), "x");
		Files.writeString(dir.resolve("with space.png"), "x");
		Files.writeString(dir.resolve("notes.txt"), "x");
		Files.writeString(dir.resolve("noext"), "x");
		Files.createDirectory(dir.resolve("folder.png"));

		List<String> specs = GuiHooks.defaultTextureSpecs(dir);

		List<String> expected = new ArrayList<>();
		expected.add("vanilla");
		for (String preset : GlintSettings.PRESETS) {
			expected.add("preset:" + preset);
		}
		expected.addAll(List.of("file:alpha.png", "file:my_glint-2.png", "file:zeta.png"));
		assertEquals(expected, specs);
		for (String spec : specs) {
			assertTrue(TextureNames.isValid(spec), spec);
		}
	}

	@Test
	void defaultTextureSpecsWithoutFolder(@TempDir Path dir) {
		List<String> expected = new ArrayList<>();
		expected.add("vanilla");
		for (String preset : GlintSettings.PRESETS) {
			expected.add("preset:" + preset);
		}
		assertEquals(expected, GuiHooks.defaultTextureSpecs(dir.resolve("missing")));
		assertEquals(expected, GuiHooks.defaultTextureSpecs(null));
		assertEquals(expected, GuiHooks.defaultTextureSpecs(dir));
	}

	@Test
	void patternTextSplitsAndJoins() {
		assertEquals(List.of("minecraft:diamond_sword", "#minecraft:swords", "@mod"),
			PatternText.split(" minecraft:diamond_sword , #minecraft:swords,, @mod ,"));
		assertEquals(List.of(), PatternText.split(""));
		assertEquals("a, b", PatternText.join(List.of("a", "b")));
		assertEquals("", PatternText.join(null));
	}

	@Test
	void patternTextFindsInvalidEntries() {
		assertTrue(PatternText.allValid(""));
		assertTrue(PatternText.allValid("minecraft:diamond_sword, #minecraft:swords, @mod, *_sword, !minecraft:stick"));

		String text = "minecraft:stick, Bad Entry!, @mod";
		List<int[]> bad = PatternText.invalidRanges(text);
		assertEquals(1, bad.size());
		assertEquals("Bad Entry!", text.substring(bad.get(0)[0], bad.get(0)[1]));
		assertFalse(PatternText.allValid(text));

		List<int[]> two = PatternText.invalidRanges("a b,minecraft:ok,c d");
		assertEquals(2, two.size());
	}

	@Test
	void hsvRoundTrip() {
		for (int rgb : new int[] {0xD088FC, 0xFF3B3B, 0x17C964, 0x123456, 0xFFFFFF, 0x000000, 0x808080}) {
			int argb = 0xFF000000 | rgb;
			float[] hsv = Hsv.fromArgb(argb, 0.25f);
			int back = Colors.hsv(hsv[0], hsv[1], hsv[2]);
			assertEquals(argb >> 16 & 0xFF, back >> 16 & 0xFF, 1, Integer.toHexString(rgb));
			assertEquals(argb >> 8 & 0xFF, back >> 8 & 0xFF, 1, Integer.toHexString(rgb));
			assertEquals(argb & 0xFF, back & 0xFF, 1, Integer.toHexString(rgb));
		}
	}

	@Test
	void hsvKeepsTheFallbackHueForGrey() {
		float[] grey = Hsv.fromArgb(0xFF808080, 0.4f);
		assertEquals(0.4f, grey[0], 1.0e-6f);
		assertEquals(0.0f, grey[1], 1.0e-6f);
		float[] red = Hsv.fromArgb(0xFFFF0000, 0.4f);
		assertArrayEquals(new float[] {0.0f, 1.0f, 1.0f}, red, 1.0e-6f);
	}
}
