package navrelis.glintsmith.config;

import navrelis.glintsmith.style.ColorStyle;

/**
 * Configs for tests that need a known baseline. The built-in defaults are a customised look, so tests that mean
 * "nothing customised" must not use {@code new GlintsmithConfig()}.
 */
public final class TestConfigs {
	private TestConfigs() {
	}

	/**
	 * The config of the old built-in defaults: a static purple outline (2 px, fully opaque, sharp), the vanilla glint on all
	 * four layers, and every other setting at its built-in default. Not sanitised, like {@code new GlintsmithConfig()}.
	 */
	public static GlintsmithConfig vanilla() {
		GlintsmithConfig c = new GlintsmithConfig();
		c.outline.color = ColorStyle.of(ColorStyle.DEFAULT_COLOR);
		c.outline.thickness = 2.0f;
		c.outline.softness = 0.0f;
		c.outline.opacity = 1.0f;
		c.glint.item = new GlintsmithConfig.GlintCategorySettings();
		c.glint.armor = new GlintsmithConfig.GlintCategorySettings();
		c.glint.entity = new GlintsmithConfig.GlintCategorySettings();
		c.glint.gui = new GlintsmithConfig.GlintCategorySettings();
		return c;
	}
}
