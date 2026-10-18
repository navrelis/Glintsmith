package navrelis.glintsmith.gui.preview;

import java.util.List;

import navrelis.glintsmith.config.GlintsmithConfig;
import navrelis.glintsmith.gui.Ui;
import navrelis.glintsmith.render.preview.PreviewRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The live preview: a framed box with the item turning in it as it looks in the player's hand, a two line caption
 * and a strip that switches between the held item and a sample sword. Drag turns the item, right click resets the
 * view. It never takes keyboard focus and makes no sound except for the strip.
 * <p>
 * Screens add it with {@code addWidget} and draw it themselves after everything else: the option list paints its
 * background over the whole screen width, which would cover a panel that is drawn before it.
 */
public final class PreviewPanel extends AbstractWidget {
	private static final Logger LOGGER = LoggerFactory.getLogger("Glintsmith/Preview");
	private static final int FRAME_FILL = 0x80000000;
	private static final int FRAME_BORDER = 0xFF404040;
	private static final int STRIP_FILL = 0x60000000;
	private static final int STRIP_HOVER = 0x40FFFFFF;
	private static final int NOTE_COLOR = 0xFFFFD27F;
	private static final int TEXT_PAD = 2;
	/** Tooltip regions: the preview itself, the source strip. */
	private static final int REGION_PREVIEW = 0;
	private static final int REGION_STRIP = 1;

	private final PreviewHost host;
	private final PreviewSession session;
	private PreviewLayout.Layout layout = PreviewLayout.Layout.HIDDEN;
	private GlintsmithConfig snapshot = new GlintsmithConfig().sanitize();
	private ItemStack shown = ItemStack.EMPTY;
	private List<String> notes = List.of();
	@Nullable
	private Component hostNote;
	@Nullable
	private List<String> hostNoteFor;
	private int tooltipRegion = -1;
	@Nullable
	private List<String> tooltipNotes;
	@Nullable
	private Component tooltipHostNote;
	private boolean failureLogged;

	public PreviewPanel(PreviewHost host) {
		super(0, 0, 0, 0, Ui.t("glintsmith.preview"));
		this.host = host;
		this.session = host.previewSession();
		this.visible = false;
	}

	public void setLayout(PreviewLayout.Layout layout) {
		this.layout = layout;
		this.visible = layout.mode() != PreviewLayout.Mode.HIDDEN;
		setX(layout.x());
		setY(layout.y());
		setWidth(layout.w());
		setHeight(layout.totalH());
	}

	// ---- drawing --------------------------------------------------------------------------------------------

	@Override
	protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		Minecraft minecraft = Minecraft.getInstance();
		Font font = minecraft.font;
		long now = Util.getMillis();
		long window = minecraft.getWindow().getWindow();
		if (session.dragging && GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) != GLFW.GLFW_PRESS) {
			// the release happened outside the panel, where it is not delivered to us
			session.release(now);
		}
		int x = layout.x();
		int y = layout.y();
		int w = layout.w();
		int h = layout.itemH();
		graphics.fill(x, y, x + w, y + h, FRAME_FILL);
		graphics.renderOutline(x, y, w, h, FRAME_BORDER);

		if (!PreviewRenderer.available()) {
			drawUnavailable(graphics, font, x, y, w, h);
			updateTooltip(REGION_PREVIEW);
			return;
		}

		try {
			host.withPendingEdits(cfg -> snapshot = session.snap.update(cfg));
			session.tick(now);
			shown = session.shownStack(minecraft);
			notes = session.notes(shown, snapshot);
			if (hostNoteFor != notes) {
				hostNote = host.previewNote(shown);
				hostNoteFor = notes;
			}
			float margin = session.marginGui(minecraft.getMainRenderTarget().height, minecraft.getWindow().getGuiScale());
			PreviewRenderer.draw(graphics, x + 1, y + 1, x + w - 1, y + h - 1, shown, snapshot, session.yaw, session.pitch,
				margin, !shown.is(Items.FILLED_MAP));
		} catch (RuntimeException e) {
			if (!failureLogged) {
				failureLogged = true;
				LOGGER.error("The preview could not be updated", e);
			}
		}

		if (layout.caption()) {
			drawCaption(graphics, font, x, layout.captionY(), w);
		}
		boolean overStrip = false;
		if (layout.toggle()) {
			overStrip = isHovered() && isOverStrip(mouseX, mouseY);
			drawStrip(graphics, font, x, layout.toggleY(), w, overStrip);
		}
		updateTooltip(overStrip ? REGION_STRIP : REGION_PREVIEW);
	}

	private void drawUnavailable(GuiGraphics graphics, Font font, int x, int y, int w, int h) {
		List<FormattedCharSequence> lines = font.split(Ui.t(PreviewNotes.UNAVAILABLE), Math.max(8, w - 2 * TEXT_PAD - 2));
		int top = y + (h - lines.size() * 10) / 2;
		for (FormattedCharSequence line : lines) {
			graphics.drawString(font, line, x + (w - font.width(line)) / 2, top, NOTE_COLOR);
			top += 10;
		}
	}

	private void drawCaption(GuiGraphics graphics, Font font, int x, int y, int w) {
		int max = w - 2 * TEXT_PAD;
		graphics.drawString(font, fit(font, shown.isEmpty() ? "" : shown.getHoverName().getString(), max),
			x + TEXT_PAD, y, Ui.WHITE);
		Component note = !notes.isEmpty() ? Ui.t(notes.get(0)) : hostNote;
		if (note != null) {
			graphics.drawString(font, fit(font, note.getString(), max), x + TEXT_PAD, y + 10, NOTE_COLOR);
		}
	}

	private void drawStrip(GuiGraphics graphics, Font font, int x, int y, int w, boolean hovered) {
		graphics.fill(x, y, x + w, y + PreviewLayout.TOGGLE_H, STRIP_FILL);
		if (hovered) {
			graphics.fill(x, y, x + w, y + PreviewLayout.TOGGLE_H, STRIP_HOVER);
		}
		graphics.renderOutline(x, y, w, PreviewLayout.TOGGLE_H, FRAME_BORDER);
		boolean held = PreviewSession.source == PreviewSession.Source.HELD;
		String label = "◀ " + Ui.t(held ? "glintsmith.preview.source.held" : "glintsmith.preview.source.sample").getString()
			+ " ▶";
		String text = fit(font, label, w - 2 * TEXT_PAD);
		graphics.drawString(font, text, x + (w - font.width(text)) / 2, y + (PreviewLayout.TOGGLE_H - 8) / 2 + 1,
			hovered ? Ui.WHITE : 0xFFE0E0E0);
	}

	private static String fit(Font font, String text, int maxWidth) {
		if (font.width(text) <= maxWidth) {
			return text;
		}
		return font.plainSubstrByWidth(text, Math.max(0, maxWidth - font.width("…"))) + "…";
	}

	/** Rebuilds the tooltip only when what it says changed. */
	private void updateTooltip(int region) {
		if (region == tooltipRegion && notes == tooltipNotes && hostNote == tooltipHostNote) {
			return;
		}
		tooltipRegion = region;
		tooltipNotes = notes;
		tooltipHostNote = hostNote;
		if (region == REGION_STRIP) {
			setTooltip(Ui.tip("glintsmith.preview.source"));
			return;
		}
		MutableComponent text = Ui.t("glintsmith.preview.tooltip");
		for (String key : notes) {
			text.append("\n").append(Ui.t(key).withStyle(ChatFormatting.YELLOW));
		}
		if (hostNote != null) {
			text.append("\n").append(hostNote.copy().withStyle(ChatFormatting.YELLOW));
		}
		setTooltip(Tooltip.create(text));
	}

	// ---- input ----------------------------------------------------------------------------------------------

	private boolean isOverStrip(double mouseX, double mouseY) {
		return layout.toggle() && mouseX >= layout.x() && mouseX < layout.x() + layout.w()
			&& mouseY >= layout.toggleY() && mouseY < layout.toggleY() + PreviewLayout.TOGGLE_H;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (!isMouseOver(mouseX, mouseY)) {
			return false;
		}
		if (button == 0) {
			if (isOverStrip(mouseX, mouseY)) {
				session.toggleSource();
				Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
			} else {
				session.press();
			}
			return true;
		}
		if (button == 1) {
			session.resetView();
			return true;
		}
		return false;
	}

	@Override
	protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
		if (session.dragging) {
			session.drag(dragX, dragY);
		}
	}

	@Override
	public void onRelease(double mouseX, double mouseY) {
		if (session.dragging) {
			session.release(Util.getMillis());
		}
	}

	@Override
	public void playDownSound(SoundManager soundManager) {
	}

	/** Keyboard navigation skips the panel; it has nothing to do with the keyboard. */
	@Nullable
	@Override
	public ComponentPath nextFocusPath(FocusNavigationEvent event) {
		return null;
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		output.add(NarratedElementType.TITLE, Ui.t("glintsmith.preview.narration",
			shown.isEmpty() ? Component.empty() : shown.getHoverName()));
	}
}
