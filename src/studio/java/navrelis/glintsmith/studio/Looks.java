package navrelis.glintsmith.studio;

import java.util.Locale;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import navrelis.glintsmith.config.ConfigManager;
import navrelis.glintsmith.config.GlintsmithConfig;
import navrelis.glintsmith.style.Rule;
import org.jetbrains.annotations.Nullable;

/**
 * Turns a scene look into a complete Glintsmith config. Nothing here touches the config file.
 * <p>
 * Base: the Glintsmith defaults, with every outline target on, "only local player" off, resolution scaling off (the
 * studio scales by the supersample factor itself), no distance fade and no rules. The look's keys are merged over it
 * in this order: {@code vanilla}, {@code outline}, {@code glint} (all four layers), {@code glint_layers},
 * {@code gap_fix}, {@code rules}, {@code config} (a raw partial {@code glintsmith.json}). Finally every outline
 * thickness is multiplied by the supersample factor.
 */
final class Looks {
	private static final String[] LAYERS = {"item", "armor", "entity", "gui"};

	private Looks() {
	}

	/**
	 * @param gapFixOverride if not null, the gap fix section to use instead of the look's (per-subject looks must not
	 *                       change baked models in the middle of a frame)
	 */
	static GlintsmithConfig build(Scene.Look look, int supersample, @Nullable GlintsmithConfig.GapFix gapFixOverride) {
		GlintsmithConfig base = new GlintsmithConfig();
		base.rules.clear();
		GlintsmithConfig.Outline o = base.outline;
		o.onlyLocalPlayer = false;
		o.scaleWithResolution = false;
		o.disableWithShaders = false;
		o.fadeStart = 0.0f;
		o.fadeEnd = 0.0f;
		GlintsmithConfig.Targets t = o.targets;
		t.firstPerson = true;
		t.thirdPerson = true;
		t.armor = true;
		t.dropped = true;
		t.itemFrame = true;
		t.gui = true;
		t.other = true;

		JsonObject tree = JsonParser.parseString(ConfigManager.toJson(base)).getAsJsonObject();
		JsonObject json = look.json();
		String path = "looks." + look.id();
		if (json.has("vanilla") && json.get("vanilla").getAsBoolean()) {
			tree.getAsJsonObject("outline").addProperty("enabled", false);
			tree.getAsJsonObject("glint").addProperty("enabled", false);
			tree.getAsJsonObject("gapFix").addProperty("enabled", false);
		}
		if (json.has("outline")) {
			merge(tree.getAsJsonObject("outline"), normalize(Scene.asObject(json.get("outline"), path + ".outline")));
		}
		if (json.has("glint")) {
			JsonObject glint = normalize(Scene.asObject(json.get("glint"), path + ".glint"));
			JsonObject glintTree = tree.getAsJsonObject("glint");
			if (glint.has("enabled") && glint.size() == 1) {
				glintTree.add("enabled", glint.get("enabled"));
			} else {
				glintTree.addProperty("enabled", true);
				for (String layer : LAYERS) {
					merge(glintTree.getAsJsonObject(layer), glint.deepCopy());
				}
			}
		}
		if (json.has("glint_layers")) {
			JsonObject layers = Scene.asObject(json.get("glint_layers"), path + ".glint_layers");
			for (Map.Entry<String, JsonElement> e : layers.entrySet()) {
				JsonObject target = tree.getAsJsonObject("glint").getAsJsonObject(e.getKey());
				if (target == null) {
					throw new IllegalArgumentException(path + ".glint_layers: unknown layer '" + e.getKey() + "' (item, armor, entity, gui)");
				}
				merge(target, normalize(Scene.asObject(e.getValue(), path + ".glint_layers." + e.getKey())));
			}
		}
		if (json.has("gap_fix")) {
			JsonElement fix = json.get("gap_fix");
			JsonObject fixTree = tree.getAsJsonObject("gapFix");
			if (fix.isJsonPrimitive()) {
				switch (fix.getAsString().toLowerCase(Locale.ROOT)) {
					case "auto" -> {
						fixTree.addProperty("enabled", true);
						fixTree.addProperty("mode", "AUTO");
					}
					case "off", "vanilla" -> fixTree.addProperty("enabled", false);
					default -> throw new IllegalArgumentException(path + ".gap_fix: expected auto, off or an object");
				}
			} else {
				merge(fixTree, normalize(Scene.asObject(fix, path + ".gap_fix")));
			}
		}
		if (json.has("rules")) {
			JsonArray rules = Scene.asArray(json.get("rules"), path + ".rules");
			JsonArray normalized = new JsonArray();
			for (JsonElement rule : rules) {
				normalized.add(normalize(Scene.asObject(rule, path + ".rules[]")));
			}
			tree.add("rules", normalized);
		}
		if (json.has("config")) {
			merge(tree, normalize(Scene.asObject(json.get("config"), path + ".config")));
		}

		GlintsmithConfig config;
		try {
			config = ConfigManager.fromJson(tree.toString());
		} catch (RuntimeException e) {
			throw new IllegalArgumentException(path + ": not a valid Glintsmith config: " + e.getMessage(), e);
		}
		config.sanitize();
		for (String warning : config.warnings()) {
			throw new IllegalArgumentException(path + ": " + warning);
		}
		for (Rule rule : config.rules) {
			if (rule.error != null) {
				throw new IllegalArgumentException(path + ".rules: " + rule.error);
			}
		}
		// Pixel sizes are output pixels: the frames are rendered supersample times larger and downsampled later.
		config.outline.thickness *= supersample;
		for (Rule rule : config.rules) {
			if (rule.outline != null && rule.outline.thickness != null) {
				rule.outline.thickness *= supersample;
			}
		}
		if (gapFixOverride != null) {
			config.gapFix = copyGapFix(gapFixOverride);
		}
		return config.sanitize();
	}

	private static GlintsmithConfig.GapFix copyGapFix(GlintsmithConfig.GapFix src) {
		GlintsmithConfig.GapFix copy = new GlintsmithConfig.GapFix();
		copy.enabled = src.enabled;
		copy.mode = src.mode;
		copy.uvShrinkMultiplier = src.uvShrinkMultiplier;
		copy.sideExpansion = src.sideExpansion;
		copy.sideIndent = src.sideIndent;
		copy.preciseSpans = src.preciseSpans;
		return copy;
	}

	/** Deep merge: objects recursively, everything else (arrays, values, null) replaces. */
	static void merge(JsonObject target, JsonObject source) {
		for (Map.Entry<String, JsonElement> e : source.entrySet()) {
			JsonElement existing = target.get(e.getKey());
			if (existing != null && existing.isJsonObject() && e.getValue().isJsonObject()) {
				merge(existing.getAsJsonObject(), e.getValue().getAsJsonObject());
			} else {
				target.add(e.getKey(), e.getValue().deepCopy());
			}
		}
	}

	/**
	 * Studio conveniences on top of the config format: a colour given as a plain hex string ({@code "color":
	 * "#AA00FF"}, also {@code tint}) becomes a static colour style, and enum values ({@code mode}, {@code condition})
	 * may be lower case.
	 */
	static JsonObject normalize(JsonObject in) {
		JsonObject out = new JsonObject();
		for (Map.Entry<String, JsonElement> e : in.entrySet()) {
			String key = e.getKey();
			JsonElement v = e.getValue();
			if ((key.equals("color") || key.equals("tint")) && v.isJsonPrimitive()) {
				JsonObject style = new JsonObject();
				style.addProperty("mode", "STATIC");
				JsonArray colors = new JsonArray();
				colors.add(v.getAsString());
				style.add("colors", colors);
				out.add(key, style);
			} else if ((key.equals("mode") || key.equals("condition")) && v.isJsonPrimitive()) {
				out.add(key, new JsonPrimitive(v.getAsString().toUpperCase(Locale.ROOT)));
			} else if (v.isJsonObject()) {
				out.add(key, normalize(v.getAsJsonObject()));
			} else if (v.isJsonArray()) {
				JsonArray array = new JsonArray();
				for (JsonElement item : v.getAsJsonArray()) {
					array.add(item.isJsonObject() ? normalize(item.getAsJsonObject()) : item.deepCopy());
				}
				out.add(key, array);
			} else {
				out.add(key, v.deepCopy());
			}
		}
		return out;
	}
}
