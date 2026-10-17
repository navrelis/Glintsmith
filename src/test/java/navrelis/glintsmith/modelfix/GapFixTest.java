package navrelis.glintsmith.modelfix;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

	@Test
	void eastIsTheLeftEdgeSoItMovesRightAndGrowsYZ() {
		Vector3f from = new Vector3f(3, 2, 7.5F);
		Vector3f to = new Vector3f(3, 6, 8.5F);
		GapFix.adjustSideElement(Direction.EAST, from, to, 0.5F, 0.25F);
		assertVec(3.25F, 1.5F, 7.0F, from);
		assertVec(3.25F, 6.5F, 9.0F, to);
		assertOrdered(from, to);
	}

	@Test
	void westIsTheRightEdgeSoItMovesLeftAndGrowsYZ() {
		Vector3f from = new Vector3f(4, 2, 7.5F);
		Vector3f to = new Vector3f(4, 6, 8.5F);
		GapFix.adjustSideElement(Direction.WEST, from, to, 0.5F, 0.25F);
		assertVec(3.75F, 1.5F, 7.0F, from);
		assertVec(3.75F, 6.5F, 9.0F, to);
		assertOrdered(from, to);
	}

	@Test
	void eastAndWestGrowOnYInsteadOfShrinking() {
		for (Direction face : new Direction[]{Direction.EAST, Direction.WEST}) {
			Vector3f from = new Vector3f(3, 2, 7.5F);
			Vector3f to = new Vector3f(3, 6, 8.5F);
			GapFix.adjustSideElement(face, from, to, 0.1F, 0.0F);
			assertTrue(to.y - from.y > 4.0F, face + " must grow on y");
			assertEquals(4.2F, to.y - from.y, 1.0E-5F);
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
	void fromNeverExceedsToEvenWithNegativeExpansion() {
		for (Direction face : Direction.values()) {
			Vector3f from = new Vector3f(1, 1, 7.5F);
			Vector3f to = new Vector3f(1.2F, 1.2F, 8.5F);
			GapFix.adjustSideElement(face, from, to, -5.0F, 0.1F);
			assertOrdered(from, to);
		}
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
}
