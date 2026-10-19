package navrelis.glintsmith.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

/**
 * Pins the built-in defaults (what a fresh install gets) to the author's own config kept in
 * {@code src/test/resources/glintsmith/owner-defaults.json}.
 */
class DefaultConfigTest {
	private static JsonElement ownerDefaults() throws IOException {
		try (InputStream in = DefaultConfigTest.class.getResourceAsStream("/glintsmith/owner-defaults.json")) {
			if (in == null) {
				throw new IOException("owner-defaults.json is missing from the test resources");
			}
			try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
				return JsonParser.parseReader(reader);
			}
		}
	}

	/** Through the text form, so that a float 0.9 and the parsed 0.9 (and 1.0E-4) compare equal. */
	private static JsonElement normalised(GlintsmithConfig config) {
		return JsonParser.parseString(GlintsmithConfig.GSON.toJson(config));
	}

	@Test
	void freshConfigEqualsTheOwnerConfig() throws IOException {
		assertEquals(ownerDefaults(), normalised(new GlintsmithConfig().sanitize()));
	}

	@Test
	void copyOfFreshConfigEqualsTheOwnerConfig() throws IOException {
		assertEquals(ownerDefaults(), normalised(new GlintsmithConfig().copy()));
	}

	@Test
	void unsanitisedFreshConfigIsAlreadySanitised() {
		assertEquals(normalised(new GlintsmithConfig().sanitize()), normalised(new GlintsmithConfig()));
	}
}
