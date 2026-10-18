package navrelis.glintsmith.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import navrelis.glintsmith.style.ColorMode;
import navrelis.glintsmith.style.ColorStyle;
import navrelis.glintsmith.style.Colors;
import navrelis.glintsmith.style.DurabilityAlert;
import navrelis.glintsmith.style.GlintCategory;
import navrelis.glintsmith.style.GlintSettings;
import navrelis.glintsmith.style.Num;
import navrelis.glintsmith.style.OutlineSettings;
import navrelis.glintsmith.style.OutlineTarget;
import navrelis.glintsmith.style.Rule;
import org.jetbrains.annotations.Nullable;

/**
 * Root of {@code glintsmith.json}. Plain public fields, serialised by Gson.
 * <p>
 * Call {@link #sanitize()} after loading or editing; {@link ConfigManager} does it on every load and save.
 * Instances handed out by {@link ConfigManager#get()} must be treated as read-only; edit a {@link #copy()}
 * and save that.
 */
public final class GlintsmithConfig {
	static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

	public int version = 1;
	public Outline outline = new Outline();
	public Glint glint = new Glint();
	public GapFix gapFix = new GapFix();
	public DurabilityAlert durabilityAlert = new DurabilityAlert();
	/** Overrides of the enchantment colours, enchantment id to hex. */
	public Map<String, String> enchantmentColors = new LinkedHashMap<>();
	/** Evaluated in order, later rules override earlier ones. */
	public List<Rule> rules = defaultRules();

	/** Problems found by the last {@link #sanitize()} (for example an invalid category texture that was reset). Not saved. */
	private transient List<String> warnings = new ArrayList<>();

	public GlintsmithConfig() {
	}

	/**
	 * @return the rules a fresh config starts with
	 */
	public static List<Rule> defaultRules() {
		List<Rule> list = new ArrayList<>();

		Rule totem = new Rule();
		totem.name = "Totem of Undying";
		totem.items.add("minecraft:totem_of_undying");
		totem.enchantedCondition = Rule.Condition.ANY;
		totem.outline = OutlineSettings.colored(ColorStyle.of("#FFF3A5"));
		list.add(totem);

		Rule books = new Rule();
		books.name = "Example: no outline on books";
		books.enabled = true;
		books.items.add("minecraft:enchanted_book");
		books.outline = OutlineSettings.enabled(false);
		list.add(books);

		return list;
	}

	/**
	 * @return the problems found by the last {@link #sanitize()}; empty if there were none. Not saved to the file.
	 */
	public List<String> warnings() {
		return warnings == null ? List.of() : warnings;
	}

	/**
	 * @return an independent deep copy (sanitised if this one was)
	 */
	public GlintsmithConfig copy() {
		GlintsmithConfig copy = GSON.fromJson(GSON.toJson(this), GlintsmithConfig.class);
		return copy.sanitize();
	}

	/**
	 * Clamps every range, replaces missing sections by defaults and compiles the rule patterns
	 * (invalid rules keep their text and get {@link Rule#error} set).
	 *
	 * @return {@code this}
	 */
	public GlintsmithConfig sanitize() {
		warnings = new ArrayList<>();
		if (version < 1) {
			version = 1;
		}
		if (outline == null) {
			outline = new Outline();
		}
		outline.sanitize();
		if (glint == null) {
			glint = new Glint();
		}
		glint.sanitize(warnings);
		if (gapFix == null) {
			gapFix = new GapFix();
		}
		gapFix.sanitize();
		if (durabilityAlert == null) {
			durabilityAlert = new DurabilityAlert();
		}
		durabilityAlert.sanitize();

		Map<String, String> colors = new LinkedHashMap<>();
		if (enchantmentColors != null) {
			for (Map.Entry<String, String> e : enchantmentColors.entrySet()) {
				if (e.getKey() != null && e.getValue() != null && Colors.isValidHex(e.getValue())) {
					colors.put(e.getKey(), e.getValue());
				}
			}
		}
		enchantmentColors = colors;

		List<Rule> cleaned = new ArrayList<>();
		if (rules != null) {
			for (Rule rule : rules) {
				if (rule != null) {
					cleaned.add(rule.sanitize());
				}
			}
		}
		rules = cleaned;
		return this;
	}

	/**
	 * Global outline settings; these are the defaults that rules override.
	 */
	public static final class Outline {
		public boolean enabled = true;
		public OutlineCondition condition = OutlineCondition.ENCHANTED;
		public Targets targets = new Targets();
		public boolean onlyLocalPlayer = false;
		public ColorStyle color = defaultColor();
		/** Screen pixels, 0.5..16. */
		public float thickness = 8.5f;
		/** Own thickness per place; null entries use {@link #thickness}. Rules that set a thickness still override both. */
		public TargetThickness targetThickness = new TargetThickness();
		/** 0..1. */
		public float softness = 0.0f;
		/** 0..1. */
		public float opacity = 0.25f;
		public boolean disableWithShaders = false;
		/** Keep first-person outlines while an Iris shader pack is active. */
		public boolean handWithShaderPack = true;
		/** The renderer multiplies the thickness by framebufferHeight / 1080. */
		public boolean scaleWithResolution = true;
		/** Distance fade of world outlines in blocks, 0..256; 0 and 0 = off. */
		public float fadeStart = 0.0f;
		/** See {@link #fadeStart}; if non-zero it is always greater than fadeStart. */
		public float fadeEnd = 0.0f;

		public Outline() {
		}

		/** The default outline colour: a slow rainbow. */
		private static ColorStyle defaultColor() {
			ColorStyle style = ColorStyle.of("#FFFFFF");
			style.mode = ColorMode.RAINBOW;
			style.speed = 0.5f;
			return style;
		}

		void sanitize() {
			if (condition == null) {
				condition = OutlineCondition.ENCHANTED;
			}
			if (targets == null) {
				targets = new Targets();
			}
			if (color == null) {
				color = defaultColor();
			}
			color.sanitize();
			thickness = Num.clamp(thickness, OutlineSettings.MIN_THICKNESS, OutlineSettings.MAX_THICKNESS, 8.5f);
			if (targetThickness == null) {
				targetThickness = new TargetThickness();
			}
			targetThickness.sanitize();
			softness = Num.clamp(softness, 0.0f, 1.0f, 0.0f);
			opacity = Num.clamp(opacity, 0.0f, 1.0f, 0.25f);
			fadeStart = Num.clamp(fadeStart, 0.0f, 256.0f, 0.0f);
			fadeEnd = Num.clamp(fadeEnd, 0.0f, 256.0f, 0.0f);
			if (fadeEnd != 0.0f && fadeEnd <= fadeStart) {
				if (fadeStart >= 256.0f) {
					fadeStart = 255.0f;
				}
				fadeEnd = fadeStart + 1.0f;
			}
		}
	}

	/**
	 * Which render targets outlines are drawn in at all. Rules cannot override these.
	 */
	public static final class Targets {
		public boolean firstPerson = true;
		public boolean thirdPerson = true;
		public boolean armor = false;
		public boolean dropped = true;
		public boolean itemFrame = true;
		public boolean gui = false;
		/** Other display contexts such as head or none, for example items worn on the head. */
		public boolean other = false;

		public Targets() {
		}

		/**
		 * @return whether outlines may be drawn in this target; {@link OutlineTarget#OTHER} follows {@code other}
		 */
		public boolean allows(OutlineTarget target) {
			return switch (target) {
				case FIRST_PERSON -> firstPerson;
				case THIRD_PERSON -> thirdPerson;
				case ARMOR -> armor;
				case DROPPED -> dropped;
				case ITEM_FRAME -> itemFrame;
				case GUI -> gui;
				case OTHER -> other;
			};
		}
	}

	/**
	 * Own outline thickness per place. Null means the place uses the global {@link Outline#thickness}, which is the
	 * default everywhere. Rules that set a thickness still override these.
	 */
	public static final class TargetThickness {
		@Nullable
		public Float firstPerson = null;
		@Nullable
		public Float thirdPerson = null;
		@Nullable
		public Float armor = null;
		@Nullable
		public Float dropped = null;
		@Nullable
		public Float itemFrame = null;
		@Nullable
		public Float gui = null;
		@Nullable
		public Float other = null;

		public TargetThickness() {
		}

		/**
		 * @return the own thickness of the place, null to use the global one
		 */
		@Nullable
		public Float get(OutlineTarget target) {
			return switch (target) {
				case FIRST_PERSON -> firstPerson;
				case THIRD_PERSON -> thirdPerson;
				case ARMOR -> armor;
				case DROPPED -> dropped;
				case ITEM_FRAME -> itemFrame;
				case GUI -> gui;
				case OTHER -> other;
			};
		}

		/**
		 * Sets the own thickness of a place.
		 *
		 * @param value the thickness, null to use the global one
		 */
		public void set(OutlineTarget target, @Nullable Float value) {
			switch (target) {
				case FIRST_PERSON -> firstPerson = value;
				case THIRD_PERSON -> thirdPerson = value;
				case ARMOR -> armor = value;
				case DROPPED -> dropped = value;
				case ITEM_FRAME -> itemFrame = value;
				case GUI -> gui = value;
				case OTHER -> other = value;
			}
		}

		/** Clamps every own thickness to the allowed range; NaN becomes null. */
		void sanitize() {
			firstPerson = clamp(firstPerson);
			thirdPerson = clamp(thirdPerson);
			armor = clamp(armor);
			dropped = clamp(dropped);
			itemFrame = clamp(itemFrame);
			gui = clamp(gui);
			other = clamp(other);
		}

		@Nullable
		private static Float clamp(@Nullable Float value) {
			return Num.clamp(value, OutlineSettings.MIN_THICKNESS, OutlineSettings.MAX_THICKNESS);
		}
	}

	/**
	 * Glint settings: a master switch and the defaults of the four glint layers.
	 */
	public static final class Glint {
		/** Master switch; off leaves every glint untouched (vanilla). */
		public boolean enabled = true;
		/**
		 * Corrects the scale of customised item glints for the size of the block texture atlas, so that Scale 1.00 looks
		 * like vanilla in packs with a large atlas. Configs written before 2.0.0 lack the field, so Gson keeps this
		 * default: they get it on.
		 */
		public boolean atlasIndependentScale = true;
		public GlintCategorySettings item = GlintCategorySettings.itemDefaults();
		public GlintCategorySettings armor = GlintCategorySettings.armorDefaults();
		public GlintCategorySettings entity = GlintCategorySettings.layerDefaults();
		public GlintCategorySettings gui = GlintCategorySettings.layerDefaults();

		public Glint() {
		}

		/**
		 * @return the defaults of the given layer
		 */
		public GlintCategorySettings forCategory(GlintCategory category) {
			return switch (category) {
				case ITEM -> item;
				case ARMOR -> armor;
				case ENTITY -> entity;
				case GUI -> gui;
			};
		}

		void sanitize(List<String> warnings) {
			if (item == null) {
				item = GlintCategorySettings.itemDefaults();
			}
			if (armor == null) {
				armor = GlintCategorySettings.armorDefaults();
			}
			if (entity == null) {
				entity = GlintCategorySettings.layerDefaults();
			}
			if (gui == null) {
				gui = GlintCategorySettings.layerDefaults();
			}
			item.sanitize(warnings, "glint.item");
			armor.sanitize(warnings, "glint.armor");
			entity.sanitize(warnings, "glint.entity");
			gui.sanitize(warnings, "glint.gui");
		}
	}

	/**
	 * Defaults of one glint layer. The vanilla look is the default value of every field.
	 */
	public static final class GlintCategorySettings {
		/** {@code false} hides the glint of this layer. */
		public boolean enabled = true;
		/** Texture spec, see {@link GlintSettings#validateTexture(String)}; null for the vanilla texture. */
		public String texture = null;
		/** Null for no tint. */
		public ColorStyle tint = null;
		/** 0..10, 1 = vanilla. */
		public float speed = 1.0f;
		/** 0.1..10, 1 = vanilla. */
		public float scale = 1.0f;
		/** 0..1. */
		public float strength = 1.0f;
		/** Degrees, -180..180, 10 = vanilla. */
		public float rotation = 10.0f;

		public GlintCategorySettings() {
		}

		/** The default of the item layer: a slowly moving frost texture with a light purple rainbow tint. */
		static GlintCategorySettings itemDefaults() {
			GlintCategorySettings s = new GlintCategorySettings();
			s.texture = "preset:frost";
			ColorStyle tint = ColorStyle.of(ColorStyle.DEFAULT_COLOR);
			tint.mode = ColorMode.RAINBOW;
			s.tint = tint;
			s.speed = 0.5f;
			s.strength = 0.9f;
			return s;
		}

		/** The default of the armor layer: switched off. */
		static GlintCategorySettings armorDefaults() {
			GlintCategorySettings s = layerDefaults();
			s.enabled = false;
			return s;
		}

		/** The default of the entity and GUI layers: the fire texture, everything else vanilla. */
		static GlintCategorySettings layerDefaults() {
			GlintCategorySettings s = new GlintCategorySettings();
			s.texture = "preset:fire";
			return s;
		}

		void sanitize(List<String> warnings, String path) {
			if (texture != null && texture.isBlank()) {
				texture = null;
			}
			String problem = GlintSettings.validateTexture(texture);
			if (problem != null) {
				warnings.add(path + ".texture: " + problem + "; reset to vanilla.");
				texture = null;
			}
			if (tint != null) {
				tint.sanitize();
			}
			speed = Num.clamp(speed, 0.0f, 10.0f, 1.0f);
			scale = Num.clamp(scale, 0.1f, 10.0f, 1.0f);
			strength = Num.clamp(strength, 0.0f, 1.0f, 1.0f);
			rotation = Num.clamp(rotation, -180.0f, 180.0f, 10.0f);
		}
	}

	/**
	 * Settings of the glint gap fix (the seams the vanilla glint leaves on some models).
	 */
	public static final class GapFix {
		public boolean enabled = true;
		public GapFixMode mode = GapFixMode.AUTO;
		/** 0..1. */
		public float uvShrinkMultiplier = 0.0f;
		/** -0.1..0.1. */
		public float sideExpansion = 0.005f;
		/** -0.1..0.1. */
		public float sideIndent = 0.0001f;
		public boolean preciseSpans = false;

		public GapFix() {
		}

		void sanitize() {
			if (mode == null) {
				mode = GapFixMode.AUTO;
			}
			uvShrinkMultiplier = Num.clamp(uvShrinkMultiplier, 0.0f, 1.0f, 0.0f);
			sideExpansion = Num.clamp(sideExpansion, -0.1f, 0.1f, 0.005f);
			sideIndent = Num.clamp(sideIndent, -0.1f, 0.1f, 0.0001f);
		}
	}
}
