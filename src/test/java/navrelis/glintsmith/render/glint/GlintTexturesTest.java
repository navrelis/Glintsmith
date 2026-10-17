package navrelis.glintsmith.render.glint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import navrelis.glintsmith.config.GlintsmithConfig;
import navrelis.glintsmith.style.GlintSettings;
import navrelis.glintsmith.style.Rule;
import net.minecraft.resources.ResourceLocation;

class GlintTexturesTest {
	private static final byte[] PNG_HEAD = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};

	private static final List<String> BASE = List.of(
		"vanilla", "preset:neutral", "preset:rainbow", "preset:aurora", "preset:fire", "preset:frost",
		"preset:sparkle", "preset:gold", "preset:void");

	private static void png(Path dir, String name) throws IOException {
		Files.write(dir.resolve(name), PNG_HEAD);
	}

	@Test
	void baseSpecsAreExactlyTheDocumentedOnes() {
		assertEquals(BASE, GlintTextures.BASE_SPECS);
		List<String> presets = new ArrayList<>();
		for (String spec : BASE.subList(1, BASE.size())) {
			presets.add(spec.substring("preset:".length()));
		}
		assertEquals(GlintSettings.PRESETS, presets, "must follow the T3 preset list and order");
		for (String spec : BASE) {
			assertEquals(null, GlintSettings.validateTexture(spec), spec + " must be a valid spec");
		}
	}

	@Test
	void presetSpecsOfAnEmptyOrMissingFolderAreTheBaseList(@TempDir Path dir) {
		assertEquals(BASE, GlintTextures.presetSpecs(dir));
		assertEquals(BASE, GlintTextures.presetSpecs(dir.resolve("missing")));
		assertEquals(BASE, GlintTextures.presetSpecs((Path) null));
	}

	@Test
	void presetSpecsAppendSortedValidPngFiles(@TempDir Path dir) throws IOException {
		png(dir, "zeta.png");
		png(dir, "alpha.png");
		png(dir, "my_glint-2.v1.png");
		png(dir, "Upper.png"); // not a valid name in the shared grammar
		png(dir, "space name.png");
		Files.write(dir.resolve("fake.png"), new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9}); // not a PNG
		Files.write(dir.resolve("empty.png"), new byte[0]);
		Files.write(dir.resolve("notes.txt"), PNG_HEAD); // wrong extension
		Files.createDirectory(dir.resolve("folder.png"));

		List<String> specs = GlintTextures.presetSpecs(dir);
		assertEquals(BASE, specs.subList(0, BASE.size()));
		assertEquals(List.of("file:alpha.png", "file:my_glint-2.v1.png", "file:zeta.png"), specs.subList(BASE.size(), specs.size()));
		for (String spec : specs) {
			assertEquals(null, GlintSettings.validateTexture(spec), spec);
		}
	}

	@Test
	void publicPresetSpecsStartWithTheBaseList() {
		List<String> specs = GlintTextures.presetSpecs();
		assertTrue(specs.size() >= BASE.size());
		assertEquals(BASE, specs.subList(0, BASE.size()));
	}

	@Test
	void sanitizeName() {
		assertEquals("my_glint-2.png", GlintTextures.sanitizeName("my_glint-2.png"));
		assertEquals("upper.png", GlintTextures.sanitizeName("Upper.PNG"));
		assertEquals("a_b_c.png", GlintTextures.sanitizeName("a b/c.png"));
		assertEquals("___", GlintTextures.sanitizeName("äöü"));
	}

	@Test
	void generatedIdsAreValidResourceLocations() {
		assertEquals("glintsmith:glint/preset/gold", GlintTextures.presetId("gold").toString());
		assertEquals("glintsmith:glint/preset/neutral_item", GlintTextures.NEUTRAL_ITEM.toString());
		assertEquals("glintsmith:glint/preset/neutral_entity", GlintTextures.NEUTRAL_ENTITY.toString());
		ResourceLocation file = GlintTextures.fileId("My Glint.PNG");
		assertEquals("glintsmith:glint/file/my_glint.png", file.toString());
		assertEquals("minecraft:textures/misc/enchanted_glint_item.png", GlintTextures.VANILLA_ITEM.toString());
		assertEquals("minecraft:textures/misc/enchanted_glint_entity.png", GlintTextures.VANILLA_ENTITY.toString());
	}

	@Test
	void vanillaSpecsResolveWithoutAnyGameState() {
		GlintTextures.clearStateForTests();
		for (String spec : new String[] {null, "vanilla", "default"}) {
			assertEquals(GlintTextures.VANILLA_ITEM, GlintTextures.resolve(spec, false, false), String.valueOf(spec));
			assertEquals(GlintTextures.VANILLA_ENTITY, GlintTextures.resolve(spec, true, false), String.valueOf(spec));
			// the neutral textures are not built yet, so a tint falls back to the vanilla texture
			assertEquals(GlintTextures.VANILLA_ITEM, GlintTextures.resolve(spec, false, true));
			assertEquals(GlintTextures.VANILLA_ENTITY, GlintTextures.resolve(spec, true, true));
		}
	}

	@Test
	void unavailablePresetsFallBackToVanilla() {
		GlintTextures.clearStateForTests();
		assertEquals(GlintTextures.VANILLA_ITEM, GlintTextures.resolve("preset:rainbow", false, false));
		assertEquals(GlintTextures.VANILLA_ENTITY, GlintTextures.resolve("preset:neutral", true, false));
		assertEquals(GlintTextures.VANILLA_ITEM, GlintTextures.resolve("preset:no_such_preset", false, false));
		assertEquals(GlintTextures.VANILLA_ITEM, GlintTextures.resolve("not a spec!", false, false));
		GlintTextures.clearStateForTests();
	}

	@Test
	void referencedFilesComeFromCategoriesAndActiveRules() {
		GlintsmithConfig config = new GlintsmithConfig().sanitize();
		config.glint.item.texture = "file:a.png";
		config.glint.armor.texture = "preset:fire";
		config.glint.gui.texture = "file:b.png";

		Rule active = new Rule();
		active.glint = new GlintSettings();
		active.glint.texture = "file:c.png";
		Rule disabled = new Rule();
		disabled.enabled = false;
		disabled.glint = new GlintSettings();
		disabled.glint.texture = "file:d.png";
		Rule broken = new Rule();
		broken.items.add("minecraft:[broken");
		broken.glint = new GlintSettings();
		broken.glint.texture = "file:e.png";
		Rule noGlint = new Rule();
		config.rules.add(active);
		config.rules.add(disabled);
		config.rules.add(broken);
		config.rules.add(noGlint);
		config.sanitize();

		Set<String> files = GlintTextures.referencedFiles(config);
		assertEquals(Set.of("a.png", "b.png", "c.png"), files);
		assertEquals(Set.of(), GlintTextures.referencedFiles(null));
	}
}
