package navrelis.glintsmith.gui.tab;

import navrelis.glintsmith.config.GapFixMode;
import navrelis.glintsmith.config.GlintsmithConfig;
import navrelis.glintsmith.gui.SettingRows;
import navrelis.glintsmith.gui.Ui;
import navrelis.glintsmith.gui.widget.FloatSlider;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;

/**
 * The glint gap fix: automatic or hand tuned model parameters.
 */
public final class ModelFixTab extends ConfigTab {
	private static final float SIDE_LIMIT = 0.1f;

	private CycleButton<GapFixMode> modeButton;
	private AbstractWidget[] customWidgets = new AbstractWidget[0];

	public ModelFixTab(GlintsmithConfig config) {
		super(config, Ui.t("glintsmith.tab.modelfix"));
	}

	@Override
	protected void fill() {
		GlintsmithConfig.GapFix fix = config.gapFix;

		list.addHeading(Ui.t("glintsmith.modelfix.heading"));
		modeButton = Ui.cycle(Ui.HALF, "glintsmith.modelfix.mode", "glintsmith.modelfix.mode", GapFixMode.values(),
			fix.mode, true, mode -> {
				fix.mode = mode;
				updateActive();
			});
		list.addPair(
			Ui.toggle(Ui.HALF, "glintsmith.modelfix.enabled", fix.enabled, v -> {
				fix.enabled = v;
				updateActive();
			}),
			modeButton);

		FloatSlider shrink = SettingRows.slider(Ui.WIDTH, "glintsmith.modelfix.uv_shrink", 0.0f, 1.0f, 0.01f,
			SettingRows.decimals("glintsmith.modelfix.uv_shrink", 2), fix.uvShrinkMultiplier, v -> fix.uvShrinkMultiplier = v);
		FloatSlider expansion = SettingRows.slider(Ui.WIDTH, "glintsmith.modelfix.side_expansion", -SIDE_LIMIT, SIDE_LIMIT,
			0.0001f, SettingRows.decimals("glintsmith.modelfix.side_expansion", 4), fix.sideExpansion, v -> fix.sideExpansion = v);
		FloatSlider indent = SettingRows.slider(Ui.WIDTH, "glintsmith.modelfix.side_indent", -SIDE_LIMIT, SIDE_LIMIT,
			0.0001f, SettingRows.decimals("glintsmith.modelfix.side_indent", 4), fix.sideIndent, v -> fix.sideIndent = v);
		AbstractWidget precise = Ui.toggle(Ui.WIDTH, "glintsmith.modelfix.precise_spans", fix.preciseSpans,
			v -> fix.preciseSpans = v);
		list.addFull(shrink);
		list.addFull(expansion);
		list.addFull(indent);
		list.addFull(precise);
		customWidgets = new AbstractWidget[] {shrink, expansion, indent, precise};
		updateActive();

		list.addHelp(Ui.t("glintsmith.modelfix.help.rebuild"));
		list.addHelp(Ui.t("glintsmith.modelfix.help.custom"));
	}

	/** Greys out what the current switches make irrelevant. */
	private void updateActive() {
		GlintsmithConfig.GapFix fix = config.gapFix;
		modeButton.active = fix.enabled;
		boolean custom = fix.enabled && fix.mode == GapFixMode.CUSTOM;
		for (AbstractWidget widget : customWidgets) {
			widget.active = custom;
		}
	}

	@Override
	public void reset(GlintsmithConfig defaults) {
		config.gapFix = defaults.gapFix;
	}
}
