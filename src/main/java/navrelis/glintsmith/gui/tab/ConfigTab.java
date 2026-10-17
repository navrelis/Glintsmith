package navrelis.glintsmith.gui.tab;

import java.util.function.Consumer;

import navrelis.glintsmith.config.GlintsmithConfig;
import navrelis.glintsmith.gui.widget.OptionList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;

/**
 * One tab of the main screen: a scrolling {@link OptionList} over the whole tab area. The rows are always built
 * from the working copy of the config, never from the saved one.
 */
public abstract class ConfigTab implements Tab {
	protected final GlintsmithConfig config;
	protected final OptionList list;
	private final Component title;

	protected ConfigTab(GlintsmithConfig config, Component title) {
		this.config = config;
		this.title = title;
		this.list = new OptionList(Minecraft.getInstance(), 100, 100, 0);
	}

	@Override
	public Component getTabTitle() {
		return title;
	}

	@Override
	public void visitChildren(Consumer<AbstractWidget> visitor) {
		visitor.accept(list);
	}

	@Override
	public void doLayout(ScreenRectangle area) {
		list.updateSizeAndPosition(area.width(), area.height(), area.top());
	}

	/** Re-creates all rows from the working copy. */
	public void rebuild() {
		list.rebuild(this::fill);
	}

	/** Adds the rows of this tab to {@link #list}. */
	protected abstract void fill();

	/** Replaces this tab's part of the working copy by the defaults; the screen rebuilds afterwards. */
	public abstract void reset(GlintsmithConfig defaults);
}
