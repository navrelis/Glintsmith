package navrelis.glintsmith.gui.preview;

import java.util.function.Consumer;

import navrelis.glintsmith.config.GlintsmithConfig;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * A screen that shows a {@link PreviewPanel}. The root settings screen holds the working config; the sub screens
 * (rule editor, colour style, colour picker) hold edits that are not in it yet and write them in on demand.
 */
public interface PreviewHost {
	/** The session shared by the whole screen chain. */
	PreviewSession previewSession();

	/**
	 * Calls {@code action} with the root working config while every pending edit of this screen and of its parents
	 * is written into it, and takes the edits out again afterwards, also when the action throws.
	 */
	void withPendingEdits(Consumer<GlintsmithConfig> action);

	/** A hint about the edit in progress (for example "this rule does not match"), or null. */
	@Nullable
	default Component previewNote(ItemStack shown) {
		return null;
	}

	/** @return the screen as a host, or null if it is none */
	@Nullable
	static PreviewHost of(@Nullable Screen screen) {
		return screen instanceof PreviewHost host ? host : null;
	}
}
