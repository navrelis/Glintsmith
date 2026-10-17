package navrelis.glintsmith.modelfix;

import navrelis.glintsmith.mixin.GlintsmithMixinPlugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class GlintsmithMixinPluginTest {
	private static final String PACKAGE = "navrelis.glintsmith.mixin.";

	@Test
	void collidingMixinIsSkippedWithModelfix() {
		for (String mixin : GlintsmithMixinPlugin.MODELFIX_COLLISIONS) {
			assertFalse(GlintsmithMixinPlugin.shouldApply(mixin, true), mixin);
		}
		assertFalse(GlintsmithMixinPlugin.shouldApply(PACKAGE + "modelfix.ItemModelGeneratorSpanMixin", true));
	}

	@Test
	void collidingMixinIsAppliedWithoutModelfix() {
		for (String mixin : GlintsmithMixinPlugin.MODELFIX_COLLISIONS) {
			assertTrue(GlintsmithMixinPlugin.shouldApply(mixin, false), mixin);
		}
	}

	@Test
	void otherMixinsAreAlwaysApplied() {
		List<String> others = List.of(
			PACKAGE + "modelfix.ItemModelGeneratorMixin",
			PACKAGE + "modelfix.SpanMixin",
			PACKAGE + "modelfix.TextureAtlasSpriteMixin");
		for (String mixin : others) {
			assertTrue(GlintsmithMixinPlugin.shouldApply(mixin, true), mixin);
			assertTrue(GlintsmithMixinPlugin.shouldApply(mixin, false), mixin);
		}
	}

	@Test
	void unknownClassNamesAreApplied() {
		for (String mixin : List.of("", "some.other.Mixin", PACKAGE + "glint.ItemRendererGlintMixin")) {
			assertTrue(GlintsmithMixinPlugin.shouldApply(mixin, true), mixin);
			assertTrue(GlintsmithMixinPlugin.shouldApply(mixin, false), mixin);
		}
	}

	@Test
	void mixinConfigRegistersThePluginAndAllMixins() throws IOException {
		JsonObject config;
		try (InputStream in = GlintsmithMixinPluginTest.class.getResourceAsStream("/glintsmith.mixins.json")) {
			assertNotNull(in, "missing glintsmith.mixins.json");
			config = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
		}
		assertEquals(GlintsmithMixinPlugin.class.getName(), config.get("plugin").getAsString());
		assertEquals(0, config.getAsJsonObject("injectors").get("defaultRequire").getAsInt());
		List<String> listed = new ArrayList<>();
		for (JsonElement e : config.getAsJsonArray("client")) {
			listed.add(PACKAGE + e.getAsString());
		}
		for (String colliding : GlintsmithMixinPlugin.MODELFIX_COLLISIONS) {
			assertTrue(listed.contains(colliding), "not listed in glintsmith.mixins.json: " + colliding);
		}
		assertEquals(4, listed.size(), listed.toString());
	}
}
