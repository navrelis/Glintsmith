package navrelis.glintsmith.style;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

class DurabilityAndEnchantmentColorsTest {
	private static final int BASE = 0xFF336699;

	@Test
	void alertIgnoredAboveThresholdOrWhenDisabled() {
		DurabilityAlert alert = new DurabilityAlert();
		assertEquals(BASE, alert.apply(BASE, 0.5f, 0.3));
		assertEquals(BASE, alert.apply(BASE, 0.1001f, 0.3));
		assertEquals(BASE, alert.apply(BASE, 1.0f, 0.3));
		alert.enabled = false;
		assertEquals(BASE, alert.apply(BASE, 0.0f, 0.3));
	}

	@Test
	void alertPulsesAtOrBelowThreshold() {
		DurabilityAlert alert = new DurabilityAlert();
		int atThreshold = alert.apply(BASE, 0.10f, 0.3);
		assertNotEquals(BASE, atThreshold);
		// t = 0: factor 0.55, t = 0.3 s (quarter period): factor 1.0 = pure alert colour
		assertEquals(0xFFFF2A2A, alert.apply(BASE, 0.05f, 0.3));
		int low = alert.apply(BASE, 0.05f, 0.9); // factor 0.1
		assertTrue((low >> 16 & 0xFF) < 0xC0);
		assertNotEquals(alert.apply(BASE, 0.05f, 0.0), alert.apply(BASE, 0.05f, 0.3));
	}

	@Test
	void alertKeepsBaseAlpha() {
		DurabilityAlert alert = new DurabilityAlert();
		int translucent = 0x80336699;
		assertEquals(0x80, alert.apply(translucent, 0.0f, 0.0) >>> 24);
		assertEquals(0x80, alert.apply(translucent, 0.0f, 0.3) >>> 24);
	}

	@Test
	void alertSanitizeRepairs() {
		DurabilityAlert alert = new DurabilityAlert();
		alert.threshold = 5.0f;
		alert.color = "red";
		alert.sanitize();
		assertEquals(1.0f, alert.threshold);
		assertEquals(DurabilityAlert.DEFAULT_COLOR, alert.color);
	}

	@Test
	void alertColourChangeIsPickedUp() {
		DurabilityAlert alert = new DurabilityAlert();
		alert.color = "#00FF00";
		assertEquals(0xFF00FF00, alert.apply(BASE, 0.0f, 0.3));
		alert.color = "#0000FF";
		assertEquals(0xFF0000FF, alert.apply(BASE, 0.0f, 0.3));
	}

	@Test
	void overrideBeatsDefaultBeatsHash() {
		assertEquals(Colors.parseHex("#FF3B3B"), EnchantmentColors.colorFor("minecraft:sharpness", null));
		assertEquals(0xFF123456, EnchantmentColors.colorFor("minecraft:sharpness", Map.of("minecraft:sharpness", "#123456")));
		// invalid override falls back to the default
		assertEquals(Colors.parseHex("#FF3B3B"), EnchantmentColors.colorFor("minecraft:sharpness", Map.of("minecraft:sharpness", "nope")));
		// no namespace means minecraft
		assertEquals(Colors.parseHex("#FF3B3B"), EnchantmentColors.colorFor("sharpness", Map.of()));
		// unknown id: hashed, and an override still wins
		int hashed = EnchantmentColors.colorFor("mymod:zap", Map.of());
		assertEquals(EnchantmentColors.hashed("mymod:zap"), hashed);
		assertEquals(0xFF010203, EnchantmentColors.colorFor("mymod:zap", Map.of("mymod:zap", "#010203")));
	}

	@Test
	void hashedColourIsStableAndDistinct() {
		int a = EnchantmentColors.colorFor("mymod:zap", null);
		assertEquals(a, EnchantmentColors.colorFor("mymod:zap", null));
		assertEquals(0xFF, a >>> 24);
		assertNotEquals(a, EnchantmentColors.colorFor("mymod:boom", null));
		// vivid: value 1.0 means the largest channel is 255
		int max = Math.max(a >> 16 & 0xFF, Math.max(a >> 8 & 0xFF, a & 0xFF));
		assertEquals(255, max);
	}

	@Test
	void everyVanillaEnchantmentHasADefault() {
		String[] vanilla = {
			"protection", "fire_protection", "feather_falling", "blast_protection", "projectile_protection",
			"respiration", "aqua_affinity", "thorns", "depth_strider", "frost_walker", "binding_curse", "soul_speed",
			"swift_sneak", "sharpness", "smite", "bane_of_arthropods", "knockback", "fire_aspect", "looting",
			"sweeping_edge", "efficiency", "silk_touch", "unbreaking", "fortune", "power", "punch", "flame",
			"infinity", "luck_of_the_sea", "lure", "loyalty", "impaling", "riptide", "channeling", "multishot",
			"quick_charge", "piercing", "density", "breach", "wind_burst", "mending", "vanishing_curse"
		};
		for (String id : vanilla) {
			assertTrue(EnchantmentColors.defaults().containsKey("minecraft:" + id), id);
		}
		assertEquals(vanilla.length, EnchantmentColors.defaults().size());
		for (String hex : EnchantmentColors.defaults().values()) {
			Colors.parseHex(hex);
		}
	}
}
