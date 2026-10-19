package navrelis.glintsmith.gui;

import java.util.List;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import navrelis.glintsmith.config.ConfigManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * The client command {@code /glintsmith}: opens the screen; {@code reload} re-reads the config file;
 * {@code toggle} flips the outline switch. {@link #build()} is loader-neutral: the loader layer registers the
 * result with its client command dispatcher.
 */
public final class GlintsmithCommands {
	private static final int MAX_WARNINGS = 5;

	private GlintsmithCommands() {
	}

	/**
	 * @param <S> the command source type of the loader's client command dispatcher; the command never reads it
	 * @return the command tree, ready to be registered
	 */
	public static <S> LiteralArgumentBuilder<S> build() {
		return LiteralArgumentBuilder.<S>literal("glintsmith")
			.executes(command -> {
				GlintsmithKeys.requestOpenScreen();
				return 1;
			})
			.then(LiteralArgumentBuilder.<S>literal("reload").executes(command -> {
				ConfigManager manager = ConfigManager.instance();
				manager.load();
				String error = manager.lastError();
				if (error != null) {
					sendError(Component.literal(error));
					return 0;
				}
				sendFeedback(Ui.t("glintsmith.command.reloaded").withStyle(ChatFormatting.GREEN));
				List<String> warnings = manager.warnings();
				for (int i = 0; i < Math.min(MAX_WARNINGS, warnings.size()); i++) {
					sendFeedback(Component.literal(warnings.get(i)).withStyle(ChatFormatting.YELLOW));
				}
				if (warnings.size() > MAX_WARNINGS) {
					sendFeedback(Ui.t("glintsmith.command.more_warnings", warnings.size() - MAX_WARNINGS)
						.withStyle(ChatFormatting.YELLOW));
				}
				return 1;
			}))
			.then(LiteralArgumentBuilder.<S>literal("toggle").executes(command -> {
				GlintsmithKeys.toggleOutlines(Minecraft.getInstance());
				return 1;
			}));
	}

	private static void sendFeedback(Component message) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player != null) {
			minecraft.player.displayClientMessage(message, false);
		}
	}

	private static void sendError(Component message) {
		sendFeedback(message.copy().withStyle(ChatFormatting.RED));
	}
}
