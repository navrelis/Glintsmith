package navrelis.glintsmith.gui;

import navrelis.glintsmith.gui.preview.PreviewHost;
import navrelis.glintsmith.gui.preview.PreviewLayout;
import navrelis.glintsmith.gui.preview.PreviewPanel;
import navrelis.glintsmith.gui.widget.OptionList;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * Base of the list based editor screens: a title, a scrolling {@link OptionList} and Done / Cancel buttons,
 * laid out like vanilla's options screens. All widgets are rebuilt from the screen's working copy in
 * {@link #fillRows()}, so resizing the window keeps the state.
 */
public abstract class ConfigSubScreen extends Screen {
	protected static final int FOOTER_HEIGHT = 33;

	protected final Screen parent;
	protected OptionList list;
	/** The live preview right of the list, or null if the screen has none (see {@link #previewHost()}). */
	@Nullable
	protected PreviewPanel previewPanel;
	private double savedScroll;

	protected ConfigSubScreen(Screen parent, Component title) {
		super(title);
		this.parent = parent;
	}

	/** Height of the area above the list. */
	protected int headerHeight() {
		return 33;
	}

	/** Vertical position of the title. */
	protected int titleY() {
		return 12;
	}

	/** Adds all rows to {@link #list}. Must only read the working copy of the screen. */
	protected abstract void fillRows();

	/** Applies the edit and leaves the screen. */
	protected abstract void done();

	/** Leaves the screen without applying anything. */
	protected void cancel() {
		minecraft.setScreen(parent);
	}

	/** The screen that feeds the preview panel, or null for no panel. */
	@Nullable
	protected PreviewHost previewHost() {
		return null;
	}

	/**
	 * Adds the preview panel right of the list. Called before the list is added, so the panel is asked first about
	 * clicks; it is drawn after everything else because the list paints its background over the full width.
	 */
	protected void addPreviewPanel(PreviewHost host) {
		previewPanel = addWidget(new PreviewPanel(host));
		previewPanel.setLayout(PreviewLayout.side(width, headerHeight(), height - FOOTER_HEIGHT));
	}

	@Override
	protected void init() {
		previewPanel = null;
		PreviewHost host = previewHost();
		if (host != null) {
			addPreviewPanel(host);
		}
		list = addRenderableWidget(new OptionList(minecraft, width, height - headerHeight() - FOOTER_HEIGHT, headerHeight()));
		fillRows();
		list.setScrollAmount(savedScroll);
		int y = height - FOOTER_HEIGHT + 6;
		addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> done())
			.bounds(width / 2 - 155, y, Ui.HALF, Ui.HEIGHT).build());
		addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> cancel())
			.bounds(width / 2 + 5, y, Ui.HALF, Ui.HEIGHT).build());
	}

	@Override
	protected void rebuildWidgets() {
		if (list != null) {
			savedScroll = list.getScrollAmount();
		}
		super.rebuildWidgets();
	}

	/** Re-creates the rows after a change that adds or removes widgets. */
	protected void refreshRows() {
		list.rebuild(this::fillRows);
	}

	@Override
	public void onClose() {
		cancel();
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		graphics.drawCenteredString(font, title, width / 2, titleY(), 0xFFFFFFFF);
		if (previewPanel != null) {
			previewPanel.render(graphics, mouseX, mouseY, partialTick);
		}
	}
}
