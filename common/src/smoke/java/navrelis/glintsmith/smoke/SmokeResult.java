package navrelis.glintsmith.smoke;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Collects what the smoke run did and writes {@code smoke-result.json} (docs/TESTING.md). Thread safe: the watchdog
 * thread may write the result while the render thread is still running a step.
 */
final class SmokeResult {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

	private final Path file;
	private final long startedNanos = System.nanoTime();
	private final JsonObject root = new JsonObject();
	private final JsonArray steps = new JsonArray();
	private boolean written;
	private boolean allOk = true;

	SmokeResult(Path file, String minecraft, String loader, String glintsmithVersion) {
		this.file = file;
		root.addProperty("minecraft", minecraft);
		root.addProperty("loader", loader);
		root.addProperty("glintsmithVersion", glintsmithVersion);
		root.add("steps", steps);
	}

	synchronized void put(String key, String value) {
		root.addProperty(key, value);
	}

	synchronized void put(String key, Number value) {
		root.addProperty(key, value);
	}

	synchronized void put(String key, boolean value) {
		root.addProperty(key, value);
	}

	synchronized void put(String key, com.google.gson.JsonElement value) {
		root.add(key, value);
	}

	synchronized void step(String name, boolean ok, String error, long millis, JsonObject extra) {
		JsonObject step = new JsonObject();
		step.addProperty("name", name);
		step.addProperty("ok", ok);
		if (error != null) {
			step.addProperty("error", error);
		}
		step.addProperty("durationMs", millis);
		if (extra != null) {
			extra.entrySet().forEach(e -> step.add(e.getKey(), e.getValue()));
		}
		steps.add(step);
		allOk &= ok;
	}

	/** Writes the result (once). {@code fatal} is a run-level failure such as a watchdog timeout, or null. */
	synchronized void finish(String fatal) {
		if (written) {
			return;
		}
		written = true;
		if (fatal != null) {
			root.addProperty("fatal", fatal);
		}
		root.addProperty("passed", allOk && fatal == null);
		root.addProperty("durationMs", (System.nanoTime() - startedNanos) / 1_000_000L);
		try {
			Files.createDirectories(file.toAbsolutePath().getParent());
			Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
			Files.writeString(tmp, GSON.toJson(root), StandardCharsets.UTF_8);
			Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			SmokeRunner.LOGGER.error("{}could not write {}", SmokeRunner.PREFIX, file, e);
		}
	}

	synchronized boolean isWritten() {
		return written;
	}
}
