package navrelis.glintsmith.modelfix;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.Util;
import net.minecraft.core.Direction;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class GapFixTest {
	private static final float EPS = 1.0E-6F;

	private static void assertVec(float x, float y, float z, Vector3f v) {
		assertEquals(x, v.x, EPS, "x");
		assertEquals(y, v.y, EPS, "y");
		assertEquals(z, v.z, EPS, "z");
	}

	private static void assertOrdered(Vector3f from, Vector3f to) {
		assertTrue(from.x <= to.x && from.y <= to.y && from.z <= to.z, "from <= to: " + from + " / " + to);
	}

	@Test
	void upMovesDownAndGrowsXZ() {
		Vector3f from = new Vector3f(2, 10, 7.5F);
		Vector3f to = new Vector3f(5, 10, 8.5F);
		GapFix.adjustSideElement(Direction.UP, from, to, 0.5F, 0.25F);
		assertVec(1.5F, 9.75F, 7.0F, from);
		assertVec(5.5F, 9.75F, 9.0F, to);
		assertOrdered(from, to);
	}

	@Test
	void downMovesUpAndGrowsXZ() {
		Vector3f from = new Vector3f(2, 4, 7.5F);
		Vector3f to = new Vector3f(5, 4, 8.5F);
		GapFix.adjustSideElement(Direction.DOWN, from, to, 0.5F, 0.25F);
		assertVec(1.5F, 4.25F, 7.0F, from);
		assertVec(5.5F, 4.25F, 9.0F, to);
		assertOrdered(from, to);
	}

	// Vanilla builds EAST (left edge) and WEST (right edge) elements with from.y > to.y; the tests below use that shape.

	@Test
	void eastIsTheLeftEdgeSoItMovesRightAndGrowsYZ() {
		Vector3f from = new Vector3f(3, 6, 7.5F);
		Vector3f to = new Vector3f(3, 2, 8.5F);
		GapFix.adjustSideElement(Direction.EAST, from, to, 0.5F, 0.25F);
		assertVec(3.25F, 6.5F, 7.0F, from);
		assertVec(3.25F, 1.5F, 9.0F, to);
	}

	@Test
	void westIsTheRightEdgeSoItMovesLeftAndGrowsYZ() {
		Vector3f from = new Vector3f(4, 6, 7.5F);
		Vector3f to = new Vector3f(4, 2, 8.5F);
		GapFix.adjustSideElement(Direction.WEST, from, to, 0.5F, 0.25F);
		assertVec(3.75F, 6.5F, 7.0F, from);
		assertVec(3.75F, 1.5F, 9.0F, to);
	}

	@Test
	void eastAndWestGrowOnYInsteadOfShrinking() {
		for (Direction face : new Direction[]{Direction.EAST, Direction.WEST}) {
			Vector3f from = new Vector3f(3, 6, 7.5F);
			Vector3f to = new Vector3f(3, 2, 8.5F);
			GapFix.adjustSideElement(face, from, to, 0.1F, 0.0F);
			assertEquals(4.2F, from.y - to.y, 1.0E-5F, face + " must grow on y");
		}
	}

	@Test
	void cornerOrderIsKeptOnEveryAxis() {
		// Swapping from and to would flip the baked quad's winding and its V direction (FaceBakery does not sort).
		for (Direction face : new Direction[]{Direction.EAST, Direction.WEST}) {
			for (float indent : new float[]{0.0F, 0.0001F, 0.05F, -0.05F}) {
				Vector3f from = new Vector3f(3, 6, 7.5F);
				Vector3f to = new Vector3f(3, 2, 8.5F);
				GapFix.adjustSideElement(face, from, to, 0.02F, indent);
				assertTrue(from.y > to.y, face + " must keep from.y > to.y");
				assertTrue(from.z < to.z, face + " must keep from.z < to.z");
				assertEquals(from.x, to.x, face + " stays a plane");
			}
		}
	}

	@Test
	void allFourDirectionsGrowSymmetrically() {
		float e = 0.2F;
		for (Direction face : new Direction[]{Direction.UP, Direction.DOWN, Direction.EAST, Direction.WEST}) {
			Vector3f from = new Vector3f(1, 1, 7.5F);
			Vector3f to = new Vector3f(1, 1, 8.5F);
			boolean horizontal = face.getAxis() == Direction.Axis.Y;
			if (horizontal) {
				to.x = 4;
			} else {
				to.y = 4;
			}
			Vector3f before = new Vector3f(to).sub(from);
			GapFix.adjustSideElement(face, from, to, e, 0.0F);
			Vector3f after = new Vector3f(to).sub(from);
			assertEquals(before.z + 2 * e, after.z, 1.0E-5F, face + " z");
			assertEquals(before.x + (horizontal ? 2 * e : 0), after.x, 1.0E-5F, face + " x");
			assertEquals(before.y + (horizontal ? 0 : 2 * e), after.y, 1.0E-5F, face + " y");
			// Centre must not move when only expanding.
			assertEquals((1 + (horizontal ? 4 : 1)) / 2.0F, (from.x + to.x) / 2.0F, 1.0E-5F, face + " centre x");
			assertEquals((1 + (horizontal ? 1 : 4)) / 2.0F, (from.y + to.y) / 2.0F, 1.0E-5F, face + " centre y");
			assertEquals(8.0F, (from.z + to.z) / 2.0F, 1.0E-5F, face + " centre z");
		}
	}

	@Test
	void frontAndBackFacesAreLeftAlone() {
		for (Direction face : new Direction[]{Direction.NORTH, Direction.SOUTH}) {
			Vector3f from = new Vector3f(0, 0, 7.5F);
			Vector3f to = new Vector3f(16, 16, 8.5F);
			GapFix.adjustSideElement(face, from, to, 0.5F, 0.5F);
			assertVec(0, 0, 7.5F, from);
			assertVec(16, 16, 8.5F, to);
		}
	}

	@Test
	void zeroValuesChangeNothing() {
		for (Direction face : Direction.values()) {
			Vector3f from = new Vector3f(1, 2, 7.5F);
			Vector3f to = new Vector3f(3, 2, 8.5F);
			GapFix.adjustSideElement(face, from, to, 0.0F, 0.0F);
			assertVec(1, 2, 7.5F, from);
			assertVec(3, 2, 8.5F, to);
		}
	}

	@Test
	void negativeExpansionNeverFlipsAnElement() {
		for (Direction face : Direction.values()) {
			Vector3f from = new Vector3f(1, 1, 7.5F);
			Vector3f to = new Vector3f(1.2F, 1.2F, 8.5F);
			GapFix.adjustSideElement(face, from, to, -5.0F, 0.1F);
			assertOrdered(from, to);

			Vector3f reversedFrom = new Vector3f(1, 1.2F, 7.5F);
			Vector3f reversedTo = new Vector3f(1, 1, 8.5F);
			GapFix.adjustSideElement(face, reversedFrom, reversedTo, -5.0F, 0.1F);
			assertTrue(reversedFrom.y >= reversedTo.y, face + " must not flip y");
		}
	}

	@Test
	void layerGuardCutsGeometryAndUvsByTheSameAmount() {
		Vector3f from = new Vector3f(0, 0, 7.5F);
		Vector3f to = new Vector3f(16, 16, 8.5F);
		float[] south = {0, 0, 16, 16};
		float[] north = {16, 0, 0, 16};
		GapFix.guardLayerElement(from, to, 0.25F, 0.125F, south, north);
		assertVec(0.25F, 0.125F, 7.5F, from);
		assertVec(15.75F, 15.875F, 8.5F, to);
		assertArrayEquals(new float[]{0.25F, 0.125F, 15.75F, 15.875F}, south, EPS);
		// The back face keeps its mirrored u, so its texels stay where they were.
		assertArrayEquals(new float[]{15.75F, 0.125F, 0.25F, 15.875F}, north, EPS);
	}

	@Test
	void edgeGuardIsSmallButAboveSamplingPrecision() {
		// About 1/256 texel is the usual sub-texel precision of texture sampling; 1/16 would show as a gap at the border.
		assertTrue(GapFix.EDGE_GUARD_TEXELS > 1.0F / 256.0F);
		assertTrue(GapFix.EDGE_GUARD_TEXELS < 1.0F / 16.0F);
		assertTrue(GapFix.SIDE_THICKNESS_INSET_TEXELS > 0.0F && GapFix.SIDE_THICKNESS_INSET_TEXELS < 0.5F);
	}

	@Test
	void sideThicknessInsetOnlyTouchesTheThicknessAxis() {
		// UP/DOWN: u along the edge, v across the thickness.
		float[] up = {2, 5, 7, 6};
		GapFix.insetSideThickness(Direction.UP, up, 0.25F);
		assertArrayEquals(new float[]{2, 5.25F, 7, 5.75F}, up, EPS);
		float[] down = {2, 5, 7, 6};
		GapFix.insetSideThickness(Direction.DOWN, down, 0.25F);
		assertArrayEquals(new float[]{2, 5.25F, 7, 5.75F}, down, EPS);
		// EAST/WEST: u across the thickness, v along the edge (vanilla stores it bottom first: t + 1, then s).
		float[] east = {3, 9, 4, 4};
		GapFix.insetSideThickness(Direction.EAST, east, 0.25F);
		assertArrayEquals(new float[]{3.25F, 9, 3.75F, 4}, east, EPS);
		float[] west = {4, 9, 3, 4};
		GapFix.insetSideThickness(Direction.WEST, west, 0.25F);
		assertArrayEquals(new float[]{3.75F, 9, 3.25F, 4}, west, EPS);
		float[] front = {0, 0, 16, 16};
		GapFix.insetSideThickness(Direction.SOUTH, front, 0.25F);
		assertArrayEquals(new float[]{0, 0, 16, 16}, front, EPS);
	}

	@Test
	void insetStopsAtTheMiddle() {
		float[] uvs = {3, 0, 4, 1};
		GapFix.insetPair(uvs, 0, 2, 5.0F);
		assertEquals(3.5F, uvs[0], EPS);
		assertEquals(3.5F, uvs[2], EPS);
	}

	@Test
	void spriteBorderDetection() {
		assertTrue(GapFix.isOnSpriteBorder(Direction.EAST, new Vector3f(0, 6, 7.5F)));
		assertTrue(GapFix.isOnSpriteBorder(Direction.WEST, new Vector3f(16, 6, 7.5F)));
		assertTrue(GapFix.isOnSpriteBorder(Direction.UP, new Vector3f(2, 16, 7.5F)));
		assertTrue(GapFix.isOnSpriteBorder(Direction.DOWN, new Vector3f(2, 0, 7.5F)));
		// Vanilla computes the plane as pixel * 16 / size, which is not always exactly 16 in floats.
		assertTrue(GapFix.isOnSpriteBorder(Direction.WEST, new Vector3f(48.0F * (16.0F / 48.0F), 6, 7.5F)));
		assertFalse(GapFix.isOnSpriteBorder(Direction.EAST, new Vector3f(1, 0, 7.5F)));
		assertFalse(GapFix.isOnSpriteBorder(Direction.UP, new Vector3f(0, 15, 7.5F)));
		assertFalse(GapFix.isOnSpriteBorder(Direction.SOUTH, new Vector3f(0, 0, 8.5F)));
	}

	@Test
	void shrinkRatioKeepsVanillaWhenDisabledOrNotBlocksAtlas() {
		GapFix.Settings on = new GapFix.Settings(true, 0.0F, 0.0F, 0.0F, false);
		assertEquals(0.01F, GapFix.shrinkRatio(0.01F, true, GapFix.vanilla()), EPS);
		assertEquals(0.01F, GapFix.shrinkRatio(0.01F, false, on), EPS);
	}

	@Test
	void shrinkRatioScalesOnBlocksAtlas() {
		assertEquals(0.0F, GapFix.shrinkRatio(0.01F, true, new GapFix.Settings(true, 0.0F, 0.0F, 0.0F, false)), EPS);
		assertEquals(0.005F, GapFix.shrinkRatio(0.01F, true, new GapFix.Settings(true, 0.5F, 0.0F, 0.0F, false)), EPS);
		assertEquals(0.01F, GapFix.shrinkRatio(0.01F, true, new GapFix.Settings(true, 1.0F, 0.0F, 0.0F, false)), EPS);
	}

	@Test
	void requiresReloadDetectsAnyDifference() {
		GapFix.Settings base = new GapFix.Settings(true, 0.0F, 0.0F, 0.0001F, false);
		assertFalse(GapFix.requiresReload(base, base));
		assertFalse(GapFix.requiresReload(base, new GapFix.Settings(true, 0.0F, 0.0F, 0.0001F, false)));
		assertTrue(GapFix.requiresReload(base, new GapFix.Settings(false, 0.0F, 0.0F, 0.0001F, false)));
		assertTrue(GapFix.requiresReload(base, new GapFix.Settings(true, 1.0F, 0.0F, 0.0001F, false)));
		assertTrue(GapFix.requiresReload(base, new GapFix.Settings(true, 0.0F, 0.1F, 0.0001F, false)));
		assertTrue(GapFix.requiresReload(base, new GapFix.Settings(true, 0.0F, 0.0F, 0.0002F, false)));
		assertTrue(GapFix.requiresReload(base, new GapFix.Settings(true, 0.0F, 0.0F, 0.0001F, true)));
	}

	@Test
	void presetsAreSane() {
		GapFix.Settings vanilla = GapFix.vanilla();
		assertFalse(vanilla.enabled());
		assertEquals(1.0F, vanilla.uvShrinkMultiplier());
		GapFix.Settings auto = GapFix.autoSettings();
		assertTrue(auto.enabled());
		assertFalse(auto.preciseSpans());
	}

	@Test
	void autoSettingsMatchThePlatformValues() {
		GapFix.Settings auto = GapFix.autoSettings();
		if (Util.getPlatform() == Util.OS.OSX) {
			assertEquals(new GapFix.Settings(true, 1.0F, 0.0F, 0.0099F, false), auto);
		} else {
			assertEquals(new GapFix.Settings(true, 0.0F, 0.005F, 0.0001F, false), auto);
			assertEquals(0.005F, auto.sideExpansion());
			assertEquals(0.0001F, auto.sideIndent());
			assertEquals(0.0F, auto.uvShrinkMultiplier());
		}
	}
}
