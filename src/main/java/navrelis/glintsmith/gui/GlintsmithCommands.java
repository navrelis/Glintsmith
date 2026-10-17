package navrelis.glintsmith.gui;

import java.util.List;

import com.mojang.brigadier.CommandDispatcher;
import navrelis.glintsmith.config.ConfigManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.network.chat.Component;

/**
 * The client command {@code /glintsmith}: opens the screen; {@code reload} re-reads the config file;
 * {@code toggle} flips the outline switch.
 */
public final class GlintsmithCommands {
	private static final int MAX_WARNINGS = 5;

	private GlintsmithCommands() {
	}

	public static void register() {
		ClientCommandRegistrationCallback.EVENT.register(GlintsmithCommands::build);
	}

	private static void build(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandBuildContext context) {
		dispatcher.register(ClientCommandManager.literal("glintsmith")
			.executes(command -> {
				GlintsmithKeys.requestOpenScreen();
				return 1;
			})
			.then(ClientCommandManager.literal("reload").executes(command -> {
				ConfigManager manager = ConfigManager.instance();
				manager.load();
				String error = manager.lastError();
				if (error != null) {
					command.getSource().sendError(Component.literal(error));
					return 0;
				}
				command.getSource().sendFeedback(Ui.t("glintsmith.command.reloaded").withStyle(ChatFormatting.GREEN));
				List<String> warnings = manager.warnings();
				for (int i = 0; i < Math.min(MAX_WARNINGS, warnings.size()); i++) {
					command.getSource().sendFeedback(Component.literal(warnings.get(i)).withStyle(ChatFormatting.YELLOW));
				}
				if (warnings.size() > MAX_WARNINGS) {
					command.getSource().sendFeedback(Ui.t("glintsmith.command.more_warnings", warnings.size() - MAX_WARNINGS)
						.withStyle(ChatFormatting.YELLOW));
				}
				return 1;
			}))
			.then(ClientCommandManager.literal("toggle").executes(command -> {
				GlintsmithKeys.toggleOutlines(command.getSource().getClient());
				return 1;
			})));
	}
}
