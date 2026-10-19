package navrelis.glintsmith.gui.widget;

import java.util.ArrayList;
import java.util.List;

import navrelis.glintsmith.gui.Ui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * A scrolling list of option rows, built like vanilla's {@code OptionsList}: every row is 24 px high (20 px of
 * widgets, 4 px gap) and 310 px wide, centred in the list. Rows hold any number of widgets laid out left to right.
 */
public class OptionList extends ContainerObjectSelectionList<OptionList.Row> {
	public static final int ROW_HEIGHT = 24;

	public OptionList(Minecraft minecraft, int width, int height, int top) {
		super(minecraft, width, height, top, ROW_HEIGHT);
		this.centerListVertically = false;
	}

	@Override
	public int getRowWidth() {
		return Ui.WIDTH;
	}

	public <R extends Row> R add(R row) {
		addEntry(row);
		return row;
	}

	/** One full-width widget. */
	public void addFull(AbstractWidget widget) {
		add(new Row(Ui.GAP, widget));
	}

	/** Two half-width widgets, vanilla spacing. */
	public void addPair(AbstractWidget left, AbstractWidget right) {
		add(new Row(Ui.PAIR_GAP, left, right));
	}

	/** Any number of widgets with the standard 4 px gap. */
	public void addRow(AbstractWidget... widgets) {
		add(new Row(Ui.GAP, widgets));
	}

	/** A heading line. */
	public void addHeading(Component text) {
		add(new TextRow(List.of(text.getVisualOrderText()), Ui.HEADING, true));
	}

	/** Grey help text, wrapped to the row width, two lines per row. */
	public void addHelp(Component text) {
		Font font = minecraft.font;
		List<FormattedCharSequence> lines = font.split(text, Ui.WIDTH);
		for (int i = 0; i < lines.size(); i += 2) {
			add(new TextRow(new ArrayList<>(lines.subList(i, Math.min(lines.size(), i + 2))), Ui.GRAY, false));
		}
	}

	/** Removes all rows and re-fills the list, keeping the scroll position as far as possible. */
	public void rebuild(Runnable filler) {
		setFocused(null);
		clearEntries();
		filler.run();
		clampScrollAmount();
	}

	/** One row of widgets. */
	public static class Row extends ContainerObjectSelectionList.Entry<Row> {
		protected List<AbstractWidget> widgets;
		protected final int gap;

		public Row(int gap, AbstractWidget... widgets) {
			this(gap, List.of(widgets));
		}

		public Row(int gap, List<AbstractWidget> widgets) {
			this.gap = gap;
			this.widgets = List.copyOf(widgets);
		}

		/** Places the widgets; the default is left to right starting at {@code x}. */
		protected void layout(int x, int y) {
			int cursor = x;
			for (AbstractWidget widget : widgets) {
				widget.setPosition(cursor, y);
				cursor += widget.getWidth() + gap;
			}
		}

		/** Draws anything that belongs behind the widgets. */
		protected void renderBefore(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
		}

		/** Draws anything that belongs in front of the widgets. */
		protected void renderAfter(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
		}

		@Override
		public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX,
			int mouseY, boolean hovered, float partialTick) {
			int x = left - 2;
			layout(x, top);
			renderBefore(graphics, x, top, mouseX, mouseY);
			for (AbstractWidget widget : widgets) {
				widget.render(graphics, mouseX, mouseY, partialTick);
			}
			renderAfter(graphics, x, top, mouseX, mouseY);
		}

		@Override
		public List<? extends GuiEventListener> children() {
			return widgets;
		}

		@Override
		public List<? extends NarratableEntry> narratables() {
			return widgets;
		}
	}

	/** A label on the left, one widget on the right. */
	public static class LabeledRow extends Row {
		public static final int LABEL_WIDTH = 92;
		/** Width to give the widget of a labelled row. */
		public static final int WIDGET_WIDTH = Ui.WIDTH - LABEL_WIDTH - Ui.GAP;

		private final Component label;

		public LabeledRow(Component label, AbstractWidget widget) {
			super(Ui.GAP, widget);
			this.label = label;
		}

		@Override
		protected void layout(int x, int y) {
			widgets.get(0).setPosition(x + LABEL_WIDTH + Ui.GAP, y);
		}

		@Override
		protected void renderBefore(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
			Font font = Minecraft.getInstance().font;
			String text = label.getString();
			if (font.width(text) > LABEL_WIDTH) {
				text = font.plainSubstrByWidth(text, LABEL_WIDTH - font.width("…")) + "…";
			}
			graphics.drawString(font, text, x, y + (Ui.HEIGHT - 8) / 2, 0xFFE0E0E0);
		}
	}

	/** One or two lines of text without widgets. */
	public static class TextRow extends Row {
		private final List<FormattedCharSequence> lines;
		private final int color;
		private final boolean shadow;

		public TextRow(List<FormattedCharSequence> lines, int color, boolean shadow) {
			super(Ui.GAP, List.of());
			this.lines = lines;
			this.color = color;
			this.shadow = shadow;
		}

		@Override
		protected void renderBefore(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
			Font font = Minecraft.getInstance().font;
			int total = lines.size() * 10 - 1;
			int cursor = y + (Ui.HEIGHT - total) / 2;
			for (FormattedCharSequence line : lines) {
				graphics.drawString(font, line, x, cursor, color, shadow);
				cursor += 10;
			}
		}
	}
}
