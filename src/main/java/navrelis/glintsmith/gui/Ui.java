package navrelis.glintsmith.gui;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import navrelis.glintsmith.style.ColorMode;
import navrelis.glintsmith.style.ColorStyle;
import navrelis.glintsmith.style.Colors;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

/**
 * Small widget and text factories shared by all Glintsmith screens.
 * <p>
 * Convention: a widget labelled with the translation key {@code K} has its tooltip in {@code K.tooltip}.
 */
public final class Ui {
	/** Width of a full option row. */
	public static final int WIDTH = 310;
	/** Width of a half row button. */
	public static final int HALF = 150;
	/** Gap between the two buttons of a pair row (vanilla uses 10). */
	public static final int PAIR_GAP = 10;
	/** Gap between the parts of any other row. */
	public static final int GAP = 4;
	public static final int HEIGHT = 20;
	public static final int ERROR_RED = 0xFFFF5555;
	public static final int GRAY = 0xFFA0A0A0;
	public static final int WHITE = 0xFFFFFFFF;
	public static final int HEADING = 0xFFFFD27F;

	private Ui() {
	}

	public static MutableComponent t(String key, Object... args) {
		return Component.translatable(key, args);
	}

	/**
	 * @return the tooltip of the label key ({@code key + ".tooltip"}), or null if the language has none
	 */
	@Nullable
	public static Tooltip tip(String key) {
		String tooltipKey = key + ".tooltip";
		return I18n.exists(tooltipKey) ? Tooltip.create(Component.translatable(tooltipKey)) : null;
	}

	/**
	 * @return a tooltip made of several translation keys, separated by a blank line
	 */
	public static Tooltip tipOf(String... keys) {
		MutableComponent all = Component.empty();
		for (int i = 0; i < keys.length; i++) {
			if (i > 0) {
				all.append("\n\n");
			}
			all.append(Component.translatable(keys[i]));
		}
		return Tooltip.create(all);
	}

	public static String lower(Enum<?> value) {
		return value.name().toLowerCase(Locale.ROOT);
	}

	/**
	 * An on/off button "Label: ON".
	 */
	public static CycleButton<Boolean> toggle(int width, String key, boolean value, Consumer<Boolean> setter) {
		return CycleButton.onOffBuilder(value)
			.withTooltip(v -> tip(key))
			.create(0, 0, width, HEIGHT, t(key), (button, v) -> setter.accept(v));
	}

	/**
	 * A button cycling through the constants of an enum. Value labels are {@code valuePrefix.<name>}; with
	 * {@code valueTips} each value also has its own tooltip paragraph in {@code valuePrefix.<name>.tooltip}.
	 */
	public static <E extends Enum<E>> CycleButton<E> cycle(int width, String key, String valuePrefix, E[] values,
		E current, boolean valueTips, Consumer<E> setter) {
		return CycleButton.<E>builder(v -> t(valuePrefix + "." + lower(v)))
			.withValues(values)
			.withInitialValue(current)
			.withTooltip(v -> {
				boolean own = I18n.exists(key + ".tooltip");
				if (valueTips && own) {
					return tipOf(key + ".tooltip", valuePrefix + "." + lower(v) + ".tooltip");
				}
				if (valueTips) {
					return tipOf(valuePrefix + "." + lower(v) + ".tooltip");
				}
				return tip(key);
			})
			.create(0, 0, width, HEIGHT, t(key), (button, v) -> setter.accept(v));
	}

	/**
	 * The "Inherit / Custom" button in front of an optional rule override. {@code custom} true means a value is set.
	 */
	public static CycleButton<Boolean> inheritToggle(int width, boolean custom, Consumer<Boolean> setter) {
		return CycleButton.<Boolean>builder(v -> t(v ? "glintsmith.common.custom" : "glintsmith.common.inherit"))
			.withValues(List.of(false, true))
			.withInitialValue(custom)
			.displayOnlyValue()
			.withTooltip(v -> tip("glintsmith.common.inherit"))
			.create(0, 0, width, HEIGHT, Component.empty(), (button, v) -> setter.accept(v));
	}

	public static Button button(int width, Component label, @Nullable Tooltip tooltip, Button.OnPress onPress) {
		return Button.builder(label, onPress).size(width, HEIGHT).tooltip(tooltip).build();
	}

	/**
	 * @return the short description of a colour style for button labels
	 */
	public static Component describe(@Nullable ColorStyle style) {
		if (style == null) {
			return t("glintsmith.common.none");
		}
		ColorMode mode = style.mode == null ? ColorMode.STATIC : style.mode;
		if (mode == ColorMode.STATIC) {
			String hex = style.colors == null || style.colors.isEmpty() ? ColorStyle.DEFAULT_COLOR : style.colors.get(0);
			return Component.literal(hex);
		}
		MutableComponent name = t("glintsmith.color.mode." + lower(mode));
		if ((mode == ColorMode.GRADIENT || mode == ColorMode.PULSE) && style.colors != null && style.colors.size() > 1) {
			name.append(" (" + style.colors.size() + ")");
		}
		return name;
	}

	/**
	 * @return {@code Label: value} in the vanilla option style
	 */
	public static Component optionLine(Component name, Component value) {
		return CommonComponents.optionNameValue(name, value);
	}

	/**
	 * @return the colour as ARGB, or grey if the text is not a valid hex colour
	 */
	public static int parseOrGray(String hex) {
		try {
			return Colors.parseHex(hex);
		} catch (IllegalArgumentException e) {
			return 0xFF808080;
		}
	}

	public static String decimals(float value, int digits) {
		return String.format(Locale.ROOT, "%." + digits + "f", value);
	}

	public static String percent(float fraction) {
		return Integer.toString(Math.round(fraction * 100.0f));
	}
}
