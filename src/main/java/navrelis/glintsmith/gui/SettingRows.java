package navrelis.glintsmith.gui;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import navrelis.glintsmith.gui.widget.ColorStyleButton;
import navrelis.glintsmith.gui.widget.FloatSlider;
import navrelis.glintsmith.gui.widget.OptionList;
import navrelis.glintsmith.style.ColorStyle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * Builders for the option rows that appear in several places: the global tabs edit plain values, the rule editor
 * edits optional values that can be "inherited".
 */
public final class SettingRows {
	/** Width of the "Inherit / Custom" button in front of optional rows. */
	public static final int INHERIT_WIDTH = 74;

	private SettingRows() {
	}

	/** Three state value of an optional boolean. */
	public enum Tri {
		INHERIT,
		ON,
		OFF;

		public static Tri of(@Nullable Boolean value) {
			return value == null ? INHERIT : value ? ON : OFF;
		}

		@Nullable
		public Boolean value() {
			return this == INHERIT ? null : this == ON;
		}
	}

	// ---- label formats ---------------------------------------------------------------------------------------

	/** "Label: 2.0 px" style: the key takes the number as its only argument. */
	public static Function<Float, Component> decimals(String key, int digits) {
		return v -> Ui.t(key, Ui.decimals(v, digits));
	}

	/** "Label: 35 %" style. */
	public static Function<Float, Component> percent(String key) {
		return v -> Ui.t(key, Ui.percent(v));
	}

	/** "Label: 10 degrees" style, rounded to whole degrees. */
	public static Function<Float, Component> whole(String key) {
		return v -> Ui.t(key, Math.round(v));
	}

	/** "Label: 24 blocks" style; 0 shows "Off". The key takes the value text as its only argument. */
	public static Function<Float, Component> blocksOrOff(String key) {
		return v -> Ui.t(key, v <= 0.0f ? Ui.t("glintsmith.common.off") : Ui.t("glintsmith.common.blocks", Math.round(v)));
	}

	// ---- plain rows -----------------------------------------------------------------------------------------

	public static FloatSlider slider(int width, String key, float min, float max, float step,
		Function<Float, Component> label, float value, Consumer<Float> setter) {
		FloatSlider slider = new FloatSlider(width, min, max, step, value, label, setter);
		slider.setTooltip(Ui.tip(key));
		return slider;
	}

	public static void sliderRow(OptionList list, String key, float min, float max, float step,
		Function<Float, Component> label, float value, Consumer<Float> setter) {
		list.addFull(slider(Ui.WIDTH, key, min, max, step, label, value, setter));
	}

	/** A full width colour style button that opens the style editor. */
	public static ColorStyleButton styleButton(int width, String key, Supplier<ColorStyle> get, Consumer<ColorStyle> set) {
		return styleButton(width, key, get, get, set);
	}

	/**
	 * A full width colour style button that opens the style editor.
	 *
	 * @param shown the style the button and the editor start from (may be an inherited stand-in)
	 * @param raw the style actually stored, which {@code set} replaces; the preview swaps the edit in through it
	 */
	public static ColorStyleButton styleButton(int width, String key, Supplier<ColorStyle> shown, Supplier<ColorStyle> raw,
		Consumer<ColorStyle> set) {
		ColorStyleButton[] self = new ColorStyleButton[1];
		self[0] = new ColorStyleButton(width, Ui.t(key), shown, () -> {
			Minecraft minecraft = Minecraft.getInstance();
			minecraft.setScreen(new ColorStyleScreen(minecraft.screen, shown.get().copy(), result -> {
				set.accept(result);
				self[0].refresh();
			}, raw, set));
		});
		self[0].setTooltip(Ui.tip(key));
		return self[0];
	}

	/**
	 * The texture row: a text box, previous / next preset buttons and a "Vanilla" button.
	 *
	 * @param vanillaValue what the "Vanilla" button writes into the box ({@code ""} or {@code "vanilla"})
	 * @param hint grey text shown while the box is empty
	 * @param current the stored texture, null for none
	 * @param setter receives the trimmed text, or null when the box is empty
	 */
	public static void textureRow(OptionList list, String vanillaValue, Component hint, @Nullable String current,
		Consumer<String> setter) {
		Minecraft minecraft = Minecraft.getInstance();
		int arrow = Ui.HEIGHT;
		int vanillaWidth = 52;
		int boxWidth = Ui.WIDTH - 2 * arrow - vanillaWidth - 3 * Ui.GAP;
		EditBox box = new EditBox(minecraft.font, 0, 0, boxWidth, Ui.HEIGHT, Ui.t("glintsmith.glint.texture"));
		box.setMaxLength(256);
		box.setHint(hint);
		box.setValue(current == null ? "" : current);
		box.setTextColor(TextureNames.isValid(box.getValue()) ? EditBox.DEFAULT_TEXT_COLOR : Ui.ERROR_RED);
		box.setResponder(text -> {
			boolean valid = TextureNames.isValid(text);
			box.setTextColor(valid ? EditBox.DEFAULT_TEXT_COLOR : Ui.ERROR_RED);
			String trimmed = text.trim();
			setter.accept(trimmed.isEmpty() ? null : trimmed);
		});
		box.setTooltip(Ui.tip("glintsmith.glint.texture"));

		Button previous = Ui.button(arrow, Component.literal("◀"), Ui.tip("glintsmith.glint.texture.previous"),
			button -> stepPreset(box, -1));
		Button next = Ui.button(arrow, Component.literal("▶"), Ui.tip("glintsmith.glint.texture.next"),
			button -> stepPreset(box, 1));
		Button vanilla = Ui.button(vanillaWidth, Ui.t("glintsmith.glint.texture.vanilla"),
			Ui.tip("glintsmith.glint.texture.vanilla"), button -> box.setValue(vanillaValue));
		list.addRow(box, previous, next, vanilla);
	}

	private static void stepPreset(EditBox box, int direction) {
		List<String> presets = GuiHooks.texturePresets.get();
		if (presets.isEmpty()) {
			return;
		}
		int index = presets.indexOf(box.getValue().trim());
		int target;
		if (index < 0) {
			target = direction > 0 ? 0 : presets.size() - 1;
		} else {
			target = Math.floorMod(index + direction, presets.size());
		}
		box.setValue(presets.get(target));
	}

	// ---- optional (inheriting) rows -------------------------------------------------------------------------

	/** An Inherit / On / Off button for an optional boolean. */
	public static void triRow(OptionList list, String key, @Nullable Boolean current, Consumer<Boolean> setter) {
		list.addFull(Ui.cycle(Ui.WIDTH, key, "glintsmith.common.tri", Tri.values(), Tri.of(current), false,
			tri -> setter.accept(tri.value())));
	}

	/**
	 * A slider that can be switched to "inherit" (value null).
	 *
	 * @param shownWhenInherited where the greyed out slider sits while the value is inherited
	 */
	public static void optionalSliderRow(OptionList list, String key, float min, float max, float step,
		Function<Float, Component> label, @Nullable Float current, float shownWhenInherited,
		Consumer<Float> setter) {
		boolean custom = current != null;
		FloatSlider slider = slider(Ui.WIDTH - INHERIT_WIDTH - Ui.GAP, key, min, max, step, label,
			custom ? current : shownWhenInherited, setter);
		slider.active = custom;
		CycleButton<Boolean> toggle = Ui.inheritToggle(INHERIT_WIDTH, custom, nowCustom -> {
			slider.active = nowCustom;
			setter.accept(nowCustom ? slider.floatValue() : null);
		});
		list.addRow(toggle, slider);
	}

	/**
	 * A toggle and a slider on one row: the value is null (taken from elsewhere) while the toggle is off, and the slider
	 * only works while it is on.
	 *
	 * @param key base key of the toggle; its tooltip is {@code key.tooltip}
	 * @param toggleLabel the toggle text for the given state (the place name is part of it)
	 * @param sliderKey key of the slider, which has its own tooltip
	 * @param shownWhenOff supplies the value the greyed out slider shows, and the value it starts from when switched on
	 */
	public static void toggledSliderRow(OptionList list, int toggleWidth, String key, Function<Boolean, Component> toggleLabel,
		String sliderKey, float min, float max, float step, Function<Float, Component> label, @Nullable Float current,
		Supplier<Float> shownWhenOff, Consumer<Float> setter) {
		boolean own = current != null;
		FloatSlider slider = slider(Ui.WIDTH - toggleWidth - Ui.GAP, sliderKey, min, max, step, label,
			own ? current : shownWhenOff.get(), setter);
		slider.active = own;
		CycleButton<Boolean> toggle = CycleButton.<Boolean>builder(toggleLabel)
			.withValues(List.of(false, true))
			.withInitialValue(own)
			.displayOnlyValue()
			.withTooltip(v -> Ui.tip(key))
			.create(0, 0, toggleWidth, Ui.HEIGHT, Component.empty(), (button, nowOwn) -> {
				slider.active = nowOwn;
				if (nowOwn) {
					slider.setFloatValue(shownWhenOff.get());
				}
				setter.accept(nowOwn ? slider.floatValue() : null);
			});
		list.addRow(toggle, slider);
	}

	/**
	 * A colour style button that can be switched to "inherit" (style null).
	 *
	 * @param shownWhenInherited the style shown, greyed out, while the value is inherited
	 */
	public static void optionalStyleRow(OptionList list, String key, Supplier<ColorStyle> get,
		ColorStyle shownWhenInherited, Consumer<ColorStyle> setter) {
		boolean custom = get.get() != null;
		ColorStyleButton button = styleButton(Ui.WIDTH - INHERIT_WIDTH - Ui.GAP, key,
			() -> get.get() != null ? get.get() : shownWhenInherited, get, setter);
		button.active = custom;
		CycleButton<Boolean> toggle = Ui.inheritToggle(INHERIT_WIDTH, custom, nowCustom -> {
			button.active = nowCustom;
			setter.accept(nowCustom ? shownWhenInherited.copy() : null);
			button.refresh();
		});
		list.addRow(toggle, button);
	}
}
