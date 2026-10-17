package navrelis.glintsmith.gui.widget;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import navrelis.glintsmith.gui.PatternText;
import navrelis.glintsmith.gui.Ui;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

/**
 * A text box for a comma-separated list of item or enchantment patterns. Invalid entries are drawn red and the
 * box gets a red border; the list is reported on every change, invalid entries included (the rule shows its error).
 */
public class PatternBox extends EditBox {
	private static final Style INVALID = Style.EMPTY.withColor(ChatFormatting.RED);

	public PatternBox(Font font, int width, Component label, List<String> value, Consumer<List<String>> setter) {
		super(font, 0, 0, width, Ui.HEIGHT, label);
		setMaxLength(2048);
		setFormatter((piece, offset) -> format(piece, offset, getValue()));
		setValue(PatternText.join(value));
		setResponder(text -> setter.accept(PatternText.split(text)));
	}

	private static FormattedCharSequence format(String piece, int offset, String full) {
		List<int[]> bad = PatternText.invalidRanges(full);
		if (bad.isEmpty() || piece.isEmpty()) {
			return FormattedCharSequence.forward(piece, Style.EMPTY);
		}
		List<FormattedCharSequence> parts = new ArrayList<>();
		int runStart = 0;
		boolean runBad = isBad(bad, offset);
		for (int i = 1; i <= piece.length(); i++) {
			boolean now = i < piece.length() && isBad(bad, offset + i);
			if (i == piece.length() || now != runBad) {
				parts.add(FormattedCharSequence.forward(piece.substring(runStart, i), runBad ? INVALID : Style.EMPTY));
				runStart = i;
				runBad = now;
			}
		}
		return FormattedCharSequence.composite(parts);
	}

	private static boolean isBad(List<int[]> ranges, int index) {
		for (int[] range : ranges) {
			if (index >= range[0] && index < range[1]) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.renderWidget(graphics, mouseX, mouseY, partialTick);
		if (!PatternText.allValid(getValue())) {
			graphics.renderOutline(getX(), getY(), getWidth(), getHeight(), Ui.ERROR_RED);
		}
	}
}
