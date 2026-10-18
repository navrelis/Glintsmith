package navrelis.glintsmith.render.outline;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import navrelis.glintsmith.config.GlintsmithConfig;
import org.junit.jupiter.api.Test;

/**
 * Pins the one thickness factor every outline phase uses: with {@code scaleWithResolution} a thickness is in pixels at
 * a framebuffer height of 1080, without it in framebuffer pixels, and neither the phase nor the GUI scale changes it.
 * One thickness must have one on-screen width in the world, the first-person hand, the GUI and the settings preview.
 */
class OutlineThicknessScaleTest {
	private static final int[] HEIGHTS = { 480, 720, 1080, 1440, 1600, 2160, 4320 };

	private static OutlineRuntimeSettings settings(boolean scaleWithResolution) {
		GlintsmithConfig config = new GlintsmithConfig().sanitize();
		config.outline.scaleWithResolution = scaleWithResolution;
		return OutlineRuntimeSettings.from(config);
	}

	@Test
	void scaledThicknessIsInPixelsAt1080() {
		OutlineRuntimeSettings s = settings(true);
		for (int height : HEIGHTS) {
			assertEquals(height / 1080.0f, s.thicknessScale(height), 1.0e-6f, "height " + height);
		}
		assertEquals(1.0f, s.thicknessScale(1080));
		assertEquals(4.0f / 3.0f, s.thicknessScale(1440), 1.0e-6f);
		assertEquals(2.0f, s.thicknessScale(2160));
	}

	@Test
	void unscaledThicknessIsInFramebufferPixels() {
		OutlineRuntimeSettings s = settings(false);
		for (int height : HEIGHTS) {
			assertEquals(1.0f, s.thicknessScale(height), "height " + height);
		}
		assertEquals(1.0f, OutlineRuntimeSettings.DISABLED.thicknessScale(2160));
	}

	@Test
	void invalidHeightsGiveOne() {
		for (boolean scaled : new boolean[] { true, false }) {
			OutlineRuntimeSettings s = settings(scaled);
			assertEquals(1.0f, s.thicknessScale(0), "minimised window");
			assertEquals(1.0f, s.thicknessScale(-1));
			assertEquals(1.0f, s.thicknessScale(Integer.MIN_VALUE));
		}
	}

	/**
	 * The factor has no phase and no GUI scale input, so WORLD, HAND, GUI and PREVIEW cannot get different widths
	 * again: GUI used to be scaled by {@code guiScale / 2}, everything else by {@code height / 1080}.
	 */
	@Test
	void factorDependsOnlyOnTheFramebufferHeight() {
		List<Method> methods = new ArrayList<>();
		for (Method method : OutlineRuntimeSettings.class.getDeclaredMethods()) {
			if (method.getName().equals("thicknessScale")) {
				methods.add(method);
			}
		}
		assertEquals(1, methods.size(), "one thicknessScale for every phase");
		assertArrayEquals(new Class<?>[] { int.class }, methods.get(0).getParameterTypes());
	}

	/**
	 * What the style table stores for the owner's 8.5 px look (in every phase, since the factor is shared) at 1080p,
	 * 1440p and 4K: the width grows with the height and quantises to 1/8 px.
	 */
	@Test
	void ownerThicknessIsEncodedProportionallyToTheHeight() {
		OutlineRuntimeSettings s = settings(true);
		int[][] expected = { { 1080, 68 }, { 1440, 91 }, { 2160, 136 } }; // thickness * 8: 8.5, 11.375, 17 px
		for (int[] row : expected) {
			StyleTable table = new StyleTable();
			int index = table.indexFor(0x40FF00FF, 8.5f * s.thicknessScale(row[0]), 0.0f);
			assertEquals(row[1], table.thicknessByte(index), "height " + row[0]);
		}
	}

	@Test
	void reachMatchesTheStyleTableRadius() {
		OutlineRuntimeSettings s = settings(true);
		assertEquals(10, s.outlineReachPixels(8.5f, 0.0f, 1080)); // ceil(8.5 + 1)
		assertEquals(13, s.outlineReachPixels(8.5f, 0.0f, 1440)); // ceil(11.375 + 1)
		assertEquals(18, s.outlineReachPixels(8.5f, 0.0f, 2160)); // ceil(17 + 1)
		assertEquals(StyleTable.MAX_RADIUS, s.outlineReachPixels(16.0f, 1.0f, 2160), "capped like the composite");
		assertEquals(10, settings(false).outlineReachPixels(8.5f, 0.0f, 2160), "unscaled");
		for (int height : HEIGHTS) {
			for (float softness = 0.0f; softness <= 1.0f; softness += 0.25f) {
				float scaled = 8.5f * s.thicknessScale(height);
				StyleTable table = new StyleTable();
				table.indexFor(0xFFFFFFFF, scaled, softness);
				assertEquals(table.maxRadius(), s.outlineReachPixels(8.5f, softness, height), height + "/" + softness);
				assertTrue(table.maxRadius() <= StyleTable.MAX_RADIUS);
			}
		}
	}
}
