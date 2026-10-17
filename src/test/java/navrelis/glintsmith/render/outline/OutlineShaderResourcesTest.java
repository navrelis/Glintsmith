package navrelis.glintsmith.render.outline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

/**
 * The shader resources as vanilla's {@code ShaderInstance} will look them up: every program named in a JSON resolves
 * through {@link OutlineShaders#remap} to a file that exists, and every sampler and uniform declared in the JSON is
 * used by the GLSL (vanilla warns about declared but missing names). No GL needed.
 */
class OutlineShaderResourcesTest {
	private static final String[] SHADERS = { "mask", "outline_edt", "outline_composite" };

	@Test
	void remapMovesOnlyOurPrefixedCoreShaders() {
		ResourceLocation json = OutlineShaders.remap(ResourceLocation.withDefaultNamespace("shaders/core/glintsmith_mask.json"));
		assertEquals(ResourceLocation.fromNamespaceAndPath("glintsmith", "shaders/core/mask.json"), json);
		assertEquals(ResourceLocation.fromNamespaceAndPath("glintsmith", "shaders/core/fullscreen.vsh"),
			OutlineShaders.remap(ResourceLocation.withDefaultNamespace("shaders/core/glintsmith_fullscreen.vsh")));

		ResourceLocation vanilla = ResourceLocation.withDefaultNamespace("shaders/core/position_tex.json");
		assertSame(vanilla, OutlineShaders.remap(vanilla));
		ResourceLocation include = ResourceLocation.withDefaultNamespace("shaders/include/fog.glsl");
		assertSame(include, OutlineShaders.remap(include));
		ResourceLocation foreign = ResourceLocation.fromNamespaceAndPath("othermod", "shaders/core/glintsmith_mask.json");
		assertSame(foreign, OutlineShaders.remap(foreign));
	}

	@Test
	void everyShaderJsonResolvesToExistingProgramsThatUseItsDeclarations() throws IOException {
		for (String shader : SHADERS) {
			JsonObject json = readJson(resolve(OutlineShaders.PREFIX + shader + ".json"));
			String vertex = read(resolve(json.get("vertex").getAsString() + ".vsh"));
			String fragment = read(resolve(json.get("fragment").getAsString() + ".fsh"));
			assertTrue(vertex.startsWith("#version 150"), shader + ": vertex program must be GLSL 150");
			assertTrue(fragment.startsWith("#version 150"), shader + ": fragment program must be GLSL 150");
			String glsl = vertex + "\n" + fragment;
			for (JsonElement sampler : json.getAsJsonArray("samplers")) {
				String name = sampler.getAsJsonObject().get("name").getAsString();
				assertTrue(glsl.contains("uniform sampler2D " + name + ";"), shader + " does not declare sampler " + name);
			}
			for (JsonElement uniform : json.getAsJsonArray("uniforms")) {
				String name = uniform.getAsJsonObject().get("name").getAsString();
				assertTrue(glsl.matches("(?s).*uniform \\w+ " + name + ";.*"), shader + " does not declare uniform " + name);
			}
		}
	}

	@Test
	void compositeUniformsMatchWhatTheCompositorSets() throws IOException {
		JsonObject json = readJson(resolve(OutlineShaders.PREFIX + "outline_composite.json"));
		StringBuilder names = new StringBuilder();
		for (JsonElement uniform : json.getAsJsonArray("uniforms")) {
			names.append(uniform.getAsJsonObject().get("name").getAsString()).append(' ');
		}
		assertEquals("Radius Region UseDepth NearFar DepthTolerance DistanceFade ", names.toString());
		assertEquals(4, json.getAsJsonArray("samplers").size(), "the compositor saves and restores four texture units");
		assertEquals(GlState.TEXTURE_UNITS, json.getAsJsonArray("samplers").size());
	}

	/** The classpath resource vanilla would load for {@code minecraft:shaders/core/<file>}. */
	private static String resolve(String file) {
		ResourceLocation location = OutlineShaders.remap(ResourceLocation.withDefaultNamespace("shaders/core/" + file));
		return "/assets/" + location.getNamespace() + "/" + location.getPath();
	}

	private static String read(String resource) throws IOException {
		try (InputStream in = OutlineShaderResourcesTest.class.getResourceAsStream(resource)) {
			assertNotNull(in, "missing resource " + resource);
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	private static JsonObject readJson(String resource) throws IOException {
		try (InputStream in = OutlineShaderResourcesTest.class.getResourceAsStream(resource)) {
			assertNotNull(in, "missing resource " + resource);
			return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
		}
	}
}
