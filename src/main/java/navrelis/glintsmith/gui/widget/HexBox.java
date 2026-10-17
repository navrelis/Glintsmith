package navrelis.glintsmith.gui.widget;

import java.util.function.Consumer;

import navrelis.glintsmith.gui.Ui;
import navrelis.glintsmith.style.Colors;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

/**
 * A text box for {@code #RRGGBB} colours. Invalid text turns red and is not reported; valid text is reported
 * as typed (trimmed). With {@code allowEmpty} an empty box is valid and reported as an empty string.
 */
public class HexBox extends EditBox {
	private final boolean allowEmpty;
	private final Consumer<String> onValid;
	private boolean valid = true;
	private boolean silent;

	public HexBox(Font font, int width, Component label, boolean allowEmpty, Consumer<String> onValid) {
		super(font, 0, 0, width, Ui.HEIGHT, label);
		this.allowEmpty = allowEmpty;
		this.onValid = onValid;
		setMaxLength(9);
		setResponder(this::changed);
	}

	private void changed(String text) {
		String trimmed = text.trim();
		valid = trimmed.isEmpty() ? allowEmpty : Colors.isValidHex(trimmed);
		setTextColor(valid ? DEFAULT_TEXT_COLOR : Ui.ERROR_RED);
		if (valid && !silent) {
			onValid.accept(trimmed);
		}
	}

	/** Sets the text without reporting it. */
	public void setValueSilently(String text) {
		silent = true;
		try {
			setValue(text);
		} finally {
			silent = false;
		}
	}

	public boolean isValidText() {
		return valid;
	}

	@Override
	public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.renderWidget(graphics, mouseX, mouseY, partialTick);
		if (!valid) {
			graphics.renderOutline(getX(), getY(), getWidth(), getHeight(), Ui.ERROR_RED);
		}
	}
}
