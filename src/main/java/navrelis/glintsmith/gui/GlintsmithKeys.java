package navrelis.glintsmith.gui;

import com.mojang.blaze3d.platform.InputConstants;
import navrelis.glintsmith.config.ConfigManager;
import navrelis.glintsmith.config.GlintsmithConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;

/**
 * The two key bindings (both unbound by default) and the client tick handler that serves them, the commands
 * and the screen opening requests.
 */
public final class GlintsmithKeys {
	public static final String CATEGORY = "key.categories.glintsmith";
	public static final String OPEN_CONFIG = "key.glintsmith.open_config";
	public static final String TOGGLE_OUTLINES = "key.glintsmith.toggle_outlines";

	private static final SystemToast.SystemToastId SAVE_FAILED = new SystemToast.SystemToastId(10000L);

	private static KeyMapping openConfig;
	private static KeyMapping toggleOutlines;
	private static volatile boolean openRequested;

	private GlintsmithKeys() {
	}

	public static void register() {
		openConfig = KeyBindingHelper.registerKeyBinding(
			new KeyMapping(OPEN_CONFIG, InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), CATEGORY));
		toggleOutlines = KeyBindingHelper.registerKeyBinding(
			new KeyMapping(TOGGLE_OUTLINES, InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), CATEGORY));
		ClientTickEvents.END_CLIENT_TICK.register(GlintsmithKeys::onTick);
	}

	/**
	 * Opens the settings screen on the next client tick. Commands use this: a screen opened directly from a chat
	 * command would be closed again by the chat screen right after the command ran.
	 */
	public static void requestOpenScreen() {
		openRequested = true;
	}

	private static void onTick(Minecraft minecraft) {
		while (openConfig.consumeClick()) {
			if (minecraft.screen == null) {
				openRequested = true;
			}
		}
		while (toggleOutlines.consumeClick()) {
			toggleOutlines(minecraft);
		}
		if (openRequested && minecraft.screen == null) {
			openRequested = false;
			minecraft.setScreen(new GlintsmithScreen(null));
		}
	}

	/**
	 * Flips the global outline switch, saves, tells the player on the action bar and runs
	 * {@link GuiHooks#onToggleOutlines}.
	 *
	 * @return the new state of the switch
	 */
	public static boolean toggleOutlines(Minecraft minecraft) {
		ConfigManager manager = ConfigManager.instance();
		GlintsmithConfig copy = manager.get().copy();
		copy.outline.enabled = !copy.outline.enabled;
		boolean written = manager.save(copy);
		// What is in effect after saving: a failed save keeps the old state.
		boolean now = manager.get().outline.enabled;
		if (minecraft.player != null) {
			minecraft.player.displayClientMessage(Ui.t(now ? "glintsmith.message.outlines_on" : "glintsmith.message.outlines_off"), true);
		}
		if (!written && manager.lastError() != null) {
			minecraft.getToasts().addToast(SystemToast.multiline(minecraft, SAVE_FAILED,
				Ui.t("glintsmith.save_failed").withStyle(ChatFormatting.RED),
				Component.literal(manager.lastError()).withStyle(ChatFormatting.RED)));
		}
		GuiHooks.onToggleOutlines.run();
		return now;
	}
}
