package navrelis.glintsmith.gui.widget;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoubleToIntFunction;

import navrelis.glintsmith.gui.Ui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * A slider whose track shows the colour gradient it controls (used by the colour picker).
 * The value is normalised to 0..1.
 */
public class GradientSlider extends AbstractSliderButton {
	private static final ResourceLocation HANDLE = ResourceLocation.withDefaultNamespace("widget/slider_handle");
	private static final ResourceLocation HANDLE_HIGHLIGHTED = ResourceLocation.withDefaultNamespace("widget/slider_handle_highlighted");

	private final DoubleFunction<Component> label;
	private final DoubleConsumer onChange;
	private final DoubleToIntFunction colorAt;

	/**
	 * @param colorAt ARGB of the track at a position 0..1; called every frame, so it may depend on live state
	 */
	public GradientSlider(int width, double initial, DoubleFunction<Component> label, DoubleConsumer onChange,
		DoubleToIntFunction colorAt) {
		super(0, 0, width, Ui.HEIGHT, Component.empty(), initial);
		this.label = label;
		this.onChange = onChange;
		this.colorAt = colorAt;
		updateMessage();
	}

	/** Moves the handle without notifying the listener. */
	public void setNormalized(double newValue) {
		this.value = Mth.clamp(newValue, 0.0, 1.0);
		updateMessage();
	}

	@Override
	protected void updateMessage() {
		setMessage(label.apply(value));
	}

	@Override
	protected void applyValue() {
		onChange.accept(value);
	}

	@Override
	public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		Minecraft minecraft = Minecraft.getInstance();
		int x = getX();
		int y = getY();
		boolean highlighted = isHoveredOrFocused();
		graphics.fill(x, y, x + width, y + height, highlighted ? 0xFFFFFFFF : 0xFF000000);
		int left = x + 1;
		int right = x + width - 1;
		for (int px = left; px < right; px += 2) {
			double t = (px + 1 - left) / (double) (right - left);
			graphics.fill(px, y + 1, Math.min(px + 2, right), y + height - 1, colorAt.applyAsInt(Mth.clamp(t, 0.0, 1.0)));
		}
		int handleX = x + (int) (value * (width - 8));
		graphics.blitSprite(highlighted ? HANDLE_HIGHLIGHTED : HANDLE, handleX, y, 8, height);
		graphics.drawCenteredString(minecraft.font, getMessage(), x + width / 2, y + (height - 8) / 2, active ? 0xFFFFFFFF : 0xFFA0A0A0);
	}
}
