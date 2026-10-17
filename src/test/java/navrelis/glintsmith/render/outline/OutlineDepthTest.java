package navrelis.glintsmith.render.outline;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The depth rules of the outline passes ({@link OutlineDepth}) and a small pure-Java mirror of the two passes on a
 * pixel grid. The mirror follows {@code outline_edt.fsh} and {@code outline_composite.fsh} step by step (seed rule,
 * horizontal search, vertical search, inside check, occlusion rule), without the soft edge. No GL.
 */
class OutlineDepthTest {
	private static final float NEAR = OutlineCompositor.NEAR_PLANE;
	private static final float FAR = 256.0f;
	private static final int RADIUS = 2;

	// ---- rules

	@Test
	void packedDepthRoundTripsWithinHalfAStep() {
		for (float z : new float[] { 0.0f, 1.0f, 0.5f, 0.99f, 0.999937f, window(0.5f), window(10.0f), window(200.0f) }) {
			int packed = OutlineDepth.pack(z);
			int[] b = OutlineDepth.bytes(packed);
			float back = OutlineDepth.unpack(b[0], b[1], b[2]);
			assertTrue(Math.abs(back - z) <= 0.5f / OutlineDepth.DEPTH_STEPS + 1.0e-7f, "z " + z + " came back as " + back);
		}
		assertEquals(OutlineDepth.DEPTH_STEPS, OutlineDepth.pack(2.0f));
		assertEquals(0, OutlineDepth.pack(-1.0f));
		assertArrayEquals(new int[] { 0x12, 0x34, 0x56 }, OutlineDepth.bytes(0x123456));
	}

	@Test
	void seedIsVisibleUnlessTheSceneIsClearlyInFront() {
		float item = window(0.5f);
		assertTrue(OutlineDepth.visible(item, item), "the item itself is in the scene depth");
		assertTrue(OutlineDepth.visible(1.0f, item), "scene behind the item (Fabulous items, cleared hand depth)");
		assertTrue(OutlineDepth.visible(item - 4.0f / OutlineDepth.DEPTH_STEPS, item), "rounding noise");
		// An arm surface 0.3 mm in front of the blade: hidden. The old polygon offset let such pixels through.
		assertFalse(OutlineDepth.visible(window(0.5f - 0.0003f), item));
		assertTrue(OutlineDepth.SEED_EPSILON < 1.0e-5f);
	}

	@Test
	void occlusionRuleKeepsBackgroundAndHidesOccluders() {
		float tol = OutlineDepth.WORLD_TOLERANCE;
		assertFalse(OutlineDepth.hidden(false, 12.0f, 10.0f, tol), "wall behind the item keeps the outline");
		assertFalse(OutlineDepth.hidden(false, 9.9f, 10.0f, tol), "slightly nearer floor keeps the outline");
		assertTrue(OutlineDepth.hidden(false, 8.0f, 10.0f, tol), "pillar in front hides the outline");
		// On a hidden item pixel the occluder must be clearly behind the seed.
		assertTrue(OutlineDepth.hidden(true, 10.05f, 10.0f, tol), "item continues behind a surface at its own depth");
		assertTrue(OutlineDepth.hidden(true, 8.0f, 10.0f, tol));
		assertFalse(OutlineDepth.hidden(true, 11.0f, 10.0f, tol), "another item hidden far behind the wall");
		// Hand: tighter front tolerance.
		assertTrue(OutlineDepth.hidden(false, 0.45f, 0.5f, OutlineDepth.HAND_TOLERANCE));
		assertFalse(OutlineDepth.hidden(false, 0.45f, 0.5f, OutlineDepth.WORLD_TOLERANCE));
		assertEquals(OutlineDepth.HAND_TOLERANCE, OutlineCompositor.frontTolerance(OutlinePhase.HAND));
		assertEquals(OutlineDepth.WORLD_TOLERANCE, OutlineCompositor.frontTolerance(OutlinePhase.WORLD));
	}

	// ---- grid scenarios

	@Test
	void fullyVisibleItemKeepsItsWholeOutline() {
		Grid g = new Grid(20, 12);
		g.item(5, 3, 9, 7, 0.5f);
		boolean[] now = g.composite(true, OutlineDepth.HAND_TOLERANCE);
		boolean[] before = g.legacy(0.0f, OutlineDepth.WORLD_TOLERANCE);
		assertArrayEquals(before, now, "no new gaps, no thinner outline");
		assertTrue(now[g.at(4, 5)] && now[g.at(10, 5)] && now[g.at(7, 2)] && now[g.at(7, 8)] && now[g.at(3, 5)]);
		assertFalse(now[g.at(7, 5)], "nothing inside the silhouette");
		assertArrayEquals(g.composite(false, 0.0f), now, "same outline as without depth");
	}

	@Test
	void bladeIntoTheSleeveGetsNoOutlineOnTheSleeve() {
		// Blade from x 2 to 17 at 0.5 blocks; the sleeve covers x >= 10 and is 0.3 mm in front of the blade there,
		// so the blade continues inside the arm. Behind both: cleared hand depth.
		Grid g = new Grid(24, 12);
		g.item(2, 4, 17, 7, 0.5f);
		g.scene(10, 0, 23, 11, 0.4997f);
		boolean[] now = g.composite(true, OutlineDepth.HAND_TOLERANCE);
		for (int y = 4; y <= 7; y++) {
			for (int x = 10; x <= 23; x++) {
				assertFalse(now[g.at(x, y)], "outline on the sleeve over the hidden blade at " + x + "," + y);
			}
		}
		assertTrue(now[g.at(1, 5)] && now[g.at(5, 3)] && now[g.at(5, 8)], "the visible blade keeps its outline");

		// The old pipeline: the mask pixels inside the polygon-offset slack became seeds whose "item depth" was the
		// sleeve depth, so the outline was drawn onto the sleeve.
		boolean[] before = g.legacy(window(0.5f) - window(0.4997f) + 1.0e-7f, OutlineDepth.WORLD_TOLERANCE);
		boolean onSleeve = false;
		for (int y = 4; y <= 7; y++) {
			for (int x = 10; x <= 23; x++) {
				onSleeve |= before[g.at(x, y)];
			}
		}
		assertTrue(onSleeve, "the mirror reproduces the reported bug with the old rules");
	}

	@Test
	void nearCoplanarIslandsMakeNoRings() {
		// Sleeve across the whole area; the hidden blade touches the sleeve surface on single pixels (z-fight winners
		// of the old offset test). The arm won those pixels in the scene, so they are no seeds.
		Grid g = new Grid(24, 9);
		g.scene(0, 0, 23, 8, 0.4f);
		g.item(2, 2, 21, 6, 0.6f);
		for (int x = 3; x <= 20; x += 5) {
			g.itemAt(x, 4, 0.40002f); // 0.02 mm behind the sleeve
		}
		boolean[] now = g.composite(true, OutlineDepth.HAND_TOLERANCE);
		for (boolean drawn : now) {
			assertFalse(drawn, "ring on the sleeve");
		}
		boolean[] before = g.legacy(window(0.40002f) - window(0.4f) + 1.0e-7f, OutlineDepth.WORLD_TOLERANCE);
		assertTrue(before[g.at(2, 4)] && before[g.at(4, 4)] && before[g.at(3, 3)] && before[g.at(3, 5)],
			"the old rules draw a closed ring around each island");
	}

	@Test
	void swordHalfBehindAPillarIsOutlinedOnlyAroundTheVisiblePart() {
		Grid g = new Grid(24, 12);
		g.item(3, 4, 16, 7, 10.0f);
		g.scene(10, 0, 23, 11, 8.0f); // pillar 2 blocks in front
		boolean[] now = g.composite(true, OutlineDepth.WORLD_TOLERANCE);
		for (int y = 0; y < 12; y++) {
			for (int x = 10; x < 24; x++) {
				assertFalse(now[g.at(x, y)], "outline on the pillar at " + x + "," + y);
			}
		}
		assertTrue(now[g.at(2, 5)] && now[g.at(6, 3)] && now[g.at(6, 8)] && now[g.at(9, 3)]);

		// Sword leaning on the pillar (pillar only 5 cm in front): the old front rule kept the outline on the pillar
		// over the hidden half; the cover rule removes it.
		Grid lean = new Grid(24, 12);
		lean.item(3, 4, 16, 7, 10.0f);
		lean.scene(10, 0, 23, 11, 9.95f);
		boolean[] leaning = lean.composite(true, OutlineDepth.WORLD_TOLERANCE);
		for (int y = 4; y <= 7; y++) {
			assertFalse(leaning[lean.at(10, y)] || leaning[lean.at(11, y)]);
		}
	}

	@Test
	void anotherItemHiddenBehindAWallDoesNotCutAVisibleOutline() {
		Grid g = new Grid(24, 12);
		g.scene(0, 0, 23, 11, 6.0f); // wall
		g.item(4, 4, 9, 7, 5.0f); // visible, in front of the wall
		g.scene(4, 4, 9, 7, 5.0f);
		g.item(10, 3, 18, 8, 7.0f); // hidden behind the wall, right next to it on screen
		boolean[] now = g.composite(true, OutlineDepth.WORLD_TOLERANCE);
		assertTrue(now[g.at(10, 5)] && now[g.at(11, 5)], "outline of the visible item over the wall");
		assertFalse(now[g.at(15, 5)], "the hidden item has no outline");
	}

	@Test
	void withoutDepthEveryMaskPixelIsASeed() {
		// GUI and the shader-pack hand: no scene depth, the mask alone decides.
		Grid g = new Grid(16, 10);
		g.item(4, 3, 9, 6, 0.5f);
		g.scene(0, 0, 15, 9, 0.1f); // would hide everything if it were used
		boolean[] flat = g.composite(false, 0.0f);
		assertTrue(flat[g.at(3, 4)] && flat[g.at(10, 4)] && flat[g.at(6, 2)] && flat[g.at(6, 7)]);
		assertFalse(flat[g.at(6, 4)]);
	}

	// ---- mirror

	/** Window depth of a linear distance (inverse of the composite's linearDepth), quantised like a depth buffer. */
	private static float window(float linear) {
		float z = ((FAR + NEAR - 2.0f * NEAR * FAR / linear) / (FAR - NEAR) + 1.0f) * 0.5f;
		return OutlineDepth.pack(z) / (float) OutlineDepth.DEPTH_STEPS;
	}

	private static final class Grid {
		final int w;
		final int h;
		final int[] id;
		final float[] itemZ;
		final float[] sceneZ;

		Grid(int w, int h) {
			this.w = w;
			this.h = h;
			id = new int[w * h];
			itemZ = new float[w * h];
			sceneZ = new float[w * h];
			java.util.Arrays.fill(itemZ, 1.0f);
			java.util.Arrays.fill(sceneZ, 1.0f);
		}

		int at(int x, int y) {
			return y * w + x;
		}

		/** Item rectangle at a linear distance, also written to the scene where it is nearest (like the real draw). */
		void item(int x0, int y0, int x1, int y1, float linear) {
			for (int y = y0; y <= y1; y++) {
				for (int x = x0; x <= x1; x++) {
					itemAt(x, y, linear);
				}
			}
		}

		void itemAt(int x, int y, float linear) {
			float z = window(linear);
			int i = at(x, y);
			if (z <= itemZ[i]) {
				// mask.fsh: nearest item surface, depth packed into the colour
				int[] b = OutlineDepth.bytes(OutlineDepth.pack(z));
				itemZ[i] = OutlineDepth.unpack(b[0], b[1], b[2]);
				id[i] = 1;
			}
			sceneZ[i] = Math.min(sceneZ[i], z);
		}

		/** Scene geometry drawn after the item, with LEQUAL. */
		void scene(int x0, int y0, int x1, int y1, float linear) {
			float z = window(linear);
			for (int y = y0; y <= y1; y++) {
				for (int x = x0; x <= x1; x++) {
					int i = at(x, y);
					sceneZ[i] = Math.min(sceneZ[i], z);
				}
			}
		}

		boolean visible(int i) {
			return OutlineDepth.visible(sceneZ[i], itemZ[i]);
		}

		/** outline_edt.fsh then outline_composite.fsh (hard edge: every pixel with a seed within RADIUS). */
		boolean[] composite(boolean useDepth, float frontTolerance) {
			boolean[] seed = new boolean[w * h];
			for (int i = 0; i < seed.length; i++) {
				seed[i] = id[i] > 0 && (!useDepth || visible(i));
			}
			boolean[] out = new boolean[w * h];
			int[] dx = horizontal(seed);
			for (int y = 0; y < h; y++) {
				for (int x = 0; x < w; x++) {
					int p = at(x, y);
					boolean onHidden = false;
					if (id[p] > 0) {
						if (!useDepth || visible(p)) {
							continue;
						}
						onHidden = true;
					}
					int s = vertical(dx, x, y);
					if (s < 0) {
						continue;
					}
					if (useDepth) {
						float zp = OutlineDepth.linear(sceneZ[p], NEAR, FAR);
						float zs = OutlineDepth.linear(sceneZ[s], NEAR, FAR);
						if (OutlineDepth.hidden(onHidden, zp, zs, frontTolerance)) {
							continue;
						}
					}
					out[p] = true;
				}
			}
			return out;
		}

		/**
		 * The pipeline before the fix: the mask held item pixels passing LEQUAL against the scene with a polygon
		 * offset ({@code slack} in window depth), every mask pixel was a seed and the seed depth was the scene depth.
		 */
		boolean[] legacy(float slack, float tolerance) {
			boolean[] seed = new boolean[w * h];
			for (int i = 0; i < seed.length; i++) {
				seed[i] = id[i] > 0 && itemZ[i] - slack <= sceneZ[i];
			}
			boolean[] out = new boolean[w * h];
			int[] dx = horizontal(seed);
			for (int y = 0; y < h; y++) {
				for (int x = 0; x < w; x++) {
					int p = at(x, y);
					if (seed[p]) {
						continue;
					}
					int s = vertical(dx, x, y);
					if (s < 0) {
						continue;
					}
					float zp = OutlineDepth.linear(sceneZ[p], NEAR, FAR);
					float zs = OutlineDepth.linear(sceneZ[s], NEAR, FAR);
					if (zp < zs - Math.max(tolerance, zs * OutlineDepth.RELATIVE_TOLERANCE)) {
						continue;
					}
					out[p] = true;
				}
			}
			return out;
		}

		/** Pass H: signed offset to the nearest seed on the row, right side first; MIN_VALUE = none. */
		private int[] horizontal(boolean[] seed) {
			int[] dx = new int[w * h];
			for (int y = 0; y < h; y++) {
				for (int x = 0; x < w; x++) {
					int found = Integer.MIN_VALUE;
					for (int i = 0; i <= RADIUS && found == Integer.MIN_VALUE; i++) {
						if (x + i < w && seed[at(x + i, y)]) {
							found = i;
						} else if (i > 0 && x - i >= 0 && seed[at(x - i, y)]) {
							found = -i;
						}
					}
					dx[at(x, y)] = found;
				}
			}
			return dx;
		}

		/** Pass V: index of the nearest seed, or -1. */
		private int vertical(int[] dx, int x, int y) {
			float best = 1.0e9f;
			int seed = -1;
			for (int j = 0; j <= RADIUS; j++) {
				if (j * j >= best) {
					break;
				}
				for (int s = -1; s <= 1; s += 2) {
					if (j == 0 && s > 0) {
						continue;
					}
					int yy = y + s * j;
					if (yy < 0 || yy >= h || dx[at(x, yy)] == Integer.MIN_VALUE) {
						continue;
					}
					int d = dx[at(x, yy)];
					float d2 = d * d + j * j;
					if (d2 < best) {
						best = d2;
						seed = at(x + d, yy);
					}
				}
			}
			return best <= RADIUS * RADIUS ? seed : -1;
		}
	}
}
