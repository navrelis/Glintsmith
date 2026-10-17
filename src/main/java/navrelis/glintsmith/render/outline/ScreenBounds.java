package navrelis.glintsmith.render.outline;

import org.joml.Matrix4f;

/**
 * Screen-space bounding box of the mask vertices of one phase, in normalised device coordinates. Vertices are
 * projected with a snapshot of {@code projection * modelView} taken when the first outlined item of the phase is
 * captured. A vertex at or behind the camera plane makes the box full-screen.
 * <p>
 * Pure Java (JOML only), render thread only, allocation-free after construction.
 */
final class ScreenBounds {
	private static final float PIXEL_LIMIT = 1.0e6f;

	private final Matrix4f scratch = new Matrix4f();
	private float m00, m01, m03, m10, m11, m13, m20, m21, m23, m30, m31, m33;
	private boolean hasMatrix;

	private float minX, minY, maxX, maxY;
	private boolean any;
	private boolean full;

	/**
	 * Takes the projection snapshot. Call once per phase before the first {@link #include}.
	 */
	void snapshot(Matrix4f projection, Matrix4f modelView) {
		scratch.set(projection).mul(modelView);
		m00 = scratch.m00();
		m01 = scratch.m01();
		m03 = scratch.m03();
		m10 = scratch.m10();
		m11 = scratch.m11();
		m13 = scratch.m13();
		m20 = scratch.m20();
		m21 = scratch.m21();
		m23 = scratch.m23();
		m30 = scratch.m30();
		m31 = scratch.m31();
		m33 = scratch.m33();
		hasMatrix = true;
	}

	boolean hasSnapshot() {
		return hasMatrix;
	}

	/**
	 * Extends the box by one vertex given in the space the vertex consumer receives (before the model-view matrix).
	 */
	void include(float x, float y, float z) {
		if (full) {
			return;
		}
		if (!hasMatrix) {
			full = true;
			return;
		}
		float cw = m03 * x + m13 * y + m23 * z + m33;
		if (!(cw > 1.0e-6f)) { // behind the camera, or NaN
			full = true;
			return;
		}
		float nx = (m00 * x + m10 * y + m20 * z + m30) / cw;
		float ny = (m01 * x + m11 * y + m21 * z + m31) / cw;
		if (!Float.isFinite(nx) || !Float.isFinite(ny)) {
			full = true;
			return;
		}
		if (!any) {
			minX = maxX = nx;
			minY = maxY = ny;
			any = true;
			return;
		}
		if (nx < minX) {
			minX = nx;
		} else if (nx > maxX) {
			maxX = nx;
		}
		if (ny < minY) {
			minY = ny;
		} else if (ny > maxY) {
			maxY = ny;
		}
	}

	/**
	 * @return whether no vertex was included since the last reset
	 */
	boolean isEmpty() {
		return !any && !full;
	}

	/**
	 * @return whether the box covers the whole screen (a vertex at or behind the camera plane, or no snapshot)
	 */
	boolean isFull() {
		return full;
	}

	/**
	 * Converts the box to an inclusive pixel rectangle, grown by {@code pad} on every side and clamped to the
	 * target. GL convention: origin at the bottom left.
	 *
	 * @param out receives x0, y0, x1, y1 (inclusive)
	 * @return false if the rectangle is empty (nothing included, or completely off screen)
	 */
	boolean toPixels(int width, int height, int pad, int[] out) {
		if (width <= 0 || height <= 0) {
			return false;
		}
		if (full) {
			out[0] = 0;
			out[1] = 0;
			out[2] = width - 1;
			out[3] = height - 1;
			return true;
		}
		if (!any) {
			return false;
		}
		int p = Math.max(0, pad);
		float px0 = clampPixel((minX * 0.5f + 0.5f) * width);
		float px1 = clampPixel((maxX * 0.5f + 0.5f) * width);
		float py0 = clampPixel((minY * 0.5f + 0.5f) * height);
		float py1 = clampPixel((maxY * 0.5f + 0.5f) * height);
		int x0 = Math.max(0, (int) Math.floor(px0) - p);
		int y0 = Math.max(0, (int) Math.floor(py0) - p);
		int x1 = Math.min(width - 1, (int) Math.ceil(px1) + p);
		int y1 = Math.min(height - 1, (int) Math.ceil(py1) + p);
		if (x1 < x0 || y1 < y0) {
			return false;
		}
		out[0] = x0;
		out[1] = y0;
		out[2] = x1;
		out[3] = y1;
		return true;
	}

	/**
	 * Forgets the vertices and the snapshot.
	 */
	void reset() {
		any = false;
		full = false;
		hasMatrix = false;
	}

	private static float clampPixel(float v) {
		if (v < -PIXEL_LIMIT) {
			return -PIXEL_LIMIT;
		}
		return Math.min(v, PIXEL_LIMIT);
	}
}
