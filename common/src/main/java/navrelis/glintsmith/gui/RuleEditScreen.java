package navrelis.glintsmith.gui;

import java.util.List;
import java.util.function.Consumer;

import com.google.gson.Gson;
import navrelis.glintsmith.config.ConfigManager;
import navrelis.glintsmith.config.GlintsmithConfig;
import navrelis.glintsmith.gui.preview.PendingEdits;
import navrelis.glintsmith.gui.preview.PreviewHost;
import navrelis.glintsmith.gui.preview.PreviewSession;
import navrelis.glintsmith.gui.widget.OptionList.LabeledRow;
import navrelis.glintsmith.gui.widget.PatternBox;
import navrelis.glintsmith.style.ColorStyle;
import navrelis.glintsmith.style.GlintCategory;
import navrelis.glintsmith.style.GlintSettings;
import navrelis.glintsmith.style.OutlineSettings;
import navrelis.glintsmith.style.OutlineTarget;
import navrelis.glintsmith.style.Rule;
import navrelis.glintsmith.style.RuleEngine;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Edits one {@link Rule}: name, match conditions and the outline / glint overrides. Works on a copy; the caller's
 * {@code onDone} receives the edited rule when the player presses Done.
 */
public final class RuleEditScreen extends ConfigSubScreen implements PreviewHost {
	private static final Gson GSON = new Gson();
	/** The targets a rule can pick; {@code OTHER} is only reachable by leaving the list empty. */
	private static final List<OutlineTarget> TARGETS = List.of(OutlineTarget.FIRST_PERSON, OutlineTarget.THIRD_PERSON,
		OutlineTarget.ARMOR, OutlineTarget.DROPPED, OutlineTarget.ITEM_FRAME, OutlineTarget.GUI);

	private final Rule working;
	/** The rule as the caller handed it over: the one in the list that Done replaces, or a new rule not in it yet. */
	private final Rule original;
	@Nullable
	private final PreviewHost host;
	private final String baseline;
	private final Consumer<Rule> onDone;
	/** Overrides switched off again, kept so switching back on restores them. */
	private OutlineSettings outlineStash;
	private GlintSettings glintStash;

	public RuleEditScreen(Screen parent, Rule rule, Consumer<Rule> onDone) {
		super(parent, Ui.t("glintsmith.rule.title"));
		this.original = rule;
		this.host = PreviewHost.of(parent);
		this.working = rule.copy().sanitize();
		this.baseline = snapshot(working);
		this.onDone = onDone;
	}

	private static String snapshot(Rule rule) {
		return GSON.toJson(rule.copy().sanitize());
	}

	// ---- preview ------------------------------------------------------------------------------------------

	@Nullable
	@Override
	protected PreviewHost previewHost() {
		return host == null ? null : this;
	}

	@Override
	public PreviewSession previewSession() {
		if (host == null) {
			throw new IllegalStateException("no preview host");
		}
		return host.previewSession();
	}

	/** The rule being edited stands in for its original (or is added) in the config the preview sees. */
	@Override
	public void withPendingEdits(Consumer<GlintsmithConfig> action) {
		if (host != null) {
			host.withPendingEdits(cfg -> PendingEdits.withRule(cfg, original, working, () -> action.accept(cfg)));
		}
	}

	@Nullable
	@Override
	public Component previewNote(ItemStack shown) {
		if (!working.targets.isEmpty() && !working.targets.contains(OutlineTarget.FIRST_PERSON)) {
			return Ui.t("glintsmith.preview.note.rule_not_first_person");
		}
		if (!working.copy().sanitize().matches(RuleEngine.factsOf(shown), OutlineTarget.FIRST_PERSON)) {
			return Ui.t("glintsmith.preview.note.rule_no_match");
		}
		return null;
	}

	@Override
	protected void fillRows() {
		addConditionRows();
		addTargetRows();
		addOutlineRows();
		addGlintRows();
	}

	// ---- match conditions ---------------------------------------------------------------------------------

	private void addConditionRows() {
		EditBox name = new EditBox(font, 0, 0, LabeledRow.WIDGET_WIDTH, Ui.HEIGHT, Ui.t("glintsmith.rule.name"));
		name.setMaxLength(64);
		name.setHint(Ui.t("glintsmith.rule.name.hint"));
		name.setValue(working.name == null ? "" : working.name);
		name.setResponder(text -> working.name = text);
		name.setTooltip(Ui.tip("glintsmith.rule.name"));
		list.add(new LabeledRow(Ui.t("glintsmith.rule.name"), name));

		list.addPair(
			Ui.toggle(Ui.HALF, "glintsmith.rule.enabled", working.enabled, v -> working.enabled = v),
			Ui.cycle(Ui.HALF, "glintsmith.rule.condition", "glintsmith.rule.condition", Rule.Condition.values(),
				working.enchantedCondition, true, v -> working.enchantedCondition = v));

		list.add(patternRow("glintsmith.rule.items", working.items, v -> working.items = v));
		list.add(patternRow("glintsmith.rule.exclude", working.excludeItems, v -> working.excludeItems = v));
		list.add(patternRow("glintsmith.rule.enchantments", working.enchantments, v -> working.enchantments = v));
		list.addHelp(Ui.t("glintsmith.rule.patterns.help"));
	}

	private LabeledRow patternRow(String key, List<String> value, Consumer<List<String>> setter) {
		PatternBox box = new PatternBox(font, LabeledRow.WIDGET_WIDTH, Ui.t(key), value, setter);
		box.setTooltip(Ui.tipOf(key + ".tooltip", "glintsmith.rule.syntax"));
		return new LabeledRow(Ui.t(key), box);
	}

	private void addTargetRows() {
		list.addHeading(Ui.t("glintsmith.rule.targets"));
		for (int i = 0; i < TARGETS.size(); i += 2) {
			list.addPair(targetToggle(TARGETS.get(i)), targetToggle(TARGETS.get(i + 1)));
		}
	}

	private AbstractWidget targetToggle(OutlineTarget target) {
		return Ui.toggle(Ui.HALF, "glintsmith.rule.target." + Ui.lower(target), working.targets.contains(target), on -> {
			working.targets.remove(target);
			if (on) {
				working.targets.add(target);
			}
		});
	}

	// ---- outline override ---------------------------------------------------------------------------------

	private void addOutlineRows() {
		GlintsmithConfig.Outline global = ConfigManager.instance().get().outline;
		list.addHeading(Ui.t("glintsmith.rule.outline"));
		list.addFull(Ui.toggle(Ui.WIDTH, "glintsmith.rule.outline.override", working.outline != null, on -> {
			if (on) {
				working.outline = outlineStash != null ? outlineStash : new OutlineSettings();
			} else {
				outlineStash = working.outline;
				working.outline = null;
			}
			refreshRows();
		}));
		OutlineSettings outline = working.outline;
		if (outline == null) {
			return;
		}
		SettingRows.triRow(list, "glintsmith.rule.outline.enabled", outline.enabled, v -> outline.enabled = v);
		SettingRows.optionalStyleRow(list, "glintsmith.rule.outline.color", () -> outline.color, global.color,
			c -> outline.color = c);
		SettingRows.optionalSliderRow(list, "glintsmith.rule.outline.thickness", OutlineSettings.MIN_THICKNESS,
			OutlineSettings.MAX_THICKNESS, 0.1f, SettingRows.decimals("glintsmith.rule.outline.thickness", 1),
			outline.thickness, global.thickness, v -> outline.thickness = v);
		SettingRows.optionalSliderRow(list, "glintsmith.rule.outline.softness", 0.0f, 1.0f, 0.01f,
			SettingRows.percent("glintsmith.rule.outline.softness"), outline.softness, global.softness,
			v -> outline.softness = v);
		SettingRows.optionalSliderRow(list, "glintsmith.rule.outline.opacity", 0.0f, 1.0f, 0.01f,
			SettingRows.percent("glintsmith.rule.outline.opacity"), outline.opacity, global.opacity,
			v -> outline.opacity = v);
	}

	// ---- glint override -----------------------------------------------------------------------------------

	private void addGlintRows() {
		GlintsmithConfig.GlintCategorySettings vanilla = ConfigManager.instance().get().glint.forCategory(GlintCategory.ITEM);
		list.addHeading(Ui.t("glintsmith.rule.glint"));
		list.addFull(Ui.toggle(Ui.WIDTH, "glintsmith.rule.glint.override", working.glint != null, on -> {
			if (on) {
				working.glint = glintStash != null ? glintStash : new GlintSettings();
			} else {
				glintStash = working.glint;
				working.glint = null;
			}
			refreshRows();
		}));
		GlintSettings glint = working.glint;
		if (glint == null) {
			return;
		}
		SettingRows.triRow(list, "glintsmith.rule.glint.enabled", glint.enabled, v -> glint.enabled = v);
		list.addHelp(Ui.t("glintsmith.glint.texture"));
		SettingRows.textureRow(list, GlintSettings.VANILLA_TEXTURE, Ui.t("glintsmith.rule.glint.texture.hint"),
			glint.texture, text -> glint.texture = text);
		SettingRows.optionalStyleRow(list, "glintsmith.rule.glint.tint", () -> glint.tint,
			ColorStyle.of(ColorStyle.DEFAULT_COLOR), c -> glint.tint = c);
		SettingRows.optionalSliderRow(list, "glintsmith.rule.glint.speed", 0.0f, 10.0f, 0.05f,
			SettingRows.decimals("glintsmith.rule.glint.speed", 2), glint.speed, 1.0f, v -> glint.speed = v);
		SettingRows.optionalSliderRow(list, "glintsmith.rule.glint.scale", 0.1f, 10.0f, 0.05f,
			SettingRows.decimals("glintsmith.rule.glint.scale", 2), glint.scale, 1.0f, v -> glint.scale = v);
		SettingRows.optionalSliderRow(list, "glintsmith.rule.glint.strength", 0.0f, 1.0f, 0.01f,
			SettingRows.percent("glintsmith.rule.glint.strength"), glint.strength, vanilla.strength,
			v -> glint.strength = v);
		SettingRows.optionalSliderRow(list, "glintsmith.rule.glint.rotation", -180.0f, 180.0f, 1.0f,
			SettingRows.whole("glintsmith.rule.glint.rotation"), glint.rotation, vanilla.rotation,
			v -> glint.rotation = v);
	}

	// ---- leaving ------------------------------------------------------------------------------------------

	@Override
	protected void done() {
		onDone.accept(working.sanitize());
		minecraft.setScreen(parent);
	}

	@Override
	protected void cancel() {
		if (snapshot(working).equals(baseline)) {
			minecraft.setScreen(parent);
			return;
		}
		minecraft.setScreen(new ConfirmScreen(discard -> minecraft.setScreen(discard ? parent : this),
			Ui.t("glintsmith.confirm.discard.title"), Ui.t("glintsmith.confirm.discard.rule"),
			Ui.t("glintsmith.confirm.discard.yes"), Component.translatable("gui.back")));
	}
}
