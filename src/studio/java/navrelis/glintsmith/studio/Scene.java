package navrelis.glintsmith.studio;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.world.item.ItemDisplayContext;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * A studio scene, read from JSON (format: docs/STUDIO.md). Immutable after {@link #load}; every problem is reported as
 * an {@link IllegalArgumentException} with the JSON path, so a broken scene fails the run with one clear log line.
 */
final class Scene {
	enum Type {
		TURNTABLE,
		STILL
	}

	enum Kind {
		ITEM,
		ARMOR_STAND
	}

	/** Camera of a scene or a shot. Angles in degrees, lengths in blocks. */
	record Camera(boolean orthographic, float pitch, float yaw, float roll, float distance, float fov, float viewHeight,
		float zoom, Vector3f target) {
	}

	/**
	 * One thing on the turntable.
	 *
	 * @param item      item spec (string in {@code /give} syntax or an object), for {@link Kind#ITEM}
	 * @param equipment slot name to item spec, for {@link Kind#ARMOR_STAND}
	 * @param nbt       extra armor stand NBT (SNBT), merged over {@code {ShowArms:1b}}
	 * @param pos       position of the subject's centre (items) or of the stand's mid-height point
	 * @param rotation  fixed orientation (degrees X, Y, Z) applied before the turntable
	 * @param pivot     point of the subject that sits on the turntable axis, in its own rotated, unscaled coordinates
	 * @param phase     turntable phase offset in degrees
	 * @param spin      whether the subject turns at all
	 * @param look      id of a look used for this subject only (instead of the frame's look), or null
	 */
	record Subject(Kind kind, @Nullable JsonElement item, Map<String, JsonElement> equipment, @Nullable String nbt,
		Vector3f pos, Vector3f rotation, float scale, Vector3f pivot, float phase, boolean spin, ItemDisplayContext context,
		@Nullable String look) {
	}

	/** A look: a partial Glintsmith config applied in memory, plus its caption. */
	record Look(String id, String caption, JsonObject json) {
	}

	/** One still: look, turntable angle, optional camera pitch / camera / subjects / time overrides. */
	record Shot(@Nullable String name, String look, float yaw, @Nullable Camera camera, @Nullable List<Subject> subjects,
		@Nullable Double time) {
	}

	final String id;
	final Type type;
	final int width;
	final int height;
	final int supersample;
	final int fps;
	final int framesPerLook;
	final double turnsPerLook;
	final double swing;
	final double timeOffset;
	final Camera camera;
	final Vector3f keyLight;
	final Vector3f fillLight;
	final List<Subject> subjects;
	final Map<String, Look> looks;
	final List<String> sequence;
	final List<Shot> shots;
	final int settleFrames;
	@Nullable
	final String output;

	private Scene(JsonObject root) {
		id = string(root, "scene", null);
		if (!id.matches("[a-z0-9_.-]+")) {
			throw new IllegalArgumentException("scene: the id must match [a-z0-9_.-]+ (it names the output folder), got '" + id + "'");
		}
		type = enumOf(Type.class, string(root, "type", "turntable"), "type");
		width = positive(integer(root, "width", 512), "width");
		height = positive(integer(root, "height", 512), "height");
		supersample = range(integer(root, "supersample", 2), 1, 8, "supersample");
		fps = range(integer(root, "fps", 30), 1, 240, "fps");
		double seconds = number(root, "seconds_per_look", 3.0);
		framesPerLook = positive(integer(root, "frames_per_look", (int) Math.round(seconds * fps)), "frames_per_look");
		turnsPerLook = number(root, "turns_per_look", 1.0);
		swing = number(root, "swing", 0.0);
		timeOffset = number(root, "time_offset", 0.0);
		settleFrames = range(integer(root, "settle_frames", 3), 1, 600, "settle_frames");
		output = root.has("output") ? string(root, "output", null) : null;

		JsonObject cameraJson = object(root, "camera");
		camera = camera(cameraJson, "camera");

		JsonObject lights = object(root, "lights");
		keyLight = vector(lights, "key", new Vector3f(-0.45f, 0.75f, 0.6f), "lights.key");
		fillLight = vector(lights, "fill", new Vector3f(0.65f, 0.2f, 0.45f), "lights.fill");
		if (keyLight.lengthSquared() == 0.0f || fillLight.lengthSquared() == 0.0f) {
			throw new IllegalArgumentException("lights: a light direction must not be zero");
		}

		JsonObject layout = object(root, "layout");
		subjects = subjects(root.get("subjects"), layout, "subjects");

		looks = new LinkedHashMap<>();
		JsonElement looksJson = root.get("looks");
		if (looksJson == null || !looksJson.isJsonArray() || looksJson.getAsJsonArray().isEmpty()) {
			throw new IllegalArgumentException("looks: expected a non-empty array");
		}
		JsonArray lookArray = looksJson.getAsJsonArray();
		for (int i = 0; i < lookArray.size(); i++) {
			String path = "looks[" + i + "]";
			JsonObject look = asObject(lookArray.get(i), path);
			String lookId = string(look, "id", null);
			if (looks.containsKey(lookId)) {
				throw new IllegalArgumentException(path + ".id: duplicate look id '" + lookId + "'");
			}
			looks.put(lookId, new Look(lookId, string(look, "caption", lookId), look));
		}

		List<String> seq = new ArrayList<>();
		if (root.has("sequence")) {
			for (JsonElement e : asArray(root.get("sequence"), "sequence")) {
				seq.add(e.getAsString());
			}
		} else {
			seq.addAll(looks.keySet());
		}
		for (String lookId : seq) {
			requireLook(lookId, "sequence");
		}
		sequence = List.copyOf(seq);

		List<Shot> shotList = new ArrayList<>();
		if (type == Type.STILL) {
			JsonArray shotArray = asArray(root.get("shots"), "shots");
			if (shotArray.isEmpty()) {
				throw new IllegalArgumentException("shots: a still scene needs at least one shot");
			}
			for (int i = 0; i < shotArray.size(); i++) {
				String path = "shots[" + i + "]";
				JsonObject shot = asObject(shotArray.get(i), path);
				String lookId = string(shot, "look", sequence.get(0));
				requireLook(lookId, path + ".look");
				Camera shotCamera = null;
				if (shot.has("camera") || shot.has("pitch")) {
					JsonObject merged = cameraJson.deepCopy();
					if (shot.has("camera")) {
						for (Map.Entry<String, JsonElement> e : asObject(shot.get("camera"), path + ".camera").entrySet()) {
							merged.add(e.getKey(), e.getValue());
						}
					}
					if (shot.has("pitch")) {
						merged.add("pitch", shot.get("pitch"));
					}
					shotCamera = camera(merged, path + ".camera");
				}
				List<Subject> shotSubjects = shot.has("subjects") ? subjects(shot.get("subjects"), layout, path + ".subjects") : null;
				Double time = shot.has("time") ? number(shot, "time", 0.0) : null;
				String name = shot.has("name") && !shot.get("name").isJsonNull() ? shot.get("name").getAsString() : null;
				shotList.add(new Shot(name, lookId, (float) number(shot, "yaw", 0.0), shotCamera, shotSubjects, time));
			}
		}
		shots = List.copyOf(shotList);
		for (Subject s : subjects) {
			if (s.look() != null) {
				requireLook(s.look(), "subjects");
			}
		}
		for (Shot shot : shots) {
			if (shot.subjects() != null) {
				for (Subject s : shot.subjects()) {
					if (s.look() != null) {
						requireLook(s.look(), "shots.subjects");
					}
				}
			}
		}
	}

	/** Output size before downsampling. */
	int renderWidth() {
		return width * supersample;
	}

	int renderHeight() {
		return height * supersample;
	}

	/**
	 * Reads a scene from a file path, or, if no such file exists, from {@code scenes/<name>.json} on the classpath
	 * (the scenes in {@code src/studio/resources/scenes}).
	 */
	static Scene load(String spec) throws IOException {
		Path path = Path.of(spec);
		String text;
		if (Files.isRegularFile(path)) {
			text = Files.readString(path, StandardCharsets.UTF_8);
		} else {
			String name = spec.endsWith(".json") ? spec.substring(0, spec.length() - 5) : spec;
			try (InputStream in = Scene.class.getResourceAsStream("/scenes/" + name + ".json")) {
				if (in == null) {
					throw new IOException("scene not found: '" + spec + "' is neither a file nor a scene in src/studio/resources/scenes");
				}
				text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			}
		}
		JsonElement root = JsonParser.parseString(text);
		return new Scene(asObject(root, "(root)"));
	}

	private void requireLook(String lookId, String path) {
		if (!looks.containsKey(lookId)) {
			throw new IllegalArgumentException(path + ": unknown look '" + lookId + "' (defined: " + looks.keySet() + ")");
		}
	}

	private static Camera camera(JsonObject c, String path) {
		String projection = string(c, "projection", "orthographic").toLowerCase(Locale.ROOT);
		boolean ortho = switch (projection) {
			case "orthographic", "ortho" -> true;
			case "perspective" -> false;
			default -> throw new IllegalArgumentException(path + ".projection: expected orthographic or perspective, got '" + projection + "'");
		};
		float fov = (float) number(c, "fov", 30.0);
		if (!(fov > 1.0f && fov < 170.0f)) {
			throw new IllegalArgumentException(path + ".fov: expected 1..170 degrees");
		}
		float zoom = (float) number(c, "zoom", 1.0);
		float viewHeight = (float) number(c, "view_height", 1.4);
		float distance = (float) number(c, "distance", 4.0);
		if (!(zoom > 0.0f) || !(viewHeight > 0.0f) || !(distance > 0.1f)) {
			throw new IllegalArgumentException(path + ": zoom and view_height must be > 0, distance > 0.1");
		}
		return new Camera(ortho, (float) number(c, "pitch", 25.0), (float) number(c, "yaw", 0.0), (float) number(c, "roll", 0.0),
			distance, fov, viewHeight, zoom, vector(c, "target", new Vector3f(), path + ".target"));
	}

	private static List<Subject> subjects(@Nullable JsonElement json, JsonObject layout, String path) {
		JsonArray array = asArray(json, path);
		if (array.isEmpty()) {
			throw new IllegalArgumentException(path + ": expected at least one subject");
		}
		String layoutType = string(layout, "type", "none").toLowerCase(Locale.ROOT);
		float spacing = (float) number(layout, "spacing", 1.25);
		Vector3f origin = vector(layout, "origin", new Vector3f(), "layout.origin");
		List<Subject> list = new ArrayList<>();
		int n = array.size();
		for (int i = 0; i < n; i++) {
			String p = path + "[" + i + "]";
			JsonObject s = asObject(array.get(i), p);
			Kind kind = enumOf(Kind.class, string(s, "type", "item"), p + ".type");
			Vector3f defaultPos = new Vector3f(origin);
			if (layoutType.equals("row")) {
				defaultPos.x += (i - (n - 1) * 0.5f) * spacing;
			} else if (!layoutType.equals("none")) {
				throw new IllegalArgumentException("layout.type: expected row or none, got '" + layoutType + "'");
			}
			Map<String, JsonElement> equipment = new LinkedHashMap<>();
			if (s.has("equipment")) {
				for (Map.Entry<String, JsonElement> e : asObject(s.get("equipment"), p + ".equipment").entrySet()) {
					equipment.put(e.getKey().toLowerCase(Locale.ROOT), e.getValue());
				}
			}
			JsonElement item = s.get("item");
			if (kind == Kind.ITEM && item == null) {
				throw new IllegalArgumentException(p + ".item: missing");
			}
			String contextName = string(s, "context", "gui").toUpperCase(Locale.ROOT);
			ItemDisplayContext context;
			try {
				context = ItemDisplayContext.valueOf(contextName);
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException(p + ".context: unknown display context '" + contextName + "'");
			}
			String look = s.has("look") && !s.get("look").isJsonNull() ? s.get("look").getAsString() : null;
			String nbt = s.has("nbt") ? s.get("nbt").getAsString() : null;
			list.add(new Subject(kind, item, equipment, nbt, vector(s, "pos", defaultPos, p + ".pos"),
				vector(s, "rotation", new Vector3f(), p + ".rotation"), (float) number(s, "scale", 1.0),
				vector(s, "pivot", new Vector3f(), p + ".pivot"),
				(float) number(s, "phase", 0.0), !s.has("spin") || s.get("spin").getAsBoolean(), context, look));
		}
		return List.copyOf(list);
	}

	// ---------------------------------------------------------------- JSON helpers

	static JsonObject asObject(@Nullable JsonElement e, String path) {
		if (e == null || !e.isJsonObject()) {
			throw new IllegalArgumentException(path + ": expected an object");
		}
		return e.getAsJsonObject();
	}

	static JsonArray asArray(@Nullable JsonElement e, String path) {
		if (e == null || !e.isJsonArray()) {
			throw new IllegalArgumentException(path + ": expected an array");
		}
		return e.getAsJsonArray();
	}

	private static JsonObject object(JsonObject parent, String key) {
		JsonElement e = parent.get(key);
		return e == null || e.isJsonNull() ? new JsonObject() : asObject(e, key);
	}

	private static String string(JsonObject o, String key, @Nullable String fallback) {
		JsonElement e = o.get(key);
		if (e == null || e.isJsonNull()) {
			if (fallback == null) {
				throw new IllegalArgumentException(key + ": missing");
			}
			return fallback;
		}
		if (!e.isJsonPrimitive()) {
			throw new IllegalArgumentException(key + ": expected a string");
		}
		return e.getAsString();
	}

	private static double number(JsonObject o, String key, double fallback) {
		JsonElement e = o.get(key);
		if (e == null || e.isJsonNull()) {
			return fallback;
		}
		try {
			double v = e.getAsDouble();
			if (!Double.isFinite(v)) {
				throw new NumberFormatException();
			}
			return v;
		} catch (RuntimeException ex) {
			throw new IllegalArgumentException(key + ": expected a number");
		}
	}

	private static int integer(JsonObject o, String key, int fallback) {
		double v = number(o, key, fallback);
		if (v != Math.rint(v)) {
			throw new IllegalArgumentException(key + ": expected a whole number");
		}
		return (int) v;
	}

	private static Vector3f vector(JsonObject o, String key, Vector3f fallback, String path) {
		JsonElement e = o.get(key);
		if (e == null || e.isJsonNull()) {
			return fallback;
		}
		JsonArray a = asArray(e, path);
		if (a.size() != 3) {
			throw new IllegalArgumentException(path + ": expected [x, y, z]");
		}
		return new Vector3f(a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat());
	}

	private static int positive(int v, String key) {
		if (v <= 0) {
			throw new IllegalArgumentException(key + ": must be > 0");
		}
		return v;
	}

	private static int range(int v, int min, int max, String key) {
		if (v < min || v > max) {
			throw new IllegalArgumentException(key + ": must be " + min + ".." + max);
		}
		return v;
	}

	private static <E extends Enum<E>> E enumOf(Class<E> type, String name, String path) {
		try {
			return Enum.valueOf(type, name.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(path + ": unknown value '" + name + "'");
		}
	}
}
