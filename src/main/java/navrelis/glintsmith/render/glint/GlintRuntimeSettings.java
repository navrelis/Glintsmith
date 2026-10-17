package navrelis.glintsmith.render.glint;

import navrelis.glintsmith.config.GlintsmithConfig;
import navrelis.glintsmith.style.ColorEvaluator;
import navrelis.glintsmith.style.ColorStyle;
import navrelis.glintsmith.style.GlintCategory;
import navrelis.glintsmith.style.GlintLook;
import navrelis.glintsmith.style.GlintSettings;
import navrelis.glintsmith.style.Rule;
import org.jetbrains.annotations.Nullable;

/**
 * Immutable snapshot of the glint part of the config, built once per config change. The renderer swaps it atomically
 * (volatile field) and never reads {@code config.glint} itself: {@link #from(GlintsmithConfig)} is the only place that
 * does.
 *
 * @param item defaults of the item layer
 * @param armor defaults of the armor layer
 * @param entity defaults of the entity/BEWLR layer
 * @param gui defaults of the GUI layer
 * @param anyCustom false when the master switch is off, or when every category is at its defaults and no rule has a
 *                  glint override: the renderer then leaves all glints vanilla without any work
 */
public record GlintRuntimeSettings(
	GlintCategoryRuntime item,
	GlintCategoryRuntime armor,
	GlintCategoryRuntime entity,
	GlintCategoryRuntime gui,
	boolean anyCustom
) {
	/** Everything vanilla. */
	public static final GlintRuntimeSettings VANILLA = from(new GlintsmithConfig().sanitize());

	/**
	 * The defaults of one glint layer.
	 *
	 * @param enabled false hides the glint of this layer
	 * @param texture a texture spec, null for the vanilla texture
	 * @param tint the tint style, or null
	 * @param speed 0..10, 1 = vanilla
	 * @param scale 0.1..10, 1 = vanilla
	 * @param strength 0..1
	 * @param rotation degrees, 10 = vanilla
	 * @param categoryLook the same values as a {@link GlintLook} with a compiled tint, used where no item is known
	 */
	public record GlintCategoryRuntime(
		boolean enabled,
		@Nullable String texture,
		@Nullable ColorStyle tint,
		float speed,
		float scale,
		float strength,
		float rotation,
		GlintLook categoryLook
	) {
		static GlintCategoryRuntime of(GlintsmithConfig.GlintCategorySettings cs) {
			String texture = GlintSettings.isVanillaTexture(cs.texture) ? null : cs.texture;
			ColorStyle tint = cs.tint == null ? null : cs.tint.copy();
			GlintLook look = new GlintLook(
				cs.enabled,
				texture,
				tint == null ? null : ColorEvaluator.compile(tint),
				tint,
				cs.speed, cs.scale, cs.strength, cs.rotation);
			return new GlintCategoryRuntime(cs.enabled, texture, tint, cs.speed, cs.scale, cs.strength, cs.rotation, look);
		}

		/**
		 * @return true if this layer is exactly the vanilla glint
		 */
		boolean isDefault() {
			return enabled && texture == null && tint == null && speed == 1.0f && scale == 1.0f && strength == 1.0f
				&& rotation == GlintParams.VANILLA_ROTATION;
		}
	}

	/**
	 * Builds the snapshot. Reads {@code config.glint} and the glint overrides of the rules. A null config gives
	 * {@link #VANILLA}-like settings.
	 */
	public static GlintRuntimeSettings from(@Nullable GlintsmithConfig config) {
		GlintsmithConfig.Glint glint = config == null ? null : config.glint;
		if (glint == null) {
			GlintsmithConfig.GlintCategorySettings defaults = new GlintsmithConfig.GlintCategorySettings();
			GlintCategoryRuntime d = GlintCategoryRuntime.of(defaults);
			return new GlintRuntimeSettings(d, d, d, d, false);
		}
		GlintCategoryRuntime item = GlintCategoryRuntime.of(orDefault(glint.item));
		GlintCategoryRuntime armor = GlintCategoryRuntime.of(orDefault(glint.armor));
		GlintCategoryRuntime entity = GlintCategoryRuntime.of(orDefault(glint.entity));
		GlintCategoryRuntime gui = GlintCategoryRuntime.of(orDefault(glint.gui));

		boolean custom = false;
		if (glint.enabled) {
			custom = !item.isDefault() || !armor.isDefault() || !entity.isDefault() || !gui.isDefault();
			if (!custom && config.rules != null) {
				for (Rule rule : config.rules) {
					if (rule != null && rule.enabled && rule.error == null && rule.glint != null) {
						custom = true;
						break;
					}
				}
			}
		}
		return new GlintRuntimeSettings(item, armor, entity, gui, custom);
	}

	private static GlintsmithConfig.GlintCategorySettings orDefault(@Nullable GlintsmithConfig.GlintCategorySettings s) {
		return s != null ? s : new GlintsmithConfig.GlintCategorySettings();
	}

	/**
	 * @return the settings of one layer
	 */
	public GlintCategoryRuntime get(GlintCategory category) {
		return switch (category) {
			case ITEM -> item;
			case ARMOR -> armor;
			case ENTITY -> entity;
			case GUI -> gui;
		};
	}
}
