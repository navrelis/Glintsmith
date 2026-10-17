package navrelis.glintsmith.studio;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import org.jetbrains.annotations.Nullable;

/**
 * Writes the frames and the {@code scene.json} of a run, following the frame contract in docs/STUDIO.md:
 * {@code NNNN_k.png} (over black) and {@code NNNN_w.png} (over white) per frame, 1-based and zero-padded to 4 digits,
 * at the supersampled size; {@code scene.json} with the output size, the frame list and the looks. Existing files of
 * the same names are overwritten; nothing is ever deleted. PNGs are encoded on worker threads.
 */
final class FrameWriter {
	private static final int MAX_QUEUED = 12;
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().serializeNulls().create();

	private final Path dir;
	private final ExecutorService pool = Executors.newFixedThreadPool(3, r -> {
		Thread t = new Thread(r, "Glintsmith Studio PNG writer");
		t.setDaemon(true);
		return t;
	});
	private final List<Future<?>> pending = new ArrayList<>();
	private final JsonArray frames = new JsonArray();

	FrameWriter(Path dir) throws IOException {
		this.dir = dir;
		Files.createDirectories(dir);
	}

	static String file(int index) {
		return String.format("%04d", index);
	}

	/**
	 * Queues one image; the image is closed after it was written. Blocks while too many images are still waiting (each
	 * holds a native buffer of the full supersampled size).
	 */
	void write(NativeImage image, int index, boolean white) throws Exception {
		int waiting = 0;
		for (int i = pending.size() - 1; i >= 0 && waiting <= MAX_QUEUED; i--) {
			if (!pending.get(i).isDone()) {
				waiting++;
			}
		}
		if (waiting > MAX_QUEUED) {
			for (Future<?> f : pending) {
				if (!f.isDone()) {
					f.get(5, TimeUnit.MINUTES);
					break;
				}
			}
		}
		Path path = dir.resolve(file(index) + (white ? "_w.png" : "_k.png"));
		pending.add(pool.submit(() -> {
			try (image) {
				image.writeToFile(path);
			}
			return null;
		}));
	}

	void addFrame(int index, String look, String caption, @Nullable String name) {
		JsonObject frame = new JsonObject();
		frame.addProperty("index", index);
		frame.addProperty("file", file(index));
		frame.addProperty("look", look);
		frame.addProperty("caption", caption);
		if (name == null) {
			frame.add("name", JsonNull.INSTANCE);
		} else {
			frame.addProperty("name", name);
		}
		frames.add(frame);
	}

	/**
	 * Waits for every PNG, then writes {@code scene.json}.
	 *
	 * @throws IOException if any image could not be written
	 */
	void finish(Scene scene, List<Scene.Look> looks, String commit) throws Exception {
		pool.shutdown();
		for (Future<?> f : pending) {
			f.get(5, TimeUnit.MINUTES);
		}
		JsonObject root = new JsonObject();
		root.addProperty("scene", scene.id);
		root.addProperty("width", scene.width);
		root.addProperty("height", scene.height);
		root.addProperty("supersample", scene.supersample);
		root.addProperty("fps", scene.fps);
		root.add("frames", frames);
		JsonArray lookArray = new JsonArray();
		for (Scene.Look look : looks) {
			JsonObject l = new JsonObject();
			l.addProperty("id", look.id());
			l.addProperty("caption", look.caption());
			lookArray.add(l);
		}
		root.add("looks", lookArray);
		root.addProperty("commit", commit);
		root.addProperty("created", OffsetDateTime.now().withNano(0).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
		Files.writeString(dir.resolve("scene.json"), GSON.toJson(root) + "\n", StandardCharsets.UTF_8);
	}

	/** Stops the workers without waiting (failed run). */
	void abort() {
		pool.shutdownNow();
	}

	/** @return the short hash of the repository HEAD, or {@code unknown} */
	static String commit(Path repo) {
		try {
			Process p = new ProcessBuilder("git", "-C", repo.toString(), "rev-parse", "--short", "HEAD")
				.redirectErrorStream(true).start();
			if (!p.waitFor(10, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				return "unknown";
			}
			String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
			return p.exitValue() == 0 && out.matches("[0-9a-f]{4,40}") ? out : "unknown";
		} catch (IOException | InterruptedException e) {
			return "unknown";
		}
	}
}
