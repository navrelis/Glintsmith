package navrelis.glintsmith.render.glint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.renderer.texture.Stitcher;
import net.minecraft.resources.ResourceLocation;

class GlintAtlasScaleTest {
	@BeforeEach
	@AfterEach
	void reset() {
		GlintAtlasScale.resetForTests();
	}

	@Test
	void factorIsOneBeforeTheFirstUpload() {
		assertEquals(1.0f, GlintAtlasScale.u());
		assertEquals(1.0f, GlintAtlasScale.v());
	}

	@Test
	void vanillaAtlasGivesOne() {
		assertFalse(GlintAtlasScale.update(1024, 512, 16, 16), "same as the initial state");
		assertEquals(1.0f, GlintAtlasScale.u());
		assertEquals(1.0f, GlintAtlasScale.v());
	}

	@Test
	void largeAtlasOfTheOwnersPack() {
		// Nytheria: 8192 x 4096 with 16 px item textures; Scale 0.13 was the manual workaround.
		assertTrue(GlintAtlasScale.update(8192, 4096, 16, 16));
		assertEquals(0.125f, GlintAtlasScale.u());
		assertEquals(0.125f, GlintAtlasScale.v());
	}

	@Test
	void nonSquareRatiosAreKeptPerAxis() {
		GlintAtlasScale.update(8192, 2048, 16, 16);
		assertEquals(0.125f, GlintAtlasScale.u());
		assertEquals(0.25f, GlintAtlasScale.v());
		GlintAtlasScale.update(2048, 2048, 16, 16);
		assertEquals(0.5f, GlintAtlasScale.u());
		assertEquals(0.25f, GlintAtlasScale.v(), "a square atlas is not square relative to the 1024 x 512 reference");
	}

	@Test
	void highResolutionPacksAreNeutral() {
		// A 32x pack doubles the atlas and the sprites: the UV range of an item is the vanilla one.
		GlintAtlasScale.update(2048, 1024, 32, 32);
		assertEquals(1.0f, GlintAtlasScale.u());
		assertEquals(1.0f, GlintAtlasScale.v());
		// The same pack in a large modpack keeps only the atlas part.
		GlintAtlasScale.update(8192, 4096, 32, 32);
		assertEquals(0.25f, GlintAtlasScale.u());
		assertEquals(0.25f, GlintAtlasScale.v());
	}

	@Test
	void missingReferenceSpriteFallsBackToTheAtlasRatio() {
		GlintAtlasScale.update(4096, 2048, 0, 0);
		assertEquals(0.25f, GlintAtlasScale.u());
		assertEquals(0.25f, GlintAtlasScale.v());
	}

	@Test
	void degenerateSizesGiveOne() {
		assertEquals(1.0f, GlintAtlasScale.factor(1024, 0, 16, 16));
		assertEquals(1.0f, GlintAtlasScale.factor(1024, -8, 16, 16));
		assertEquals(1.0f, GlintAtlasScale.factor(0, 1024, 16, 16));
		assertEquals(1.0f, GlintAtlasScale.factor(1024, 1024, 0, 16));
		assertEquals(1.0f, GlintAtlasScale.factor(1024, 1024, 16, -1));
		GlintAtlasScale.update(0, 0, 16, 16);
		assertEquals(1.0f, GlintAtlasScale.u());
		assertEquals(1.0f, GlintAtlasScale.v());
	}

	@Test
	void extremeSizesAreClamped() {
		assertEquals(GlintAtlasScale.MIN_FACTOR, GlintAtlasScale.factor(1024, 1 << 20, 16, 16));
		assertEquals(GlintAtlasScale.MAX_FACTOR, GlintAtlasScale.factor(1024, 1, 16, 16));
		assertEquals(GlintAtlasScale.MAX_FACTOR, GlintAtlasScale.factor(1024, 1024, 16, 1 << 16));
	}

	@Test
	void updateReportsChanges() {
		assertTrue(GlintAtlasScale.update(2048, 1024, 16, 16));
		assertFalse(GlintAtlasScale.update(2048, 1024, 16, 16));
		assertTrue(GlintAtlasScale.update(2048, 2048, 16, 16));
	}

	/**
	 * The reference is the vanilla 1.21.1 block atlas. This stitches exactly what {@code atlases/blocks.json} of the
	 * client jar on the test class path lists, with the vanilla {@code Stitcher} at the default mipmap level 4, and checks
	 * the constants against the result.
	 */
	@Test
	void referenceIsTheStitchedVanillaBlockAtlas() throws IOException {
		URL url = GlintAtlasScaleTest.class.getClassLoader().getResource("assets/minecraft/atlases/blocks.json");
		assertNotNull(url, "vanilla assets are not on the test class path");
		JarURLConnection connection = (JarURLConnection) url.openConnection();
		connection.setUseCaches(false);
		try (JarFile jar = connection.getJarFile()) {
			JsonObject atlas;
			try (InputStream in = jar.getInputStream(jar.getEntry("assets/minecraft/atlases/blocks.json"))) {
				atlas = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
			}
			Map<ResourceLocation, Sprite> sprites = new LinkedHashMap<>();
			for (JsonElement element : atlas.getAsJsonArray("sources")) {
				JsonObject source = element.getAsJsonObject();
				switch (source.get("type").getAsString()) {
					case "directory" -> addDirectory(jar, source.get("source").getAsString(), source.get("prefix").getAsString(), sprites);
					case "single" -> addSingle(jar, source.get("resource").getAsString(), sprites);
					case "paletted_permutations" -> addPermutations(jar, source, sprites);
					default -> throw new AssertionError("unexpected source type " + source.get("type"));
				}
			}
			ResourceLocation missing = ResourceLocation.withDefaultNamespace("missingno");
			sprites.put(missing, new Sprite(16, 16, missing));
			assertTrue(sprites.containsKey(GlintAtlasScale.REFERENCE_SPRITE_ID), "reference sprite is not in the block atlas");
			Sprite reference = sprites.get(GlintAtlasScale.REFERENCE_SPRITE_ID);
			assertEquals(GlintAtlasScale.REFERENCE_SPRITE, reference.width());
			assertEquals(GlintAtlasScale.REFERENCE_SPRITE, reference.height());

			Stitcher<Sprite> stitcher = new Stitcher<>(16384, 16384, 4);
			sprites.values().forEach(stitcher::registerSprite);
			stitcher.stitch();
			assertEquals(GlintAtlasScale.REFERENCE_WIDTH, stitcher.getWidth(), sprites.size() + " sprites");
			assertEquals(GlintAtlasScale.REFERENCE_HEIGHT, stitcher.getHeight(), sprites.size() + " sprites");
		}
	}

	private record Sprite(int width, int height, ResourceLocation name) implements Stitcher.Entry {
	}

	private static void addDirectory(JarFile jar, String folder, String prefix, Map<ResourceLocation, Sprite> sprites) throws IOException {
		String root = "assets/minecraft/textures/" + folder + "/";
		List<String> names = new ArrayList<>();
		for (Enumeration<JarEntry> e = jar.entries(); e.hasMoreElements();) {
			String name = e.nextElement().getName();
			if (name.startsWith(root) && name.endsWith(".png")) {
				names.add(name);
			}
		}
		for (String name : names) {
			String id = prefix + name.substring(root.length(), name.length() - ".png".length());
			ResourceLocation location = ResourceLocation.withDefaultNamespace(id);
			sprites.put(location, read(jar, name, location));
		}
	}

	private static void addSingle(JarFile jar, String resource, Map<ResourceLocation, Sprite> sprites) throws IOException {
		ResourceLocation location = ResourceLocation.parse(resource);
		sprites.put(location, read(jar, "assets/minecraft/textures/" + location.getPath() + ".png", location));
	}

	private static void addPermutations(JarFile jar, JsonObject source, Map<ResourceLocation, Sprite> sprites) throws IOException {
		for (JsonElement texture : source.getAsJsonArray("textures")) {
			ResourceLocation base = ResourceLocation.parse(texture.getAsString());
			Sprite size = read(jar, "assets/minecraft/textures/" + base.getPath() + ".png", base);
			for (String permutation : source.getAsJsonObject("permutations").keySet()) {
				ResourceLocation location = base.withSuffix("_" + permutation);
				sprites.put(location, new Sprite(size.width(), size.height(), location));
			}
		}
	}

	/** The sprite size: the PNG size from its IHDR chunk, or one frame of it for an animation (vanilla's rule). */
	private static Sprite read(JarFile jar, String path, ResourceLocation location) throws IOException {
		JarEntry entry = jar.getJarEntry(path);
		assertNotNull(entry, path);
		byte[] header = new byte[24];
		try (InputStream in = jar.getInputStream(entry)) {
			assertEquals(24, in.readNBytes(header, 0, 24), path);
		}
		int width = readInt(header, 16);
		int height = readInt(header, 20);
		JarEntry meta = jar.getJarEntry(path + ".mcmeta");
		if (meta != null) {
			JsonObject json;
			try (InputStream in = jar.getInputStream(meta)) {
				json = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
			}
			if (json.has("animation")) {
				JsonObject animation = json.getAsJsonObject("animation");
				int frameWidth = animation.has("width") ? animation.get("width").getAsInt() : -1;
				int frameHeight = animation.has("height") ? animation.get("height").getAsInt() : -1;
				if (frameWidth == -1 && frameHeight == -1) {
					int side = Math.min(width, height);
					width = side;
					height = side;
				} else {
					width = frameWidth != -1 ? frameWidth : width;
					height = frameHeight != -1 ? frameHeight : height;
				}
			}
		}
		return new Sprite(width, height, location);
	}

	private static int readInt(byte[] b, int offset) {
		return (b[offset] & 255) << 24 | (b[offset + 1] & 255) << 16 | (b[offset + 2] & 255) << 8 | (b[offset + 3] & 255);
	}
}
