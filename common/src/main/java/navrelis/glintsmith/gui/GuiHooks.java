package navrelis.glintsmith.gui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.stream.Stream;

import navrelis.glintsmith.config.ConfigManager;
import navrelis.glintsmith.style.GlintSettings;

/**
 * Plug points that other parts of the mod fill in at start-up, so the GUI does not depend on them.
 */
public final class GuiHooks {
	/**
	 * Texture specs offered by the glint texture selector (arrow buttons), in cycling order. The supplier is called
	 * when a row is built or a button is pressed, never per frame. The default lists {@code vanilla}, all presets and
	 * the PNG files of the glints folder, see {@link #defaultTextureSpecs(Path)}.
	 */
	public static Supplier<List<String>> texturePresets = () -> defaultTextureSpecs(ConfigManager.instance().textureDir());

	/** Runs after the outline toggle key or {@code /glintsmith toggle} flipped and saved the outline switch. */
	public static Runnable onToggleOutlines = () -> {
	};

	private GuiHooks() {
	}

	/**
	 * The default cycling list: {@code vanilla}, {@code preset:<name>} for every preset, then
	 * {@code file:<name>.png} for each PNG in {@code dir} whose name is a valid file spec (sorted).
	 * A missing or unreadable folder contributes no files.
	 */
	public static List<String> defaultTextureSpecs(Path dir) {
		List<String> specs = new ArrayList<>();
		specs.add(GlintSettings.VANILLA_TEXTURE);
		for (String preset : GlintSettings.PRESETS) {
			specs.add(GlintSettings.PRESET_PREFIX + preset);
		}
		List<String> files = new ArrayList<>();
		if (dir != null && Files.isDirectory(dir)) {
			try (Stream<Path> stream = Files.list(dir)) {
				stream.filter(Files::isRegularFile).forEach(path -> {
					String name = path.getFileName().toString();
					if (name.toLowerCase(Locale.ROOT).endsWith(".png")
						&& GlintSettings.validateTexture(GlintSettings.FILE_PREFIX + name) == null) {
						files.add(GlintSettings.FILE_PREFIX + name);
					}
				});
			} catch (IOException | RuntimeException e) {
				// unreadable folder: no files
			}
		}
		files.sort(null);
		specs.addAll(files);
		return specs;
	}
}
