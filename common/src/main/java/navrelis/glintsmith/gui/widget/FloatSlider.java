package navrelis.glintsmith.gui.widget;

import java.util.function.Consumer;
import java.util.function.Function;

import navrelis.glintsmith.gui.Ui;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * A vanilla-looking slider for a float range. The handle moves smoothly, the value handed to the setter is rounded to
 * {@code step} (so keyboard arrows, which move the handle one pixel, still reach every value).
 */
public class FloatSlider extends AbstractSliderButton {
	private final float min;
	private final float max;
	private final float step;
	private final Function<Float, Component> label;
	private final Consumer<Float> setter;

	/**
	 * @param step rounding step of the value, 0 for none
	 * @param label builds the button text from the (rounded) value
	 */
	public FloatSlider(int width, float min, float max, float step, float initial, Function<Float, Component> label,
		Consumer<Float> setter) {
		super(0, 0, width, Ui.HEIGHT, Component.empty(), toNormalized(initial, min, max));
		this.min = min;
		this.max = max;
		this.step = step;
		this.label = label;
		this.setter = setter;
		updateMessage();
	}

	private static double toNormalized(float value, float min, float max) {
		return max <= min ? 0.0 : Mth.clamp((value - min) / (double) (max - min), 0.0, 1.0);
	}

	/** @return the current value, rounded to the step and clamped to the range */
	public float floatValue() {
		double real = min + value * (max - min);
		if (step > 0.0f) {
			real = Math.round((real - min) / step) * (double) step + min;
		}
		return (float) Mth.clamp(real, min, max);
	}

	/** Moves the handle to the value without calling the setter. */
	public void setFloatValue(float newValue) {
		value = toNormalized(newValue, min, max);
		updateMessage();
	}

	@Override
	protected void updateMessage() {
		setMessage(label.apply(floatValue()));
	}

	@Override
	protected void applyValue() {
		setter.accept(floatValue());
	}
}
