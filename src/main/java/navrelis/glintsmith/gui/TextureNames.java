package navrelis.glintsmith.gui;

import navrelis.glintsmith.style.GlintSettings;

/**
 * Validation of glint texture names typed into the texture box. Pure Java.
 * <p>
 * The grammar is the one of {@link GlintSettings#validateTexture(String)}: {@code vanilla} / {@code default},
 * {@code preset:<name>}, {@code file:<name>.png} (a lower case file name in the glints folder of the config) or a
 * resource location {@code namespace:path}. An empty text is valid too: it means vanilla (global tab) or
 * inherit (rule editor).
 */
public final class TextureNames {
	private TextureNames() {
	}

	/**
	 * @return {@code true} for an empty text or a text that {@link GlintSettings#validateTexture(String)} accepts
	 */
	public static boolean isValid(String text) {
		if (text == null) {
			return true;
		}
		String s = text.trim();
		return s.isEmpty() || GlintSettings.validateTexture(s) == null;
	}
}
