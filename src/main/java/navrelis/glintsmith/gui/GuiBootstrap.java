package navrelis.glintsmith.gui;

/**
 * Registers everything the GUI adds to the game: key bindings and the client command.
 * Called once from the client entry point.
 */
public final class GuiBootstrap {
	private GuiBootstrap() {
	}

	public static void init() {
		GlintsmithKeys.register();
		GlintsmithCommands.register();
	}
}
