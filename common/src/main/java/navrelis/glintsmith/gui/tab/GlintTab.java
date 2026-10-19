package navrelis.glintsmith.gui.tab;

import navrelis.glintsmith.config.GlintsmithConfig;
import navrelis.glintsmith.gui.SettingRows;
import navrelis.glintsmith.gui.Ui;
import navrelis.glintsmith.style.ColorStyle;
import navrelis.glintsmith.style.GlintCategory;

/**
 * The glint master switch, the atlas-independent scale and the settings of the four glint layers (item, armor, entity,
 * GUI).
 */
public final class GlintTab extends ConfigTab {
	private GlintCategory category = GlintCategory.ITEM;

	public GlintTab(GlintsmithConfig config) {
		super(config, Ui.t("glintsmith.tab.glint"));
	}

	@Override
	protected void fill() {
		GlintsmithConfig.Glint glint = config.glint;
		GlintsmithConfig.GlintCategorySettings layer = glint.forCategory(category);

		list.addHeading(Ui.t("glintsmith.glint.heading"));
		list.addFull(Ui.toggle(Ui.WIDTH, "glintsmith.glint.enabled", glint.enabled, v -> glint.enabled = v));
		list.addFull(Ui.toggle(Ui.WIDTH, "glintsmith.glint.atlas_scale", glint.atlasIndependentScale,
			v -> glint.atlasIndependentScale = v));
		list.addFull(Ui.cycle(Ui.WIDTH, "glintsmith.glint.category", "glintsmith.glint.category",
			GlintCategory.values(), category, true, selected -> {
				category = selected;
				rebuild();
			}));

		list.addFull(Ui.toggle(Ui.WIDTH, "glintsmith.glint.layer_enabled", layer.enabled, v -> layer.enabled = v));
		list.addHelp(Ui.t("glintsmith.glint.texture"));
		SettingRows.textureRow(list, "", Ui.t("glintsmith.glint.texture.hint_vanilla"), layer.texture,
			text -> layer.texture = text);

		list.addFull(Ui.toggle(Ui.WIDTH, "glintsmith.glint.tint", layer.tint != null, on -> {
			layer.tint = on ? ColorStyle.of(ColorStyle.DEFAULT_COLOR) : null;
			rebuild();
		}));
		if (layer.tint != null) {
			list.addFull(SettingRows.styleButton(Ui.WIDTH, "glintsmith.glint.tint_color", () -> layer.tint, c -> layer.tint = c));
		}

		SettingRows.sliderRow(list, "glintsmith.glint.speed", 0.0f, 10.0f, 0.05f,
			SettingRows.decimals("glintsmith.glint.speed", 2), layer.speed, v -> layer.speed = v);
		SettingRows.sliderRow(list, "glintsmith.glint.scale", 0.1f, 10.0f, 0.05f,
			SettingRows.decimals("glintsmith.glint.scale", 2), layer.scale, v -> layer.scale = v);
		SettingRows.sliderRow(list, "glintsmith.glint.strength", 0.0f, 1.0f, 0.01f,
			SettingRows.percent("glintsmith.glint.strength"), layer.strength, v -> layer.strength = v);
		SettingRows.sliderRow(list, "glintsmith.glint.rotation", -180.0f, 180.0f, 1.0f,
			SettingRows.whole("glintsmith.glint.rotation"), layer.rotation, v -> layer.rotation = v);

		list.addHelp(Ui.t("glintsmith.glint.help"));
	}

	@Override
	public void reset(GlintsmithConfig defaults) {
		config.glint = defaults.glint;
	}
}
