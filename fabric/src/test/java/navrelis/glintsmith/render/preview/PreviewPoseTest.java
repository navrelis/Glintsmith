package navrelis.glintsmith.render.preview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.block.model.ItemTransform;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class PreviewPoseTest {
	private static final float EPS = 1.0e-4f;
	private static final float BOX_EPS = 1.0e-3f;

	private static ItemTransform transform(float rx, float ry, float rz, float tx, float ty, float tz, float sx, float sy, float sz) {
		return new ItemTransform(new Vector3f(rx, ry, rz), new Vector3f(tx, ty, tz), new Vector3f(sx, sy, sz));
	}

	private static float between(Random random, float min, float max) {
		return min + random.nextFloat() * (max - min);
	}

	private static void assertIdentity(Matrix4f m, String what) {
		Matrix4f identity = new Matrix4f();
		for (int col = 0; col < 4; col++) {
			for (int row = 0; row < 4; row++) {
				assertEquals(identity.get(col, row), m.get(col, row), EPS, what + " [" + col + "," + row + "]");
			}
		}
	}

	private static void assertIdentity(Matrix3f m, String what) {
		Matrix3f identity = new Matrix3f();
		for (int col = 0; col < 3; col++) {
			for (int row = 0; row < 3; row++) {
				assertEquals(identity.get(col, row), m.get(col, row), EPS, what + " normal [" + col + "," + row + "]");
			}
		}
	}

	@Test
	void applyInverseCancelsTheVanillaTransform() {
		Random random = new Random(1234);
		for (int i = 0; i < 500; i++) {
			float sx = between(random, 0.1f, 3.0f);
			boolean uniform = i % 3 == 0;
			ItemTransform t = transform(between(random, -180, 180), between(random, -180, 180), between(random, -180, 180),
				between(random, -2, 2), between(random, -2, 2), between(random, -2, 2),
				sx, uniform ? sx : between(random, 0.1f, 3.0f), uniform ? sx : between(random, 0.1f, 3.0f));
			assertTrue(PreviewPose.invertible(t));
			PoseStack pose = new PoseStack();
			PreviewPose.applyInverse(t, pose);
			t.apply(false, pose);
			assertIdentity(pose.last().pose(), "transform " + i);
			assertIdentity(pose.last().normal(), "transform " + i);
		}
	}

	@Test
	void degenerateTransformsAreNotInvertible() {
		assertTrue(PreviewPose.invertible(ItemTransform.NO_TRANSFORM));
		assertFalse(PreviewPose.invertible(null));
		assertFalse(PreviewPose.invertible(transform(0, 0, 0, 0, 0, 0, 0, 1, 1)));
		assertFalse(PreviewPose.invertible(transform(0, 0, 0, 0, 0, 0, 1, 5.0e-5f, 1)));
		assertFalse(PreviewPose.invertible(transform(0, 0, 0, 0, 0, 0, 1, 1, -5.0e-5f)));
		assertTrue(PreviewPose.invertible(transform(0, 0, 0, 0, 0, 0, -1, 1, 1)), "mirrored is fine");
		assertFalse(PreviewPose.invertible(transform(Float.NaN, 0, 0, 0, 0, 0, 1, 1, 1)));
		assertFalse(PreviewPose.invertible(transform(0, 0, 0, 0, Float.NaN, 0, 1, 1, 1)));
		assertFalse(PreviewPose.invertible(transform(0, 0, 0, 0, 0, Float.POSITIVE_INFINITY, 1, 1, 1)));
		assertFalse(PreviewPose.invertible(transform(0, 0, 0, 0, 0, 0, 1, Float.NaN, 1)));
	}

	@Test
	void theFittedItemStaysInsideTheBoxAtEveryAngle() {
		float[][] boxes = {
			{-0.5f, -0.5f, -0.5f, 0.5f, 0.5f, 0.5f}, // unit cube
			{-0.5f, -0.5f, -0.03125f, 0.5f, 0.5f, 0.03125f}, // flat sprite
			{-0.1f, -1.0f, -0.1f, 0.1f, 1.0f, 0.1f}, // tall
			{0.2f, -0.3f, 0.1f, 1.4f, 0.6f, 0.3f}, // off centre
		};
		float[][] areas = {{100, 100}, {180, 120}, {72, 200}, {400, 400}};
		float margin = 6.0f;
		float maxDepth = 110.0f;
		Vector3f corner = new Vector3f();
		for (float[] b : boxes) {
			for (float[] area : areas) {
				for (float pitch : new float[] {-60, 0, 15, 60}) {
					PreviewPose.Fit fit = PreviewPose.fit(b[0], b[1], b[2], b[3], b[4], b[5], area[0], area[1], margin, pitch, maxDepth);
					assertTrue(fit.scale() >= 1.0f);
					for (int yaw = 0; yaw < 360; yaw += 5) {
						PoseStack pose = new PoseStack();
						PreviewPose.turntable(pose, fit, yaw, pitch);
						Matrix4f m = pose.last().pose();
						for (int c = 0; c < 8; c++) {
							corner.set((c & 1) == 0 ? b[0] : b[3], (c & 2) == 0 ? b[1] : b[4], (c & 4) == 0 ? b[2] : b[5]);
							m.transformPosition(corner);
							String what = "box " + java.util.Arrays.toString(b) + " area " + area[0] + "x" + area[1]
								+ " pitch " + pitch + " yaw " + yaw + " corner " + c;
							assertTrue(Math.abs(corner.x) <= area[0] / 2 - margin + BOX_EPS, what + " x " + corner.x);
							assertTrue(Math.abs(corner.y) <= area[1] / 2 - margin + BOX_EPS, what + " y " + corner.y);
							assertTrue(Math.abs(corner.z) <= maxDepth + BOX_EPS, what + " z " + corner.z);
						}
					}
				}
			}
		}
	}

	@Test
	void fitCentresTheBoundsAndNeverShrinksBelowOne() {
		PreviewPose.Fit fit = PreviewPose.fit(0, 0, 0, 1, 2, 3, 100, 100, 6, 15, 110);
		assertEquals(0.5f, fit.cx(), EPS);
		assertEquals(1.0f, fit.cy(), EPS);
		assertEquals(1.5f, fit.cz(), EPS);
		assertEquals(1.0f, PreviewPose.fit(-0.5f, -0.5f, -0.5f, 0.5f, 0.5f, 0.5f, 8, 8, 10, 0, 110).scale(),
			"a margin larger than the box");
		assertEquals(1.0f, PreviewPose.fit(0, 0, 0, 0, 0, 0, 8, 8, Float.NaN, 0, 110).scale());
	}
}
