package navrelis.glintsmith.integration;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import navrelis.glintsmith.gui.GlintsmithScreen;

/**
 * Mod Menu entry: the "configure" button of Glintsmith opens the settings screen.
 * Mod Menu is optional; this class is only loaded by Mod Menu itself.
 */
public final class ModMenuIntegration implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return GlintsmithScreen::new;
	}
}
