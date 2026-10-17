package navrelis.glintsmith.gui.tab;

import navrelis.glintsmith.config.GlintsmithConfig;
import navrelis.glintsmith.config.OutlineCondition;
import navrelis.glintsmith.gui.ColorPickerScreen;
import navrelis.glintsmith.gui.SettingRows;
import navrelis.glintsmith.gui.Ui;
import navrelis.glintsmith.gui.widget.ColorSwatch;
import navrelis.glintsmith.gui.widget.HexBox;
import navrelis.glintsmith.render.outline.OutlineSystem;
import navrelis.glintsmith.style.Colors;
import navrelis.glintsmith.style.DurabilityAlert;
import navrelis.glintsmith.style.OutlineSettings;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;

/**
 * Global outline settings and the low durability alert.
 */
public final class OutlineTab extends ConfigTab {
	/** Upper end of the distance fade sliders, in blocks. */
	private static final float FADE_MAX = 256.0f;
	private static final int COLOR_PICK_WIDTH = 182;
	private static final int COLOR_BOX_WIDTH = 100;

	public OutlineTab(GlintsmithConfig config) {
		super(config, Ui.t("glintsmith.tab.outline"));
	}

	@Override
	protected void fill() {
		GlintsmithConfig.Outline outline = config.outline;
		GlintsmithConfig.Targets targets = outline.targets;

		if (OutlineSystem.shaderPackActive()) {
			list.addHelp(Ui.t("glintsmith.outline.iris_active").withStyle(ChatFormatting.YELLOW));
		}
		list.addHeading(Ui.t("glintsmith.outline.heading"));
		list.addPair(
			Ui.toggle(Ui.HALF, "glintsmith.outline.enabled", outline.enabled, v -> outline.enabled = v),
			Ui.toggle(Ui.HALF, "glintsmith.outline.only_local_player", outline.onlyLocalPlayer, v -> outline.onlyLocalPlayer = v));
		list.addFull(Ui.cycle(Ui.WIDTH, "glintsmith.outline.condition", "glintsmith.outline.condition",
			OutlineCondition.values(), outline.condition, true, v -> outline.condition = v));

		list.addHeading(Ui.t("glintsmith.outline.targets"));
		list.addPair(
			Ui.toggle(Ui.HALF, "glintsmith.outline.target.first_person", targets.firstPerson, v -> targets.firstPerson = v),
			Ui.toggle(Ui.HALF, "glintsmith.outline.target.third_person", targets.thirdPerson, v -> targets.thirdPerson = v));
		list.addPair(
			Ui.toggle(Ui.HALF, "glintsmith.outline.target.armor", targets.armor, v -> targets.armor = v),
			Ui.toggle(Ui.HALF, "glintsmith.outline.target.dropped", targets.dropped, v -> targets.dropped = v));
		list.addPair(
			Ui.toggle(Ui.HALF, "glintsmith.outline.target.item_frame", targets.itemFrame, v -> targets.itemFrame = v),
			Ui.toggle(Ui.HALF, "glintsmith.outline.target.gui", targets.gui, v -> targets.gui = v));
		list.addRow(Ui.toggle(Ui.HALF, "glintsmith.outline.target.other", targets.other, v -> targets.other = v));

		list.addHeading(Ui.t("glintsmith.outline.look"));
		list.addFull(SettingRows.styleButton(Ui.WIDTH, "glintsmith.outline.color", () -> outline.color, c -> outline.color = c));
		SettingRows.sliderRow(list, "glintsmith.outline.thickness", OutlineSettings.MIN_THICKNESS,
			OutlineSettings.MAX_THICKNESS, 0.1f, SettingRows.decimals("glintsmith.outline.thickness", 1),
			outline.thickness, v -> outline.thickness = v);
		list.addFull(Ui.toggle(Ui.WIDTH, "glintsmith.outline.scale_with_resolution", outline.scaleWithResolution,
			v -> outline.scaleWithResolution = v));
		SettingRows.sliderRow(list, "glintsmith.outline.softness", 0.0f, 1.0f, 0.01f,
			SettingRows.percent("glintsmith.outline.softness"), outline.softness, v -> outline.softness = v);
		SettingRows.sliderRow(list, "glintsmith.outline.opacity", 0.0f, 1.0f, 0.01f,
			SettingRows.percent("glintsmith.outline.opacity"), outline.opacity, v -> outline.opacity = v);
		SettingRows.sliderRow(list, "glintsmith.outline.fade_start", 0.0f, FADE_MAX, 1.0f,
			SettingRows.blocksOrOff("glintsmith.outline.fade_start"), outline.fadeStart, v -> outline.fadeStart = v);
		SettingRows.sliderRow(list, "glintsmith.outline.fade_end", 0.0f, FADE_MAX, 1.0f,
			SettingRows.blocksOrOff("glintsmith.outline.fade_end"), outline.fadeEnd, v -> outline.fadeEnd = v);

		list.addHeading(Ui.t("glintsmith.outline.shader_heading"));
		list.addPair(
			Ui.toggle(Ui.HALF, "glintsmith.outline.shaders", outline.disableWithShaders, v -> outline.disableWithShaders = v),
			Ui.toggle(Ui.HALF, "glintsmith.outline.hand_with_shader_pack", outline.handWithShaderPack,
				v -> outline.handWithShaderPack = v));

		DurabilityAlert alert = config.durabilityAlert;
		list.addHeading(Ui.t("glintsmith.outline.durability"));
		list.addPair(
			Ui.toggle(Ui.HALF, "glintsmith.outline.durability.enabled", alert.enabled, v -> alert.enabled = v),
			SettingRows.slider(Ui.HALF, "glintsmith.outline.durability.threshold", 0.0f, 1.0f, 0.01f,
				SettingRows.percent("glintsmith.outline.durability.threshold"), alert.threshold, v -> alert.threshold = v));
		addAlertColorRow(alert);
	}

	private void addAlertColorRow(DurabilityAlert alert) {
		Minecraft minecraft = Minecraft.getInstance();
		HexBox hex = new HexBox(minecraft.font, COLOR_BOX_WIDTH, Ui.t("glintsmith.outline.durability.color"), false,
			text -> alert.color = text);
		hex.setValueSilently(alert.color);
		hex.setTooltip(Ui.tip("glintsmith.outline.durability.color"));
		Runnable open = () -> minecraft.setScreen(new ColorPickerScreen(minecraft.screen, Ui.parseOrGray(alert.color), picked -> {
			alert.color = Colors.toHex(picked);
			hex.setValueSilently(alert.color);
		}));
		ColorSwatch swatch = new ColorSwatch(Ui.HEIGHT, Ui.t("glintsmith.outline.durability.color"),
			() -> Ui.parseOrGray(alert.color), open);
		swatch.setTooltip(Ui.tip("glintsmith.outline.durability.color"));
		Button pick = Ui.button(COLOR_PICK_WIDTH, Ui.t("glintsmith.outline.durability.color"),
			Ui.tip("glintsmith.outline.durability.color"), button -> open.run());
		list.addRow(hex, swatch, pick);
	}

	@Override
	public void reset(GlintsmithConfig defaults) {
		config.outline = defaults.outline;
		config.durabilityAlert = defaults.durabilityAlert;
	}
}
