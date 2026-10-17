package navrelis.glintsmith.gui.widget;

import java.util.Map;
import java.util.function.Supplier;

import navrelis.glintsmith.config.ConfigManager;
import navrelis.glintsmith.gui.Ui;
import navrelis.glintsmith.style.ColorEvaluator;
import navrelis.glintsmith.style.ColorStyle;
import navrelis.glintsmith.style.EnchantmentColors;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * A button that shows a colour style as a live, animated swatch and opens its editor when pressed.
 */
public class ColorStyleButton extends AbstractButton {
	private static final int SWATCH_WIDTH = 24;
	private static final int TEXT_MARGIN = SWATCH_WIDTH + 8;

	private final Component name;
	private final Supplier<ColorStyle> style;
	private final Runnable onPress;
	@Nullable
	private ColorStyle snapshot;
	private ColorEvaluator.Compiled compiled;

	public ColorStyleButton(int width, Component name, Supplier<ColorStyle> style, Runnable onPress) {
		super(0, 0, width, Ui.HEIGHT, Component.empty());
		this.name = name;
		this.style = style;
		this.onPress = onPress;
		refresh();
	}

	/** Updates the label (and the swatch) after the style changed. */
	public void refresh() {
		ColorStyle current = style.get();
		setMessage(Ui.optionLine(name, Ui.describe(current)));
		snapshot = current == null ? null : current.copy();
		compiled = ColorEvaluator.compile(current);
	}

	/**
	 * @return three sample enchantment colours (sharpness, unbreaking, mending) for previews of the
	 *         "enchantment colours" mode
	 */
	public static int[] sampleEnchantmentColors() {
		Map<String, String> overrides = ConfigManager.instance().get().enchantmentColors;
		return new int[] {
			EnchantmentColors.colorFor("minecraft:sharpness", overrides),
			EnchantmentColors.colorFor("minecraft:unbreaking", overrides),
			EnchantmentColors.colorFor("minecraft:mending", overrides)
		};
	}

	@Override
	public void onPress() {
		onPress.run();
	}

	@Override
	protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		ColorStyle current = style.get();
		if (current != null && !current.equals(snapshot)) {
			refresh();
		}
		super.renderWidget(graphics, mouseX, mouseY, partialTick);
		int argb = compiled.evaluate(Util.getMillis() / 1000.0, sampleEnchantmentColors());
		ColorSwatch.paint(graphics, getX() + 4, getY() + 4, SWATCH_WIDTH, getHeight() - 8, active ? argb : dim(argb), false);
	}

	private static int dim(int argb) {
		return 0xFF000000 | ((argb >> 16 & 0xFF) / 2) << 16 | ((argb >> 8 & 0xFF) / 2) << 8 | (argb & 0xFF) / 2;
	}

	@Override
	public void renderString(GuiGraphics graphics, Font font, int color) {
		renderScrollingString(graphics, font, TEXT_MARGIN, color);
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		defaultButtonNarrationText(output);
	}
}
