package navrelis.glintsmith.gui;

/**
 * Sets up everything the GUI adds to the game that does not need a loader: the key bindings. The loader layer
 * registers them ({@link GlintsmithKeys#mappings()}) and the client command ({@link GlintsmithCommands#build()}).
 * Called once from {@code GlintsmithClient.init()}.
 */
public final class GuiBootstrap {
	private GuiBootstrap() {
	}

	public static void init() {
		GlintsmithKeys.mappings();
	}
}
