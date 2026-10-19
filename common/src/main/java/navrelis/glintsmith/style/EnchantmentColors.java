package navrelis.glintsmith.style;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Colours per enchantment id: user override, then built-in default, then a stable hashed colour,
 * so every modded enchantment gets its own consistent colour without any configuration.
 */
public final class EnchantmentColors {
	private static final Map<String, String> DEFAULTS;

	static {
		Map<String, String> m = new LinkedHashMap<>();
		// Armour
		m.put("minecraft:protection", "#3F76E4");
		m.put("minecraft:fire_protection", "#FF8A1F");
		m.put("minecraft:blast_protection", "#C25A0A");
		m.put("minecraft:projectile_protection", "#1FA79B");
		m.put("minecraft:feather_falling", "#7EC8FF");
		m.put("minecraft:respiration", "#3FD6E8");
		m.put("minecraft:aqua_affinity", "#2EE6C4");
		m.put("minecraft:thorns", "#1E6B2F");
		m.put("minecraft:depth_strider", "#1B3FA8");
		m.put("minecraft:frost_walker", "#A9E6FF");
		m.put("minecraft:soul_speed", "#4AD9E0");
		m.put("minecraft:swift_sneak", "#8E5BE8");
		// Melee
		m.put("minecraft:sharpness", "#FF3B3B");
		m.put("minecraft:smite", "#FFD23F");
		m.put("minecraft:bane_of_arthropods", "#4CC24C");
		m.put("minecraft:fire_aspect", "#FF7A18");
		m.put("minecraft:knockback", "#7C93B5");
		m.put("minecraft:looting", "#17C964");
		m.put("minecraft:sweeping_edge", "#C8CDD6");
		m.put("minecraft:density", "#8A8D93");
		m.put("minecraft:breach", "#8B1A1A");
		m.put("minecraft:wind_burst", "#C9F3F7");
		// Tools
		m.put("minecraft:efficiency", "#22D3EE");
		m.put("minecraft:silk_touch", "#FFD6EC");
		m.put("minecraft:fortune", "#9AD13F");
		m.put("minecraft:unbreaking", "#4A78A8");
		m.put("minecraft:mending", "#8DE800");
		// Bows and crossbows
		m.put("minecraft:power", "#FF5A2B");
		m.put("minecraft:punch", "#C97A3A");
		m.put("minecraft:flame", "#FF9A1F");
		m.put("minecraft:infinity", "#C8A2FF");
		m.put("minecraft:multishot", "#E23FD0");
		m.put("minecraft:quick_charge", "#FFE94A");
		m.put("minecraft:piercing", "#F0948F");
		// Tridents
		m.put("minecraft:loyalty", "#4B3FBF");
		m.put("minecraft:impaling", "#127A82");
		m.put("minecraft:riptide", "#1E7FD6");
		m.put("minecraft:channeling", "#F4F23A");
		// Fishing rods
		m.put("minecraft:luck_of_the_sea", "#2E9E6B");
		m.put("minecraft:lure", "#7FE3C0");
		// Curses
		m.put("minecraft:binding_curse", "#8B0F2A");
		m.put("minecraft:vanishing_curse", "#8B0F2A");
		DEFAULTS = Collections.unmodifiableMap(m);
	}

	private EnchantmentColors() {
	}

	/**
	 * @return the built-in colours by enchantment id ({@code namespace:path}) as hex strings, unmodifiable
	 */
	public static Map<String, String> defaults() {
		return DEFAULTS;
	}

	/**
	 * @param enchantmentId {@code namespace:path}; a missing namespace means {@code minecraft}
	 * @param overrides user colours by id, may be null; invalid entries are ignored
	 * @return opaque ARGB
	 */
	public static int colorFor(String enchantmentId, Map<String, String> overrides) {
		String id = normalise(enchantmentId);
		if (overrides != null) {
			String custom = overrides.get(id);
			if (custom == null) {
				custom = overrides.get(enchantmentId);
			}
			if (custom != null) {
				try {
					return Colors.parseHex(custom);
				} catch (IllegalArgumentException ignored) {
					// fall through to the default
				}
			}
		}
		String known = DEFAULTS.get(id);
		if (known != null) {
			return Colors.parseHex(known);
		}
		return hashed(id);
	}

	/**
	 * The stable colour of an id without a configured colour: hue from an avalanche-mixed hash of the id.
	 */
	public static int hashed(String id) {
		int h = fmix32(id.hashCode());
		float hue = (float) ((h & 0xFFFFFFFFL) / 4294967296.0);
		return Colors.hsv(hue, 0.65f, 1.0f);
	}

	private static String normalise(String id) {
		if (id == null) {
			return "minecraft:";
		}
		String s = id.toLowerCase(Locale.ROOT);
		return s.indexOf(':') < 0 ? "minecraft:" + s : s;
	}

	/** MurmurHash3 32-bit finaliser. */
	private static int fmix32(int h) {
		h ^= h >>> 16;
		h *= 0x85EBCA6B;
		h ^= h >>> 13;
		h *= 0xC2B2AE35;
		h ^= h >>> 16;
		return h;
	}
}
