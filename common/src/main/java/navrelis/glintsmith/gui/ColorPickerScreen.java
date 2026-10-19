package navrelis.glintsmith.gui;

import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

import navrelis.glintsmith.config.GlintsmithConfig;
import navrelis.glintsmith.gui.preview.PendingEdits;
import navrelis.glintsmith.gui.preview.PreviewHost;
import navrelis.glintsmith.gui.preview.PreviewLayout;
import navrelis.glintsmith.gui.preview.PreviewPanel;
import navrelis.glintsmith.gui.preview.PreviewSession;
import navrelis.glintsmith.gui.widget.ColorSwatch;
import navrelis.glintsmith.gui.widget.GradientSlider;
import navrelis.glintsmith.gui.widget.HexBox;
import navrelis.glintsmith.style.Colors;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Picks one colour with hue, saturation and brightness sliders, a hex box and a row of presets.
 * The colour lives in the fields of this screen, so resizing the window keeps it.
 */
public final class ColorPickerScreen extends Screen implements PreviewHost {
	private static final int[] PRESETS = {
		0xFFD088FC, 0xFFFFF3A5, 0xFF55FFFF, 0xFFFF5555, 0xFF55FF55, 0xFFFFAA00,
		0xFFFFFFFF, 0xFFAA00AA, 0xFF5555FF, 0xFFFF55FF, 0xFF00AAAA, 0xFFFFD700
	};
	private static final int PREVIEW_HEIGHT = 32;
	private static final int SWATCH = 20;

	private final Screen parent;
	private final int initial;
	private final IntConsumer onDone;
	@Nullable
	private final PreviewHost host;
	/** Read and write the stored colour that {@code onDone} replaces, so the preview can show the pick; may be null. */
	@Nullable
	private final Supplier<String> rawGet;
	@Nullable
	private final Consumer<String> rawSet;
	private float hue;
	private float saturation;
	private float value;
	private int alpha;

	private GradientSlider hueSlider;
	private GradientSlider saturationSlider;
	private GradientSlider valueSlider;
	private HexBox hexBox;
	private int titleY;
	private int previewY;

	public ColorPickerScreen(Screen parent, int initialArgb, IntConsumer onDone) {
		this(parent, initialArgb, onDone, null, null);
	}

	/**
	 * @param rawGet reads the stored hex colour that {@code onDone} replaces; null if the preview cannot show the pick
	 * @param rawSet writes it
	 */
	public ColorPickerScreen(Screen parent, int initialArgb, IntConsumer onDone, @Nullable Supplier<String> rawGet,
		@Nullable Consumer<String> rawSet) {
		super(Ui.t("glintsmith.picker.title"));
		this.parent = parent;
		this.initial = initialArgb;
		this.onDone = onDone;
		this.host = PreviewHost.of(parent);
		this.rawGet = rawGet;
		this.rawSet = rawSet;
		setFromArgb(initialArgb);
	}

	// ---- preview ------------------------------------------------------------------------------------------

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
			host.withPendingEdits(cfg -> PendingEdits.withValue(rawGet, rawSet, Colors.toHex(currentArgb()),
				() -> action.accept(cfg)));
		}
	}

	@Nullable
	@Override
	public Component previewNote(ItemStack shown) {
		return host == null ? null : host.previewNote(shown);
	}

	private void setFromArgb(int argb) {
		float[] hsv = Hsv.fromArgb(argb, hue);
		hue = hsv[0];
		saturation = hsv[1];
		value = hsv[2];
		alpha = argb >>> 24;
	}

	private int currentArgb() {
		return Colors.withAlpha(Colors.hsv(hue, saturation, value), alpha / 255.0f);
	}

	@Override
	protected void init() {
		if (host != null) {
			PreviewPanel panel = addRenderableWidget(new PreviewPanel(this));
			panel.setLayout(PreviewLayout.side(width, 6, height - 33));
		}
		int left = width / 2 - 155;
		int blockHeight = 16 + PREVIEW_HEIGHT + 6 + 3 * 24 + 24 + 20;
		titleY = Math.max(6, (height - 33 - blockHeight) / 2);
		previewY = titleY + 16;
		int y = previewY + PREVIEW_HEIGHT + 6;

		hueSlider = addRenderableWidget(new GradientSlider(Ui.WIDTH, hue,
			v -> Ui.t("glintsmith.picker.hue", Math.round(v * 360.0)),
			v -> {
				hue = (float) v;
				syncHex();
			},
			t -> Colors.hsv((float) t, 1.0f, 1.0f)));
		hueSlider.setPosition(left, y);
		hueSlider.setTooltip(Ui.tip("glintsmith.picker.hue"));
		y += 24;

		saturationSlider = addRenderableWidget(new GradientSlider(Ui.WIDTH, saturation,
			v -> Ui.t("glintsmith.picker.saturation", Math.round(v * 100.0)),
			v -> {
				saturation = (float) v;
				syncHex();
			},
			t -> Colors.hsv(hue, (float) t, value)));
		saturationSlider.setPosition(left, y);
		saturationSlider.setTooltip(Ui.tip("glintsmith.picker.saturation"));
		y += 24;

		valueSlider = addRenderableWidget(new GradientSlider(Ui.WIDTH, value,
			v -> Ui.t("glintsmith.picker.brightness", Math.round(v * 100.0)),
			v -> {
				value = (float) v;
				syncHex();
			},
			t -> Colors.hsv(hue, saturation, (float) t)));
		valueSlider.setPosition(left, y);
		valueSlider.setTooltip(Ui.tip("glintsmith.picker.brightness"));
		y += 24;

		hexBox = addRenderableWidget(new HexBox(font, Ui.HALF, Ui.t("glintsmith.picker.hex"), false, this::hexChanged));
		hexBox.setValueSilently(Colors.toHex(currentArgb()));
		hexBox.setPosition(left, y);
		hexBox.setTooltip(Ui.tip("glintsmith.picker.hex"));
		Button revert = addRenderableWidget(Ui.button(Ui.HALF, Ui.t("glintsmith.picker.revert"),
			Ui.tip("glintsmith.picker.revert"), button -> applyColor(initial)));
		revert.setPosition(left + Ui.HALF + Ui.PAIR_GAP, y);
		y += 24;

		int presetsLeft = width / 2 - (PRESETS.length * SWATCH + (PRESETS.length - 1) * Ui.GAP) / 2;
		for (int i = 0; i < PRESETS.length; i++) {
			int argb = PRESETS[i];
			ColorSwatch swatch = addRenderableWidget(new ColorSwatch(SWATCH, Component.literal(Colors.toHex(argb)),
				() -> argb, () -> applyColor(argb)));
			swatch.setPosition(presetsLeft + i * (SWATCH + Ui.GAP), y);
			swatch.setTooltip(Tooltip.create(Component.literal(Colors.toHex(argb))));
		}

		int footerY = height - 27;
		addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> done())
			.bounds(width / 2 - 155, footerY, Ui.HALF, Ui.HEIGHT).build());
		addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> onClose())
			.bounds(width / 2 + 5, footerY, Ui.HALF, Ui.HEIGHT).build());
	}

	private void syncHex() {
		if (hexBox != null) {
			hexBox.setValueSilently(Colors.toHex(currentArgb()));
		}
	}

	/** Called when valid text was typed into the hex box. */
	private void hexChanged(String text) {
		setFromArgb(Colors.parseHex(text));
		syncSliders();
	}

	/** Takes a whole colour (preset, revert) and updates every widget. */
	private void applyColor(int argb) {
		setFromArgb(argb);
		syncSliders();
		syncHex();
	}

	private void syncSliders() {
		hueSlider.setNormalized(hue);
		saturationSlider.setNormalized(saturation);
		valueSlider.setNormalized(value);
	}

	private void done() {
		onDone.accept(currentArgb());
		minecraft.setScreen(parent);
	}

	@Override
	public void onClose() {
		minecraft.setScreen(parent);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		graphics.drawCenteredString(font, title, width / 2, titleY, 0xFFFFFFFF);
		renderPreview(graphics);
	}

	private void renderPreview(GuiGraphics graphics) {
		int x = width / 2 - 155;
		int half = Ui.WIDTH / 2;
		int now = currentArgb();
		graphics.fill(x - 1, previewY - 1, x + Ui.WIDTH + 1, previewY + PREVIEW_HEIGHT + 1, 0xFF000000);
		paintHalf(graphics, x, half, initial, Ui.t("glintsmith.picker.old"));
		paintHalf(graphics, x + half, half, now, Ui.t("glintsmith.picker.new"));
	}

	private void paintHalf(GuiGraphics graphics, int x, int w, int argb, Component label) {
		if ((argb >>> 24) < 255) {
			graphics.fill(x, previewY, x + w, previewY + PREVIEW_HEIGHT, 0xFF808080);
		}
		graphics.fill(x, previewY, x + w, previewY + PREVIEW_HEIGHT, argb);
		int luminance = (299 * (argb >> 16 & 0xFF) + 587 * (argb >> 8 & 0xFF) + 114 * (argb & 0xFF)) / 1000;
		int textColor = luminance > 140 ? 0xFF000000 : 0xFFFFFFFF;
		graphics.drawCenteredString(font, label, x + w / 2, previewY + (PREVIEW_HEIGHT - 8) / 2, textColor);
	}
}
