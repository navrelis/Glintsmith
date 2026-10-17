package navrelis.glintsmith.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import navrelis.glintsmith.config.GapFixMode;
import navrelis.glintsmith.config.OutlineCondition;
import navrelis.glintsmith.style.ColorMode;
import navrelis.glintsmith.style.GlintCategory;
import navrelis.glintsmith.style.OutlineTarget;
import navrelis.glintsmith.style.Rule;
import org.junit.jupiter.api.Test;

class LangFilesTest {
	/** Literals that are only the start of keys built from enum names (see dynamicKeys). */
	private static final List<String> PREFIXES = List.of("glintsmith.common.tri");

	private static Map<String, String> load(String name) throws IOException {
		try (InputStream in = LangFilesTest.class.getResourceAsStream("/assets/glintsmith/lang/" + name)) {
			assertNotNull(in, name);
			try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
				JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
				Map<String, String> map = new TreeMap<>();
				json.entrySet().forEach(e -> map.put(e.getKey(), e.getValue().getAsString()));
				return map;
			}
		}
	}

	private static int count(String text, String token) {
		int n = 0;
		for (int i = text.indexOf(token); i >= 0; i = text.indexOf(token, i + token.length())) {
			n++;
		}
		return n;
	}

	@Test
	void englishAndGermanHaveTheSameKeys() throws IOException {
		Map<String, String> en = load("en_us.json");
		Map<String, String> de = load("de_de.json");
		TreeSet<String> onlyEn = new TreeSet<>(en.keySet());
		onlyEn.removeAll(de.keySet());
		TreeSet<String> onlyDe = new TreeSet<>(de.keySet());
		onlyDe.removeAll(en.keySet());
		assertTrue(onlyEn.isEmpty(), "missing in de_de.json: " + onlyEn);
		assertTrue(onlyDe.isEmpty(), "missing in en_us.json: " + onlyDe);
	}

	@Test
	void noValueIsEmptyAndFormatArgumentsMatch() throws IOException {
		Map<String, String> en = load("en_us.json");
		Map<String, String> de = load("de_de.json");
		for (Map.Entry<String, String> e : en.entrySet()) {
			assertFalse(e.getValue().isBlank(), "empty en_us value: " + e.getKey());
			String german = de.get(e.getKey());
			assertNotNull(german, e.getKey());
			assertFalse(german.isBlank(), "empty de_de value: " + e.getKey());
			assertEquals(count(e.getValue(), "%s"), count(german, "%s"), "%s count differs: " + e.getKey());
		}
	}

	@Test
	void everyTooltipBelongsToAKnownKey() throws IOException {
		Map<String, String> en = load("en_us.json");
		for (String key : en.keySet()) {
			if (key.endsWith(".tooltip") && key.startsWith("glintsmith.")) {
				String base = key.substring(0, key.length() - ".tooltip".length());
				boolean baseExists = en.containsKey(base);
				boolean usedAsLiteral = sourceLiterals().contains(base) || dynamicKeys().contains(key);
				assertTrue(baseExists || usedAsLiteral, "tooltip without a label or user: " + key);
			}
		}
	}

	@Test
	void everyKeyUsedByTheCodeIsTranslated() throws IOException {
		Map<String, String> en = load("en_us.json");
		List<String> missing = new ArrayList<>();
		for (String literal : sourceLiterals()) {
			if (!en.containsKey(literal) && !en.containsKey(literal + ".tooltip") && !PREFIXES.contains(literal)) {
				missing.add(literal);
			}
		}
		for (String key : dynamicKeys()) {
			if (!en.containsKey(key)) {
				missing.add(key);
			}
		}
		assertTrue(missing.isEmpty(), "keys used in code but missing from en_us.json: " + missing);
	}

	/** Keys the code builds from enum names. */
	private static TreeSet<String> dynamicKeys() {
		TreeSet<String> keys = new TreeSet<>();
		for (ColorMode mode : ColorMode.values()) {
			keys.add("glintsmith.color.mode." + mode.name().toLowerCase(Locale.ROOT));
			keys.add("glintsmith.color.mode." + mode.name().toLowerCase(Locale.ROOT) + ".tooltip");
			keys.add("glintsmith.color.help." + mode.name().toLowerCase(Locale.ROOT));
		}
		for (OutlineCondition v : OutlineCondition.values()) {
			addValueKeys(keys, "glintsmith.outline.condition", v.name().toLowerCase(Locale.ROOT));
		}
		for (GlintCategory v : GlintCategory.values()) {
			addValueKeys(keys, "glintsmith.glint.category", v.name().toLowerCase(Locale.ROOT));
		}
		for (GapFixMode v : GapFixMode.values()) {
			addValueKeys(keys, "glintsmith.modelfix.mode", v.name().toLowerCase(Locale.ROOT));
		}
		for (Rule.Condition v : Rule.Condition.values()) {
			addValueKeys(keys, "glintsmith.rule.condition", v.name().toLowerCase(Locale.ROOT));
		}
		for (OutlineTarget target : OutlineTarget.values()) {
			if (target != OutlineTarget.OTHER) {
				addValueKeys(keys, "glintsmith.rule.target", target.name().toLowerCase(Locale.ROOT));
				addValueKeys(keys, "glintsmith.outline.target", target.name().toLowerCase(Locale.ROOT));
			}
		}
		for (String tri : List.of("inherit", "on", "off")) {
			keys.add("glintsmith.common.tri." + tri);
		}
		keys.add("key.categories.glintsmith");
		return keys;
	}

	private static void addValueKeys(TreeSet<String> keys, String prefix, String name) {
		keys.add(prefix + "." + name);
		keys.add(prefix + "." + name + ".tooltip");
	}

	private static TreeSet<String> sourceLiterals() throws IOException {
		Path root = Path.of(System.getProperty("user.dir")).toAbsolutePath();
		Path gui = null;
		for (Path dir = root; dir != null; dir = dir.getParent()) {
			Path candidate = dir.resolve("src/main/java/navrelis/glintsmith");
			if (Files.isDirectory(candidate.resolve("gui"))) {
				gui = candidate;
				break;
			}
		}
		assertNotNull(gui, "could not find the source tree from " + root);
		Pattern literal = Pattern.compile("\"((?:key\\.)?(?:glintsmith|categories\\.glintsmith)\\.[a-z0-9_.]*[a-z0-9_])\"");
		TreeSet<String> found = new TreeSet<>();
		for (String folder : List.of("gui", "integration")) {
			try (Stream<Path> files = Files.walk(gui.resolve(folder))) {
				for (Path file : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".java"))::iterator) {
					String text = Files.readString(file, StandardCharsets.UTF_8);
					Matcher m = literal.matcher(text);
					while (m.find()) {
						found.add(m.group(1));
					}
				}
			}
		}
		return found;
	}

	@Test
	void sourceScanFindsKeys() throws IOException {
		assertTrue(sourceLiterals().size() > 50);
	}
}
