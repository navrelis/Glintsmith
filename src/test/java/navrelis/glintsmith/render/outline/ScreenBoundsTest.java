package navrelis.glintsmith.render.outline;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

class ScreenBoundsTest {
	private static ScreenBounds identity() {
		ScreenBounds bounds = new ScreenBounds();
		bounds.snapshot(new Matrix4f(), new Matrix4f());
		return bounds;
	}

	@Test
	void emptyBoundsGiveNoRegion() {
		ScreenBounds bounds = identity();
		assertTrue(bounds.isEmpty());
		assertFalse(bounds.toPixels(100, 100, 4, new int[4]));
	}

	@Test
	void includedPointsMapToPixelsWithPadding() {
		ScreenBounds bounds = identity();
		bounds.include(-0.5f, -0.5f, 0.0f);
		bounds.include(0.5f, 0.0f, 0.0f);
		assertFalse(bounds.isEmpty());
		int[] out = new int[4];
		assertTrue(bounds.toPixels(200, 100, 3, out));
		// x: -0.5 -> 50, 0.5 -> 150; y: -0.5 -> 25, 0 -> 50 (GL origin bottom left)
		assertArrayEquals(new int[] {47, 22, 153, 53}, out);
	}

	@Test
	void regionIsClippedToTheTarget() {
		ScreenBounds bounds = identity();
		bounds.include(-0.99f, -0.99f, 0.0f);
		bounds.include(0.99f, 0.99f, 0.0f);
		int[] out = new int[4];
		assertTrue(bounds.toPixels(100, 50, 10, out));
		assertArrayEquals(new int[] {0, 0, 99, 49}, out);
	}

	@Test
	void boundsCompletelyOffScreenGiveNoRegion() {
		ScreenBounds bounds = identity();
		bounds.include(3.0f, 3.0f, 0.0f);
		bounds.include(4.0f, 5.0f, 0.0f);
		assertFalse(bounds.toPixels(100, 100, 2, new int[4]));
	}

	@Test
	void projectionIsApplied() {
		ScreenBounds bounds = new ScreenBounds();
		// Model-view moves everything right by 0.5, projection scales by 0.5: x' = 0.5 * (x + 0.5)
		bounds.snapshot(new Matrix4f().scaling(0.5f), new Matrix4f().translation(0.5f, 0.0f, 0.0f));
		bounds.include(0.5f, 0.0f, 0.0f);
		int[] out = new int[4];
		assertTrue(bounds.toPixels(100, 100, 0, out));
		// NDC x 0.5 -> pixel 75, y 0 -> 50
		assertArrayEquals(new int[] {75, 50, 75, 50}, out);
	}

	@Test
	void vertexBehindTheCameraMakesTheBoxFullScreen() {
		ScreenBounds bounds = new ScreenBounds();
		Matrix4f perspective = new Matrix4f().perspective((float) Math.toRadians(70.0), 1.0f, 0.05f, 100.0f);
		bounds.snapshot(perspective, new Matrix4f());
		bounds.include(0.0f, 0.0f, -2.0f); // in front (camera looks down -z)
		bounds.include(0.0f, 0.0f, 2.0f); // behind
		assertTrue(bounds.isFull());
		int[] out = new int[4];
		assertTrue(bounds.toPixels(64, 32, 5, out));
		assertArrayEquals(new int[] {0, 0, 63, 31}, out);
	}

	@Test
	void withoutSnapshotTheBoxIsFullScreen() {
		ScreenBounds bounds = new ScreenBounds();
		bounds.include(0.0f, 0.0f, 0.0f);
		assertTrue(bounds.isFull());
	}

	@Test
	void resetForgetsPointsAndSnapshot() {
		ScreenBounds bounds = identity();
		bounds.include(0.0f, 0.0f, 0.0f);
		bounds.reset();
		assertTrue(bounds.isEmpty());
		assertFalse(bounds.hasSnapshot());
		assertFalse(bounds.toPixels(10, 10, 1, new int[4]));
	}

	@Test
	void zeroSizedTargetGivesNoRegion() {
		ScreenBounds bounds = identity();
		bounds.include(0.0f, 0.0f, 0.0f);
		assertFalse(bounds.toPixels(0, 100, 1, new int[4]));
	}
}
