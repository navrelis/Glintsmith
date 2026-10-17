package navrelis.glintsmith.render.glint;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.mojang.blaze3d.platform.NativeImage;
import navrelis.glintsmith.Glintsmith;
import navrelis.glintsmith.config.ConfigManager;
import navrelis.glintsmith.config.GlintsmithConfig;
import navrelis.glintsmith.style.GlintSettings;
import navrelis.glintsmith.style.Rule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Nullable;

/**
 * Glint texture specs to {@link ResourceLocation}s, and the textures behind them. Render thread only (except
 * {@link #presetSpecs()}).
 * <p>
 * Grammar (see {@link GlintSettings#validateTexture}): {@code null}, {@code vanilla} and {@code default} are the
 * vanilla texture (the neutral grey variant if a tint is set); {@code preset:<name>} is a generated preset;
 * {@code file:<name>.png} is a PNG in {@code config/glintsmith/glints}; anything else is a resource pack texture.
 * An invalid or unavailable spec logs one warning and falls back to the vanilla texture.
 */
public final class GlintTextures {
	static final ResourceLocation VANILLA_ITEM = ResourceLocation.withDefaultNamespace("textures/misc/enchanted_glint_item.png");
	static final ResourceLocation VANILLA_ENTITY = ResourceLocation.withDefaultNamespace("textures/misc/enchanted_glint_entity.png");
	static final ResourceLocation NEUTRAL_ITEM = own("glint/preset/neutral_item");
	static final ResourceLocation NEUTRAL_ENTITY = own("glint/preset/neutral_entity");

	/** The values of {@link #presetSpecs()} that do not depend on the files in the texture folder. */
	static final List<String> BASE_SPECS = List.of(
		"vanilla", "preset:neutral", "preset:rainbow", "preset:aurora", "preset:fire", "preset:frost",
		"preset:sparkle", "preset:gold", "preset:void");

	private static final byte[] PNG_MAGIC = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
	private static final int MAX_FILE_BYTES = 16 * 1024 * 1024;
	private static final int MAX_FILE_SIZE_PX = 1024;

	private static final Map<String, ResourceLocation[]> CACHE = new HashMap<>();
	private static final Set<String> READY_PRESETS = new HashSet<>();
	private static final Map<String, FileEntry> FILES = new HashMap<>();
	private static final Set<String> FAILED_FILES = new HashSet<>();
	private static final Set<String> WARNED = new HashSet<>();
	private static boolean neutralItemReady;
	private static boolean neutralEntityReady;

	private GlintTextures() {
	}

	private static ResourceLocation own(String path) {
		return ResourceLocation.fromNamespaceAndPath(Glintsmith.MOD_ID, path);
	}

	// ---------------------------------------------------------------- public API

	/**
	 * The texture specs the UI offers: {@code vanilla}, the presets, then {@code file:<name>.png} for every valid PNG
	 * currently in the texture folder, sorted by name. Never throws; if the folder cannot be read only the fixed entries
	 * are returned.
	 */
	public static List<String> presetSpecs() {
		try {
			return presetSpecs(ConfigManager.instance().textureDir());
		} catch (Throwable t) {
			return BASE_SPECS;
		}
	}

	/**
	 * Same as {@link #presetSpecs()} for an arbitrary folder.
	 */
	static List<String> presetSpecs(@Nullable Path dir) {
		List<String> specs = new ArrayList<>(BASE_SPECS);
		if (dir == null) {
			return specs;
		}
		List<String> names = new ArrayList<>();
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
			for (Path file : stream) {
				String name = file.getFileName().toString();
				if (GlintSettings.validateTexture(GlintSettings.FILE_PREFIX + name) == null && isPng(file)) {
					names.add(name);
				}
			}
		} catch (IOException | RuntimeException e) {
			// unreadable folder: only the fixed entries
		}
		Collections.sort(names);
		for (String name : names) {
			specs.add(GlintSettings.FILE_PREFIX + name);
		}
		return specs;
	}

	// ---------------------------------------------------------------- package API

	/**
	 * Generates the presets (once) and builds the neutral textures from the current resources.
	 */
	static void init() {
		Minecraft mc = Minecraft.getInstance();
		TextureManager textures = mc.getTextureManager();
		for (String name : GlintPresets.GENERATED) {
			if (READY_PRESETS.contains(name)) {
				continue;
			}
			try {
				int[] pixels = GlintPresets.generate(name);
				register(textures, presetId(name), pixels, GlintPresets.SIZE, GlintPresets.SIZE);
				READY_PRESETS.add(name);
			} catch (Throwable t) {
				Glintsmith.LOGGER.warn("Could not generate the glint preset '{}'; it will look like the vanilla glint.", name, t);
			}
		}
		if (!neutralItemReady || !neutralEntityReady) {
			buildNeutral(mc.getResourceManager());
		}
		CACHE.clear();
	}

	/**
	 * Resource reload: rebuilds what depends on resource packs, reloads the file textures, forgets resolved specs.
	 */
	static void reload(ResourceManager manager) {
		buildNeutral(manager);
		FAILED_FILES.clear();
		for (String name : new ArrayList<>(FILES.keySet())) {
			loadFile(name);
		}
		CACHE.clear();
	}

	/**
	 * Config change: forgets resolved specs, loads the PNG files the config refers to (or that changed on disk) and
	 * releases the ones it no longer refers to.
	 */
	static void onConfigChanged(GlintsmithConfig config) {
		CACHE.clear();
		FAILED_FILES.clear();
		Set<String> wanted = referencedFiles(config);
		TextureManager textures = Minecraft.getInstance().getTextureManager();
		for (String name : new ArrayList<>(FILES.keySet())) {
			if (!wanted.contains(name)) {
				FileEntry entry = FILES.remove(name);
				textures.release(entry.id);
			} else if (changedOnDisk(name, FILES.get(name))) {
				loadFile(name);
			}
		}
		for (String name : wanted) {
			if (!FILES.containsKey(name)) {
				loadFile(name);
			}
		}
	}

	/**
	 * The texture to draw.
	 *
	 * @param spec a texture spec; null, {@code vanilla} and {@code default} mean the vanilla texture
	 * @param entityFamily true for the kinds that use the entity glint texture
	 * @param tinted true if a tint is set (the vanilla texture is then replaced by the neutral grey one)
	 */
	static ResourceLocation resolve(@Nullable String spec, boolean entityFamily, boolean tinted) {
		if (GlintSettings.isVanillaTexture(spec)) {
			if (tinted) {
				ResourceLocation neutral = neutral(entityFamily);
				if (neutral != null) {
					return neutral;
				}
			}
			return entityFamily ? VANILLA_ENTITY : VANILLA_ITEM;
		}
		ResourceLocation[] pair = CACHE.get(spec);
		if (pair == null) {
			pair = compute(spec);
			CACHE.put(spec, pair);
		}
		return pair[entityFamily ? 1 : 0];
	}

	/**
	 * @return the names ({@code foo.png}) of the PNG files the config refers to through {@code file:} specs
	 */
	static Set<String> referencedFiles(@Nullable GlintsmithConfig config) {
		Set<String> names = new HashSet<>();
		if (config == null) {
			return names;
		}
		if (config.glint != null) {
			for (var category : new GlintsmithConfig.GlintCategorySettings[] {
				config.glint.item, config.glint.armor, config.glint.entity, config.glint.gui}) {
				if (category != null) {
					addFile(names, category.texture);
				}
			}
		}
		if (config.rules != null) {
			for (Rule rule : config.rules) {
				if (rule != null && rule.enabled && rule.error == null && rule.glint != null) {
					addFile(names, rule.glint.texture);
				}
			}
		}
		return names;
	}

	/**
	 * Lower case, and every character outside {@code [a-z0-9_.-]} becomes {@code _}.
	 */
	static String sanitizeName(String name) {
		String lower = name.toLowerCase(Locale.ROOT);
		StringBuilder out = new StringBuilder(lower.length());
		for (int i = 0; i < lower.length(); i++) {
			char c = lower.charAt(i);
			boolean ok = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '.' || c == '-';
			out.append(ok ? c : '_');
		}
		return out.toString();
	}

	/**
	 * @return the id of the texture a {@code file:<name>} spec is loaded under
	 */
	static ResourceLocation fileId(String name) {
		return own("glint/file/" + sanitizeName(name));
	}

	static ResourceLocation presetId(String name) {
		return own("glint/preset/" + name);
	}

	/** Test hook. */
	static void clearStateForTests() {
		CACHE.clear();
		READY_PRESETS.clear();
		FILES.clear();
		FAILED_FILES.clear();
		WARNED.clear();
		neutralItemReady = false;
		neutralEntityReady = false;
	}

	// ---------------------------------------------------------------- internals

	private static void addFile(Set<String> names, @Nullable String spec) {
		if (spec != null && spec.startsWith(GlintSettings.FILE_PREFIX) && GlintSettings.validateTexture(spec) == null) {
			names.add(spec.substring(GlintSettings.FILE_PREFIX.length()));
		}
	}

	@Nullable
	private static ResourceLocation neutral(boolean entityFamily) {
		if (entityFamily) {
			return neutralEntityReady ? NEUTRAL_ENTITY : null;
		}
		return neutralItemReady ? NEUTRAL_ITEM : null;
	}

	private static ResourceLocation[] compute(String spec) {
		ResourceLocation fallbackItem = VANILLA_ITEM;
		ResourceLocation fallbackEntity = VANILLA_ENTITY;
		try {
			String problem = GlintSettings.validateTexture(spec);
			if (problem != null) {
				warnOnce(spec, problem);
				return new ResourceLocation[] {fallbackItem, fallbackEntity};
			}
			if (spec.startsWith(GlintSettings.PRESET_PREFIX)) {
				String name = spec.substring(GlintSettings.PRESET_PREFIX.length());
				if ("neutral".equals(name)) {
					return new ResourceLocation[] {
						neutral(false) != null ? NEUTRAL_ITEM : fallbackItem,
						neutral(true) != null ? NEUTRAL_ENTITY : fallbackEntity};
				}
				if (READY_PRESETS.contains(name)) {
					ResourceLocation id = presetId(name);
					return new ResourceLocation[] {id, id};
				}
				warnOnce(spec, "The glint preset '" + name + "' is not available");
				return new ResourceLocation[] {fallbackItem, fallbackEntity};
			}
			if (spec.startsWith(GlintSettings.FILE_PREFIX)) {
				String name = spec.substring(GlintSettings.FILE_PREFIX.length());
				FileEntry entry = FILES.get(name);
				if (entry == null && !FAILED_FILES.contains(name)) {
					entry = loadFile(name);
				}
				if (entry == null) {
					warnOnce(spec, "The glint texture file '" + name + "' could not be loaded");
					return new ResourceLocation[] {fallbackItem, fallbackEntity};
				}
				return new ResourceLocation[] {entry.id, entry.id};
			}
			ResourceLocation location = ResourceLocation.tryParse(spec);
			if (location == null || Minecraft.getInstance().getResourceManager().getResource(location).isEmpty()) {
				warnOnce(spec, "The glint texture '" + spec + "' does not exist");
				return new ResourceLocation[] {fallbackItem, fallbackEntity};
			}
			return new ResourceLocation[] {location, location};
		} catch (RuntimeException e) {
			warnOnce(spec, "The glint texture '" + spec + "' could not be resolved: " + e);
			return new ResourceLocation[] {fallbackItem, fallbackEntity};
		}
	}

	private static void warnOnce(String spec, String message) {
		if (WARNED.add(spec)) {
			Glintsmith.LOGGER.warn("Glint texture '{}': {}. Using the vanilla glint texture.", spec, message);
		}
	}

	// ---- file textures

	/** The result of loading one PNG; the stamp tells whether it changed on disk. */
	private record FileEntry(ResourceLocation id, long modified, long size) {
	}

	@Nullable
	private static FileEntry loadFile(String name) {
		try {
			Path dir = ConfigManager.instance().textureDir();
			Path file = dir.resolve(name);
			if (!Files.isRegularFile(file) || Files.size(file) > MAX_FILE_BYTES) {
				throw new IOException("not a regular file or larger than " + MAX_FILE_BYTES / (1024 * 1024) + " MB");
			}
			byte[] data = Files.readAllBytes(file);
			long modified = Files.getLastModifiedTime(file).toMillis();
			ResourceLocation id = fileId(name);
			try (NativeImage image = NativeImage.read(data)) {
				if (image.getWidth() > MAX_FILE_SIZE_PX || image.getHeight() > MAX_FILE_SIZE_PX) {
					throw new IOException("larger than " + MAX_FILE_SIZE_PX + "x" + MAX_FILE_SIZE_PX + " pixels ("
						+ image.getWidth() + "x" + image.getHeight() + ")");
				}
				register(Minecraft.getInstance().getTextureManager(), id, image.getPixelsRGBA(), image.getWidth(), image.getHeight());
			}
			FileEntry entry = new FileEntry(id, modified, data.length);
			FILES.put(name, entry);
			FAILED_FILES.remove(name);
			return entry;
		} catch (IOException | RuntimeException e) {
			FAILED_FILES.add(name);
			FILES.remove(name);
			warnOnce(GlintSettings.FILE_PREFIX + name, "Could not load " + name + " from the glints folder: " + e.getMessage());
			return null;
		}
	}

	private static boolean changedOnDisk(String name, @Nullable FileEntry entry) {
		if (entry == null) {
			return true;
		}
		try {
			Path file = ConfigManager.instance().textureDir().resolve(name);
			return Files.getLastModifiedTime(file).toMillis() != entry.modified || Files.size(file) != entry.size;
		} catch (IOException | RuntimeException e) {
			return true;
		}
	}

	private static boolean isPng(Path file) {
		try (InputStream in = Files.newInputStream(file)) {
			byte[] head = in.readNBytes(PNG_MAGIC.length);
			return java.util.Arrays.equals(head, PNG_MAGIC);
		} catch (IOException | RuntimeException e) {
			return false;
		}
	}

	// ---- neutral textures

	private static void buildNeutral(ResourceManager manager) {
		TextureManager textures = Minecraft.getInstance().getTextureManager();
		neutralItemReady = buildNeutral(manager, textures, VANILLA_ITEM, NEUTRAL_ITEM);
		neutralEntityReady = buildNeutral(manager, textures, VANILLA_ENTITY, NEUTRAL_ENTITY);
		CACHE.clear();
	}

	private static boolean buildNeutral(ResourceManager manager, TextureManager textures, ResourceLocation source, ResourceLocation target) {
		try {
			Optional<Resource> resource = manager.getResource(source);
			if (resource.isEmpty()) {
				Glintsmith.LOGGER.warn("The vanilla glint texture {} is missing; tinted glints will use it directly.", source);
				return false;
			}
			try (InputStream in = resource.get().open(); NativeImage image = NativeImage.read(in)) {
				int[] grey = GlintPresets.neutral(image.getPixelsRGBA());
				register(textures, target, grey, image.getWidth(), image.getHeight());
			}
			return true;
		} catch (IOException | RuntimeException e) {
			Glintsmith.LOGGER.warn("Could not build the neutral glint texture {}; tinted glints will use the vanilla texture.", target, e);
			return false;
		}
	}

	/**
	 * Uploads ABGR pixels as a texture. An existing texture under the same id is closed by the texture manager.
	 */
	private static void register(TextureManager textures, ResourceLocation id, int[] abgr, int width, int height) {
		NativeImage image = new NativeImage(width, height, false);
		try {
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					image.setPixelRGBA(x, y, abgr[y * width + x]);
				}
			}
		} catch (RuntimeException e) {
			image.close();
			throw e;
		}
		textures.register(id, new DynamicTexture(image));
	}
}
