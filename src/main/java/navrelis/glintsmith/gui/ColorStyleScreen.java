package navrelis.glintsmith.gui;

import java.util.function.Consumer;
import java.util.function.Supplier;

import navrelis.glintsmith.config.GlintsmithConfig;
import navrelis.glintsmith.gui.preview.PendingEdits;
import navrelis.glintsmith.gui.preview.PreviewHost;
import navrelis.glintsmith.gui.preview.PreviewSession;
import navrelis.glintsmith.gui.widget.ColorStyleButton;
import navrelis.glintsmith.gui.widget.ColorSwatch;
import navrelis.glintsmith.gui.widget.FloatSlider;
import navrelis.glintsmith.gui.widget.HexBox;
import navrelis.glintsmith.style.ColorEvaluator;
import navrelis.glintsmith.style.ColorMode;
import navrelis.glintsmith.style.ColorStyle;
import navrelis.glintsmith.style.Colors;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Edits a {@link ColorStyle}: mode, colours, speed, saturation and brightness, with a live animated preview.
 * The edit works on a copy; {@code onDone} receives the result when the player presses Done.
 */
public final class ColorStyleScreen extends ConfigSubScreen implements PreviewHost {
	/** Seconds of colour history shown across the preview bar. */
	private static final double PREVIEW_WINDOW = 4.0;
	private static final int PICK_WIDTH = 128;

	private final ColorStyle working;
	private final Consumer<ColorStyle> onDone;
	@Nullable
	private final PreviewHost host;
	/** Read and write the style this edit will replace, so the preview can show the edit before Done; may be null. */
	@Nullable
	private final Supplier<ColorStyle> rawGet;
	@Nullable
	private final Consumer<ColorStyle> rawSet;

	public ColorStyleScreen(Screen parent, ColorStyle style, Consumer<ColorStyle> onDone) {
		this(parent, style, onDone, null, null);
	}

	/**
	 * @param rawGet reads the stored style that {@code onDone} replaces; null if the preview cannot show the edit
	 * @param rawSet writes it
	 */
	public ColorStyleScreen(Screen parent, ColorStyle style, Consumer<ColorStyle> onDone,
		@Nullable Supplier<ColorStyle> rawGet, @Nullable Consumer<ColorStyle> rawSet) {
		super(parent, Ui.t("glintsmith.color.title"));
		this.working = style.copy().sanitize();
		this.onDone = onDone;
		this.host = PreviewHost.of(parent);
		this.rawGet = rawGet;
		this.rawSet = rawSet;
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

	@Override
	public void withPendingEdits(Consumer<GlintsmithConfig> action) {
		if (host == null) {
			return;
		}
		if (rawGet == null || rawSet == null) {
			host.withPendingEdits(action);
		} else {
			host.withPendingEdits(cfg -> PendingEdits.withValue(rawGet, rawSet, working, () -> action.accept(cfg)));
		}
	}

	@Nullable
	@Override
	public Component previewNote(ItemStack shown) {
		return host == null ? null : host.previewNote(shown);
	}

	@Override
	protected int headerHeight() {
		return 52;
	}

	@Override
	protected int titleY() {
		return 6;
	}

	@Override
	protected void fillRows() {
		ColorMode mode = working.mode;
		list.addFull(Ui.cycle(Ui.WIDTH, "glintsmith.color.mode", "glintsmith.color.mode", ColorMode.values(), mode, true, newMode -> {
			working.mode = newMode;
			refreshRows();
		}));
		list.addHelp(Ui.t("glintsmith.color.help." + Ui.lower(mode)));

		int shown = switch (mode) {
			case STATIC -> 1;
			case PULSE, GRADIENT -> working.colors.size();
			default -> 0;
		};
		for (int i = 0; i < shown; i++) {
			addColorRow(i, mode);
		}
		if (mode == ColorMode.PULSE || mode == ColorMode.GRADIENT) {
			Button add = Ui.button(Ui.WIDTH, Ui.t("glintsmith.color.add"), Ui.tip("glintsmith.color.add"), button -> {
				working.colors.add("#FFFFFF");
				refreshRows();
			});
			add.active = working.colors.size() < ColorStyle.MAX_COLORS;
			list.addFull(add);
		}

		FloatSlider speed = SettingRows.slider(Ui.WIDTH, "glintsmith.color.speed", 0.0f, 10.0f, 0.05f,
			SettingRows.decimals("glintsmith.color.speed", 2), working.speed, v -> working.speed = v);
		speed.active = mode != ColorMode.STATIC;
		list.addFull(speed);
		FloatSlider saturation = SettingRows.slider(Ui.WIDTH, "glintsmith.color.saturation", 0.0f, 1.0f, 0.01f,
			SettingRows.percent("glintsmith.color.saturation"), working.saturation, v -> working.saturation = v);
		saturation.active = mode == ColorMode.RAINBOW;
		list.addFull(saturation);
		FloatSlider brightness = SettingRows.slider(Ui.WIDTH, "glintsmith.color.brightness", 0.0f, 1.0f, 0.01f,
			SettingRows.percent("glintsmith.color.brightness"), working.brightness, v -> working.brightness = v);
		brightness.active = mode == ColorMode.RAINBOW;
		list.addFull(brightness);
	}

	private void addColorRow(int index, ColorMode mode) {
		ColorSwatch swatch = new ColorSwatch(Ui.HEIGHT, Ui.t("glintsmith.color.pick"),
			() -> Ui.parseOrGray(working.colors.get(index)), () -> openPicker(index));
		swatch.setTooltip(Ui.tip("glintsmith.color.pick"));
		HexBox hex = new HexBox(font, 130, Ui.t("glintsmith.color.hex"), false, text -> working.colors.set(index, text));
		hex.setValueSilently(working.colors.get(index));
		hex.setTooltip(Ui.tip("glintsmith.color.hex"));
		Button pick = Ui.button(PICK_WIDTH, Ui.t("glintsmith.color.pick"), Ui.tip("glintsmith.color.pick"),
			button -> openPicker(index));
		Button remove = Ui.button(Ui.HEIGHT, Component.literal("X").withStyle(ChatFormatting.RED),
			Ui.tip("glintsmith.color.remove"), button -> {
				working.colors.remove(index);
				refreshRows();
			});
		remove.active = mode != ColorMode.STATIC && working.colors.size() > 1;
		list.addRow(swatch, hex, pick, remove);
	}

	private void openPicker(int index) {
		int argb = Ui.parseOrGray(working.colors.get(index));
		minecraft.setScreen(new ColorPickerScreen(this, argb, picked -> working.colors.set(index, Colors.toHex(picked)),
			() -> working.colors.get(index), hex -> working.colors.set(index, hex)));
	}

	@Override
	protected void done() {
		working.sanitize();
		onDone.accept(working);
		minecraft.setScreen(parent);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		renderPreview(graphics);
	}

	/**
	 * Draws the colour over the last few seconds, oldest on the left and the current colour at the right edge,
	 * so the whole animation is visible at once.
	 */
	private void renderPreview(GuiGraphics graphics) {
		int x = width / 2 - 155;
		int y = 22;
		int w = Ui.WIDTH;
		int h = 22;
		ColorEvaluator.Compiled compiled = ColorEvaluator.compile(working);
		int[] samples = ColorStyleButton.sampleEnchantmentColors();
		double now = Util.getMillis() / 1000.0;
		graphics.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF000000);
		for (int px = 0; px < w; px += 2) {
			double time = now - (w - px) / (double) w * PREVIEW_WINDOW;
			graphics.fill(x + px, y, x + Math.min(px + 2, w), y + h, compiled.evaluate(time, samples));
		}
	}
}
