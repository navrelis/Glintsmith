package navrelis.glintsmith.gui;

import java.util.List;
import java.util.function.Consumer;

import navrelis.glintsmith.config.ConfigManager;
import navrelis.glintsmith.config.GlintsmithConfig;
import navrelis.glintsmith.gui.preview.PreviewHost;
import navrelis.glintsmith.gui.preview.PreviewLayout;
import navrelis.glintsmith.gui.preview.PreviewPanel;
import navrelis.glintsmith.gui.preview.PreviewSession;
import navrelis.glintsmith.gui.tab.ConfigTab;
import navrelis.glintsmith.gui.tab.EnchantmentColorsTab;
import navrelis.glintsmith.gui.tab.GlintTab;
import navrelis.glintsmith.gui.tab.ModelFixTab;
import navrelis.glintsmith.gui.tab.OutlineTab;
import navrelis.glintsmith.gui.tab.RulesTab;
import navrelis.glintsmith.render.preview.PreviewRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.components.tabs.TabNavigationBar;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * The Glintsmith settings screen: tabs for outline, glint, model fix, rules and enchantment colours.
 * <p>
 * The screen edits a copy of the config. Done saves it, Cancel (or Esc) throws it away, asking first if anything
 * changed. The widgets of the tabs read and write that copy only, so nothing is lost when the window is resized
 * or a sub screen (colour picker, rule editor) is opened.
 */
public final class GlintsmithScreen extends Screen implements PreviewHost {
	private static final SystemToast.SystemToastId SAVE_FAILED = new SystemToast.SystemToastId(10000L);
	private static final int FOOTER_HEIGHT = 56;
	private static final int BUTTON_WIDTH = 104;
	private static final int FILE_BUTTON_WIDTH = 150;

	private final Screen parent;
	private final GlintsmithConfig working;
	private final String baseline;
	private final PreviewSession previewSession = new PreviewSession();
	private final TabManager tabManager = new TabManager(this::addRenderableWidget, this::removeWidget);
	private PreviewPanel previewPanel;
	private TabNavigationBar tabBar;
	private List<ConfigTab> tabs = List.of();
	private int selectedTab;
	private int headerHeight = 24;

	private Button doneButton;
	private Button cancelButton;
	private Button resetButton;
	private Button openConfigButton;
	private Button openTexturesButton;

	public GlintsmithScreen(Screen parent) {
		super(Ui.t("glintsmith.title"));
		this.parent = parent;
		this.working = ConfigManager.instance().get().copy();
		this.baseline = ConfigManager.toJson(ConfigManager.instance().get().copy());
	}

	@Override
	protected void init() {
		tabs = List.of(
			new OutlineTab(working),
			new GlintTab(working),
			new ModelFixTab(working),
			new RulesTab(working),
			new EnchantmentColorsTab(working));
		tabBar = TabNavigationBar.builder(tabManager, width).addTabs(tabs.toArray(new Tab[0])).build();
		addRenderableWidget(tabBar);

		doneButton = addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> done())
			.tooltip(Ui.tip("glintsmith.button.done")).build());
		cancelButton = addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> requestCancel())
			.tooltip(Ui.tip("glintsmith.button.cancel")).build());
		resetButton = addRenderableWidget(Button.builder(Ui.t("glintsmith.button.reset_tab"), button -> requestReset())
			.tooltip(Ui.tip("glintsmith.button.reset_tab")).build());
		openConfigButton = addRenderableWidget(Button.builder(Ui.t("glintsmith.button.open_config"),
			button -> Util.getPlatform().openPath(ConfigManager.instance().path()))
			.tooltip(Ui.tip("glintsmith.button.open_config")).build());
		openTexturesButton = addRenderableWidget(Button.builder(Ui.t("glintsmith.button.open_textures"),
			button -> Util.getPlatform().openPath(ConfigManager.instance().textureDir()))
			.tooltip(Ui.tip("glintsmith.button.open_textures")).build());
		// Not renderable: drawn by render() after the tab list, whose background would cover it. It is added before
		// the tab widgets (added by selectTab below) so it is asked about clicks first.
		previewPanel = addWidget(new PreviewPanel(this));

		for (ConfigTab tab : tabs) {
			tab.rebuild();
		}
		tabBar.selectTab(Math.min(selectedTab, tabs.size() - 1), false);
		repositionElements();
	}

	@Override
	protected void setInitialFocus() {
	}

	@Override
	protected void repositionElements() {
		if (tabBar == null) {
			return;
		}
		tabBar.setWidth(width);
		tabBar.arrangeElements();
		headerHeight = tabBar.getRectangle().bottom();
		tabManager.setTabArea(new ScreenRectangle(0, headerHeight, width, height - FOOTER_HEIGHT - headerHeight));
		layoutFooter();
		layoutPreview();
	}

	private int buttonWidth() {
		return Math.min(BUTTON_WIDTH, (width - 24) / 3);
	}

	private int fileWidth() {
		return Math.min(FILE_BUTTON_WIDTH, (width - 20) / 2);
	}

	/** The preview goes right of the options; if that does not fit, left of the footer buttons; else it is hidden. */
	private void layoutPreview() {
		PreviewLayout.Layout layout = PreviewLayout.side(width, headerHeight, height - FOOTER_HEIGHT);
		if (layout.mode() == PreviewLayout.Mode.HIDDEN) {
			int widestRow = Math.max(3 * buttonWidth() + 2 * Ui.GAP, 2 * fileWidth() + Ui.GAP);
			layout = PreviewLayout.footer(width, height, FOOTER_HEIGHT, widestRow);
		}
		if (previewPanel != null) {
			previewPanel.setLayout(layout);
		}
	}

	@Override
	public PreviewSession previewSession() {
		return previewSession;
	}

	/** The working copy is the root of the preview chain: nothing to write in, the edits are already there. */
	@Override
	public void withPendingEdits(Consumer<GlintsmithConfig> action) {
		action.accept(working);
	}

	private void layoutFooter() {
		int buttonWidth = buttonWidth();
		int rowWidth = 3 * buttonWidth + 2 * Ui.GAP;
		int x = (width - rowWidth) / 2;
		int top = height - FOOTER_HEIGHT + 6;
		place(doneButton, x, top, buttonWidth);
		place(cancelButton, x + buttonWidth + Ui.GAP, top, buttonWidth);
		place(resetButton, x + 2 * (buttonWidth + Ui.GAP), top, buttonWidth);

		int fileWidth = fileWidth();
		int fileRow = 2 * fileWidth + Ui.GAP;
		int fileX = (width - fileRow) / 2;
		int fileTop = top + Ui.HEIGHT + Ui.GAP;
		place(openConfigButton, fileX, fileTop, fileWidth);
		place(openTexturesButton, fileX + fileWidth + Ui.GAP, fileTop, fileWidth);
	}

	private static void place(Button button, int x, int y, int width) {
		button.setX(x);
		button.setY(y);
		button.setWidth(width);
	}

	// ---- actions --------------------------------------------------------------------------------------------

	private void done() {
		ConfigManager manager = ConfigManager.instance();
		boolean written = manager.save(working);
		String error = manager.lastError();
		if (!written || error != null) {
			SystemToast toast = SystemToast.multiline(minecraft, SAVE_FAILED,
				Ui.t("glintsmith.save_failed").withStyle(ChatFormatting.RED),
				Component.literal(error == null ? "?" : error).withStyle(ChatFormatting.RED));
			minecraft.getToasts().addToast(toast);
		}
		PreviewRenderer.clearCache();
		minecraft.setScreen(parent);
	}

	private boolean hasChanges() {
		return !ConfigManager.toJson(working).equals(baseline);
	}

	private void requestCancel() {
		if (!hasChanges()) {
			PreviewRenderer.clearCache();
			minecraft.setScreen(parent);
			return;
		}
		minecraft.setScreen(new ConfirmScreen(discard -> {
			if (discard) {
				PreviewRenderer.clearCache();
			}
			minecraft.setScreen(discard ? parent : this);
		},
			Ui.t("glintsmith.confirm.discard.title"), Ui.t("glintsmith.confirm.discard.message"),
			Ui.t("glintsmith.confirm.discard.yes"), Component.translatable("gui.back")));
	}

	private void requestReset() {
		ConfigTab tab = currentTab();
		if (tab == null) {
			return;
		}
		minecraft.setScreen(new ConfirmScreen(confirmed -> {
			if (confirmed) {
				tab.reset(new GlintsmithConfig().sanitize());
				tab.rebuild();
			}
			minecraft.setScreen(this);
		}, Ui.t("glintsmith.confirm.reset.title"), Ui.t("glintsmith.confirm.reset.message", tab.getTabTitle()),
			Ui.t("glintsmith.confirm.reset.yes"), Component.translatable("gui.back")));
	}

	private ConfigTab currentTab() {
		Tab tab = tabManager.getCurrentTab();
		return tab instanceof ConfigTab config ? config : null;
	}

	@Override
	public void onClose() {
		requestCancel();
	}

	// ---- input and drawing ----------------------------------------------------------------------------------

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (tabBar != null && tabBar.keyPressed(keyCode)) {
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		ConfigTab current = currentTab();
		if (current != null) {
			selectedTab = Math.max(0, tabs.indexOf(current));
		}
		super.render(graphics, mouseX, mouseY, partialTick);
		if (previewPanel != null) {
			previewPanel.render(graphics, mouseX, mouseY, partialTick);
		}
	}

	@Override
	protected void renderMenuBackground(GuiGraphics graphics) {
		graphics.blit(CreateWorldScreen.TAB_HEADER_BACKGROUND, 0, 0, 0.0F, 0.0F, width, headerHeight, 16, 16);
		renderMenuBackground(graphics, 0, headerHeight, width, height);
	}
}
