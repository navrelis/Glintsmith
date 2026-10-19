package navrelis.glintsmith.style;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class ColorEvaluatorTest {
	private static final int RED = 0xFFFF0000;
	private static final int GREEN = 0xFF00FF00;
	private static final int BLUE = 0xFF0000FF;

	private static ColorStyle style(ColorMode mode, float speed, String... colors) {
		ColorStyle s = new ColorStyle();
		s.mode = mode;
		s.speed = speed;
		s.colors = new ArrayList<>(List.of(colors));
		return s;
	}

	private static int eval(ColorStyle s, double t, int... ench) {
		return ColorEvaluator.compile(s).evaluate(t, ench);
	}

	@Test
	void staticReturnsFirstColour() {
		ColorStyle s = style(ColorMode.STATIC, 1.0f, "#102030", "#FFFFFF");
		assertEquals(0xFF102030, eval(s, 0));
		assertEquals(0xFF102030, eval(s, 123.4));
	}

	@Test
	void rainbowCyclesEveryFourSeconds() {
		ColorStyle s = ColorStyle.rainbow(1.0f);
		assertEquals(RED, eval(s, 0));
		assertEquals(GREEN, eval(s, 4.0 / 3.0));
		assertEquals(BLUE, eval(s, 8.0 / 3.0));
		assertEquals(RED, eval(s, 4.0));
		assertEquals(eval(s, 1.3), eval(s, 5.3));
	}

	@Test
	void rainbowSpeedScalesTime() {
		ColorStyle fast = ColorStyle.rainbow(2.0f);
		ColorStyle slow = ColorStyle.rainbow(1.0f);
		assertEquals(eval(slow, 2.0), eval(fast, 1.0));
	}

	@Test
	void rainbowUsesSaturationAndBrightness() {
		ColorStyle s = ColorStyle.rainbow(1.0f);
		s.saturation = 0.0f;
		assertEquals(0xFFFFFFFF, eval(s, 0));
		s.saturation = 1.0f;
		s.brightness = 0.0f;
		assertEquals(0xFF000000, eval(s, 0));
	}

	@Test
	void pulseSingleColourBreathesBetween35And100Percent() {
		ColorStyle s = style(ColorMode.PULSE, 1.0f, "#FFFFFF");
		assertEquals(0xFFFFFFFF, eval(s, 0));
		int dim = eval(s, 1.0); // half period: darkest
		int r = dim >> 16 & 0xFF;
		assertTrue(Math.abs(r - Math.round(255 * 0.35f)) <= 1, "dim red channel " + r);
		assertEquals(0xFFFFFFFF, eval(s, 2.0));
		assertEquals(0xFF, eval(s, 0.7) >>> 24);
	}

	@Test
	void pulseTwoColoursBlendsFirstToSecond() {
		ColorStyle s = style(ColorMode.PULSE, 1.0f, "#FF0000", "#0000FF");
		assertEquals(RED, eval(s, 0));
		assertEquals(BLUE, eval(s, 1.0));
		assertEquals(RED, eval(s, 2.0));
		int mid = eval(s, 0.5);
		assertNotEquals(RED, mid);
		assertNotEquals(BLUE, mid);
	}

	@Test
	void pulseSpeedShortensThePeriod() {
		ColorStyle s = style(ColorMode.PULSE, 2.0f, "#FF0000", "#0000FF");
		assertEquals(BLUE, eval(s, 0.5));
		assertEquals(RED, eval(s, 1.0));
	}

	@Test
	void gradientVisitsEveryColourAndLoops() {
		ColorStyle s = style(ColorMode.GRADIENT, 1.0f, "#FF0000", "#00FF00", "#0000FF");
		assertEquals(RED, eval(s, 0));
		assertEquals(GREEN, eval(s, 1.5));
		assertEquals(BLUE, eval(s, 3.0));
		assertEquals(RED, eval(s, 4.5));
		assertEquals(GREEN, eval(s, 6.0));
		int between = eval(s, 0.75);
		assertNotEquals(RED, between);
		assertNotEquals(GREEN, between);
	}

	@Test
	void gradientSpeedAndNegativeTime() {
		ColorStyle s = style(ColorMode.GRADIENT, 2.0f, "#FF0000", "#00FF00", "#0000FF");
		assertEquals(GREEN, eval(s, 0.75));
		// One segment back from red wraps to blue.
		ColorStyle slow = style(ColorMode.GRADIENT, 1.0f, "#FF0000", "#00FF00", "#0000FF");
		assertEquals(BLUE, eval(slow, -1.5));
	}

	@Test
	void gradientSingleColourIsConstant() {
		ColorStyle s = style(ColorMode.GRADIENT, 1.0f, "#FF0000");
		assertEquals(RED, eval(s, 0));
		assertEquals(RED, eval(s, 9.9));
	}

	@Test
	void speedZeroFreezesAtTimeZero() {
		for (ColorMode mode : ColorMode.values()) {
			ColorStyle s = style(mode, 0.0f, "#FF0000", "#00FF00", "#0000FF");
			assertEquals(eval(s, 0, GREEN, BLUE), eval(s, 77.7, GREEN, BLUE), mode.name());
		}
		assertEquals(RED, eval(style(ColorMode.GRADIENT, 0.0f, "#FF0000", "#00FF00"), 5.0));
	}

	@Test
	void enchantmentModeUsesEnchantmentColours() {
		ColorStyle s = style(ColorMode.ENCHANTMENT, 1.0f, "#123456");
		// no enchantments: first colour
		assertEquals(0xFF123456, eval(s, 3.0));
		assertEquals(0xFF123456, ColorEvaluator.compile(s).evaluate(3.0, null));
		// exactly one: that colour
		assertEquals(GREEN, eval(s, 3.0, GREEN));
		// several: gradient over them
		assertEquals(RED, eval(s, 0, RED, GREEN));
		assertEquals(GREEN, eval(s, 1.5, RED, GREEN));
		assertEquals(RED, eval(s, 3.0, RED, GREEN));
	}

	@Test
	void staticEvaluateMatchesCompiled() {
		ColorStyle s = style(ColorMode.GRADIENT, 1.0f, "#FF0000", "#00FF00");
		int[] parsed = {RED, GREEN};
		assertEquals(eval(s, 0.4), ColorEvaluator.evaluate(s, 0.4, null, parsed));
		assertEquals(eval(s, 0.4), ColorEvaluator.evaluate(s, 0.4, null, null));
	}

	@Test
	void invalidColoursAreSkippedAtCompile() {
		ColorStyle s = style(ColorMode.STATIC, 1.0f, "nonsense", "#00FF00");
		assertEquals(GREEN, eval(s, 0));
		ColorStyle none = style(ColorMode.STATIC, 1.0f, "nonsense");
		assertEquals(0xFFD088FC, eval(none, 0));
	}

	@Test
	void compiledEqualityAndAnimationFlags() {
		ColorStyle a = ColorStyle.rainbow(1.0f);
		assertEquals(ColorEvaluator.compile(a), ColorEvaluator.compile(a.copy()));
		assertEquals(ColorEvaluator.compile(a).hashCode(), ColorEvaluator.compile(a.copy()).hashCode());
		assertNotEquals(ColorEvaluator.compile(a), ColorEvaluator.compile(ColorStyle.rainbow(2.0f)));
		assertTrue(ColorEvaluator.compile(a).isAnimated());
		assertFalse(ColorEvaluator.compile(ColorStyle.of("#FF0000")).isAnimated());
		assertFalse(ColorEvaluator.compile(ColorStyle.rainbow(0.0f)).isAnimated());
		assertTrue(ColorEvaluator.compile(style(ColorMode.ENCHANTMENT, 1.0f, "#FF0000")).usesEnchantmentColors());
	}

	@Test
	void sanitizeClampsAndRepairs() {
		ColorStyle s = new ColorStyle();
		s.speed = 99.0f;
		s.saturation = -1.0f;
		s.brightness = 5.0f;
		s.mode = null;
		s.colors = new ArrayList<>();
		s.colors.add("nonsense");
		s.colors.add(null);
		for (int i = 0; i < 30; i++) {
			s.colors.add("#00000" + Integer.toHexString(i % 16));
		}
		s.sanitize();
		assertEquals(10.0f, s.speed);
		assertEquals(0.0f, s.saturation);
		assertEquals(1.0f, s.brightness);
		assertEquals(ColorMode.STATIC, s.mode);
		assertEquals(ColorStyle.MAX_COLORS, s.colors.size());
		assertFalse(s.colors.contains("nonsense"));

		ColorStyle empty = new ColorStyle();
		empty.colors = null;
		empty.speed = Float.NaN;
		empty.sanitize();
		assertEquals(List.of(ColorStyle.DEFAULT_COLOR), empty.colors);
		assertEquals(1.0f, empty.speed);
	}

	@Test
	void factoriesAndCopy() {
		ColorStyle s = ColorStyle.of("#ABCDEF");
		assertEquals(ColorMode.STATIC, s.mode);
		assertEquals(List.of("#ABCDEF"), s.colors);
		ColorStyle copy = s.copy();
		copy.colors.add("#000000");
		assertEquals(1, s.colors.size());
		assertEquals(ColorMode.RAINBOW, ColorStyle.rainbow(3.0f).mode);
		assertEquals(3.0f, ColorStyle.rainbow(3.0f).speed);
		assertEquals(s, s.copy());
	}
}
