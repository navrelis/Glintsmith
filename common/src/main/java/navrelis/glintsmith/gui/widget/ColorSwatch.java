package navrelis.glintsmith.gui.widget;

import java.util.function.IntSupplier;

import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * A small square showing a colour. Clickable (focusable, with a hover frame) when it has an action,
 * otherwise purely decorative.
 */
public class ColorSwatch extends AbstractWidget {
	private final IntSupplier color;
	@Nullable
	private final Runnable action;

	public ColorSwatch(int size, Component narration, IntSupplier color, @Nullable Runnable action) {
		super(0, 0, size, size, narration);
		this.color = color;
		this.action = action;
	}

	/**
	 * Draws a colour rectangle with a frame: black, or white when {@code highlight} is set.
	 * Translucent colours are drawn over a grey backing.
	 */
	public static void paint(GuiGraphics graphics, int x, int y, int width, int height, int argb, boolean highlight) {
		graphics.fill(x, y, x + width, y + height, highlight ? 0xFFFFFFFF : 0xFF000000);
		if ((argb >>> 24) < 255) {
			graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF808080);
		}
		graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, argb);
	}

	@Override
	protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		boolean highlight = action != null && isHoveredOrFocused();
		paint(graphics, getX(), getY(), width, height, color.getAsInt(), highlight);
	}

	@Override
	public void onClick(double mouseX, double mouseY) {
		if (action != null) {
			action.run();
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		return action != null && super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (action != null && active && visible && (keyCode == 257 || keyCode == 32 || keyCode == 335)) {
			action.run();
			return true;
		}
		return false;
	}

	@Nullable
	@Override
	public ComponentPath nextFocusPath(FocusNavigationEvent event) {
		return action == null ? null : super.nextFocusPath(event);
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		defaultButtonNarrationText(output);
	}
}
