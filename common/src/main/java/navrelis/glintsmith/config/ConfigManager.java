package navrelis.glintsmith.config;

import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import navrelis.glintsmith.platform.Services;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads, saves and live-reloads {@code config/glintsmith.json}.
 * <p>
 * Use {@link #instance()} in the game. Everything runs on the calling thread, there are no background threads:
 * call {@link #tick()} once per client tick for live reload. Nothing here throws to the caller; problems are logged
 * once and kept in {@link #lastError()} for the UI.
 * <p>
 * This class knows nothing about rendering. Listeners (for example the rule engine cache) are added from outside.
 */
public final class ConfigManager {
	public static final String FILE_NAME = "glintsmith.json";
	private static final Logger LOGGER = LoggerFactory.getLogger("Glintsmith/Config");
	private static final int CHECK_INTERVAL_TICKS = 20;

	private final Path file;
	private final List<Consumer<GlintsmithConfig>> listeners = new CopyOnWriteArrayList<>();
	private volatile GlintsmithConfig current = new GlintsmithConfig().sanitize();
	private volatile String lastError;
	private int tickCounter;
	/** Modification time and size of the file as we last read or wrote it; -1 when it did not exist. */
	private long knownModified = -1L;
	private long knownSize = -1L;

	private ConfigManager(Path file) {
		this.file = file;
	}

	/**
	 * The manager of the game's config directory. Created and loaded on first use.
	 */
	public static ConfigManager instance() {
		return Holder.INSTANCE;
	}

	/**
	 * Shortcut for {@code instance().get()}.
	 */
	public static GlintsmithConfig current() {
		return Holder.INSTANCE.current;
	}

	/**
	 * A detached manager on an arbitrary file, not loaded yet. Meant for tests.
	 */
	public static ConfigManager forPath(Path file) {
		return new ConfigManager(file);
	}

	private static final class Holder {
		static final ConfigManager INSTANCE = create();

		private static ConfigManager create() {
			ConfigManager manager = new ConfigManager(Services.PLATFORM.configDir().resolve(FILE_NAME));
			manager.load();
			return manager;
		}
	}

	/**
	 * @return the current config; treat it as read-only and edit a {@link GlintsmithConfig#copy()} instead
	 */
	public GlintsmithConfig get() {
		return current;
	}

	/**
	 * @return the path of the config file
	 */
	public Path path() {
		return file;
	}

	/**
	 * @return the folder for custom glint textures ({@code config/glintsmith/glints}), created if possible
	 */
	public Path textureDir() {
		Path folder = file.toAbsolutePath().getParent().resolve("glintsmith").resolve("glints");
		try {
			Files.createDirectories(folder);
		} catch (IOException e) {
			LOGGER.warn("Could not create {}: {}", folder, e.toString());
		}
		return folder;
	}

	/**
	 * @return problems the last load or save repaired in the current config, such as an invalid category texture
	 *         that was reset; empty if none
	 */
	public List<String> warnings() {
		return current.warnings();
	}

	/**
	 * @return the message of the last failed load or save, or null if the last one worked
	 */
	public String lastError() {
		return lastError;
	}

	/**
	 * Registers a listener that runs on the calling thread after every load and save, with the new config.
	 * Exceptions in a listener are caught and logged.
	 */
	public void addListener(Consumer<GlintsmithConfig> listener) {
		listeners.add(listener);
	}

	public void removeListener(Consumer<GlintsmithConfig> listener) {
		listeners.remove(listener);
	}

	/**
	 * Reads the file. A missing file is created with the defaults. A file that cannot be parsed keeps the previous
	 * config (the defaults on first load), logs one warning and sets {@link #lastError()}.
	 * Listeners are notified either way.
	 *
	 * @return the config that is current afterwards
	 */
	public synchronized GlintsmithConfig load() {
		if (Files.notExists(file)) {
			GlintsmithConfig defaults = new GlintsmithConfig().sanitize();
			current = defaults;
			lastError = null;
			if (!writeFile(defaults)) {
				recordStamp();
			}
			notifyListeners(defaults);
			return defaults;
		}

		try {
			String text = Files.readString(file, StandardCharsets.UTF_8);
			GlintsmithConfig parsed = parse(text);
			parsed.sanitize();
			for (String warning : parsed.warnings()) {
				LOGGER.warn("Config {}: {}", FILE_NAME, warning);
			}
			current = parsed;
			lastError = null;
		} catch (IOException | JsonParseException | IllegalStateException | IllegalArgumentException e) {
			lastError = "Could not read " + FILE_NAME + ": " + e.getMessage();
			LOGGER.warn("{} Keeping the previous settings.", lastError);
		}
		recordStamp();
		GlintsmithConfig result = current;
		notifyListeners(result);
		return result;
	}

	/**
	 * Sanitises a copy of the config, writes it atomically, makes it current and notifies the listeners.
	 * The in-memory config is updated even if writing fails.
	 *
	 * @return {@code true} if the file was written
	 */
	public synchronized boolean save(GlintsmithConfig config) {
		GlintsmithConfig stored = config.copy();
		current = stored;
		boolean written = writeFile(stored);
		notifyListeners(stored);
		return written;
	}

	/**
	 * Call once per client tick. Every 20th call checks whether the file changed on disk (and was not written by us)
	 * and reloads it.
	 */
	public void tick() {
		if (++tickCounter < CHECK_INTERVAL_TICKS) {
			return;
		}
		tickCounter = 0;
		long modified = -1L;
		long size = -1L;
		try {
			BasicFileAttributes attributes = Files.readAttributes(file, BasicFileAttributes.class);
			modified = attributes.lastModifiedTime().toMillis();
			size = attributes.size();
		} catch (IOException ignored) {
			// missing file: modified stays -1
		}
		boolean changed;
		synchronized (this) {
			changed = modified != knownModified || size != knownSize;
		}
		if (changed) {
			LOGGER.info("{} changed on disk, reloading.", FILE_NAME);
			load();
		}
	}

	/** Serialises a config the way it is stored in the file. */
	public static String toJson(GlintsmithConfig config) {
		return GlintsmithConfig.GSON.toJson(config);
	}

	/**
	 * Strict parse: comments, single quotes and trailing text are errors.
	 *
	 * @throws JsonParseException if the text is not a valid config object
	 */
	public static GlintsmithConfig fromJson(String text) {
		try {
			JsonReader reader = new JsonReader(new StringReader(text));
			reader.setLenient(false);
			TypeAdapter<GlintsmithConfig> adapter = GlintsmithConfig.GSON.getAdapter(GlintsmithConfig.class);
			GlintsmithConfig config = adapter.read(reader);
			if (reader.peek() != JsonToken.END_DOCUMENT) {
				throw new JsonParseException("unexpected content after the end of the config");
			}
			if (config == null) {
				throw new JsonParseException("the file does not contain a config object");
			}
			return config;
		} catch (IOException | IllegalStateException e) {
			throw new JsonParseException(e.getMessage() == null ? "malformed JSON" : e.getMessage(), e);
		}
	}

	private static GlintsmithConfig parse(String text) {
		if (text.isBlank()) {
			throw new JsonParseException("the file is empty");
		}
		return fromJson(text);
	}

	private void notifyListeners(GlintsmithConfig config) {
		for (Consumer<GlintsmithConfig> listener : listeners) {
			try {
				listener.accept(config);
			} catch (RuntimeException e) {
				LOGGER.warn("A config listener failed.", e);
			}
		}
	}

	private void recordStamp() {
		try {
			BasicFileAttributes attributes = Files.readAttributes(file, BasicFileAttributes.class);
			knownModified = attributes.lastModifiedTime().toMillis();
			knownSize = attributes.size();
		} catch (IOException e) {
			knownModified = -1L;
			knownSize = -1L;
		}
	}

	/**
	 * Writes through a temporary file in the same folder, then moves it into place, atomically where supported.
	 */
	private boolean writeFile(GlintsmithConfig config) {
		Path folder = file.toAbsolutePath().getParent();
		Path temp = folder.resolve(file.getFileName() + ".tmp");
		try {
			Files.createDirectories(folder);
			ByteBuffer data = ByteBuffer.wrap(toJson(config).getBytes(StandardCharsets.UTF_8));
			try (FileChannel channel = FileChannel.open(temp,
				StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
				while (data.hasRemaining()) {
					channel.write(data);
				}
				channel.force(true);
			}
			try {
				Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
			}
			lastError = null;
			recordStamp();
			return true;
		} catch (IOException | RuntimeException e) {
			lastError = "Could not save " + FILE_NAME + ": " + e.getMessage();
			LOGGER.warn(lastError);
			try {
				Files.deleteIfExists(temp);
			} catch (IOException ignored) {
				// nothing left to do
			}
			return false;
		}
	}
}
