package navrelis.glintsmith.render.outline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import navrelis.glintsmith.config.GlintsmithConfig;
import net.minecraft.client.renderer.block.model.ItemTransform;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

/**
 * The math of proportional outlines ({@link OutlineScale}) with the matrices vanilla 1.21.1 draws items with: the
 * first-person hand at rest is the reference (factor 1), GUI icons get {@code 16 × guiScale / 807.6}, world items scale
 * with {@code 1 / distance}, and the switch off gives the old same-pixels-everywhere widths.
 */
class OutlineScaleTest {
	private static final float EPS = 1.0e-3f;
	/** {@code firstperson_righthand} of item/generated and item/handheld (translation already in blocks). */
	private static final ItemTransform FIRST_PERSON = transform(0.0f, -90.0f, 25.0f, 1.13f, 3.2f, 1.13f, 0.68f);
	/** {@code ground} of item/generated. */
	private static final ItemTransform GROUND = transform(0.0f, 0.0f, 0.0f, 0.0f, 2.0f, 0.0f, 0.5f);
	/** {@code thirdperson_righthand} of item/handheld. */
	private static final ItemTransform THIRD_PERSON = transform(0.0f, -90.0f, 55.0f, 0.0f, 4.0f, 0.5f, 0.85f);

	private static ItemTransform transform(float rx, float ry, float rz, float tx, float ty, float tz, float scale) {
		return new ItemTransform(new Vector3f(rx, ry, rz), new Vector3f(tx / 16.0f, ty / 16.0f, tz / 16.0f),
			new Vector3f(scale, scale, scale));
	}

	/** {@code GameRenderer.getProjectionMatrix}: perspective with near 0.05. */
	private static Matrix4f perspective(float fovDegrees, int width, int height) {
		return new Matrix4f().perspective((float) Math.toRadians(fovDegrees), (float) width / height, 0.05f, 512.0f);
	}

	/** The GUI projection of {@code GameRenderer.render}. */
	private static Matrix4f guiProjection(int width, int height, double guiScale) {
		return new Matrix4f().setOrtho(0.0f, (float) (width / guiScale), (float) (height / guiScale), 0.0f, 1000.0f, 21000.0f);
	}

	/** The first-person right-hand item at rest: arm offset of {@code applyItemArmTransform}, then the display transform. */
	private static Matrix4f firstPersonModelToView(Matrix4f cameraRotation) {
		// renderItemInHand: model-view = camera rotation, pose = its inverse, so the item ends up in eye space.
		Matrix4f pose = new Matrix4f(cameraRotation).invert().translate(0.56f, -0.52f, -0.72f);
		return new Matrix4f(cameraRotation).mul(OutlineScale.itemPose(new Matrix4f(), pose, FIRST_PERSON, false));
	}

	/** A GUI item at x, y: {@code GuiGraphics.renderItem} pose under the GUI model-view. */
	private static Matrix4f guiModelToView(int x, int y) {
		Matrix4f modelView = new Matrix4f().translation(0.0f, 0.0f, -11000.0f);
		Matrix4f pose = new Matrix4f().translate(x + 8, y + 8, 150.0f).scale(16.0f, -16.0f, 16.0f);
		return modelView.mul(OutlineScale.itemPose(new Matrix4f(), pose, ItemTransform.NO_TRANSFORM, false));
	}

	/** An item {@code distance} blocks in front of a camera looking along {@code yawDeg}, drawn with {@code display}. */
	private static Matrix4f worldModelToView(float distance, float yawDeg, float sideways, ItemTransform display) {
		Matrix4f camera = new Matrix4f().rotationY((float) Math.toRadians(yawDeg));
		// Camera-relative entity position: straight ahead of the camera in world space, then the camera rotation.
		Vector3f ahead = new Matrix4f(camera).invert().transformDirection(new Vector3f(sideways, 0.0f, -distance));
		Matrix4f pose = new Matrix4f().translation(ahead).rotateY(1.3f);
		return new Matrix4f(camera).mul(OutlineScale.itemPose(new Matrix4f(), pose, display, false));
	}

	private static OutlineRuntimeSettings settings(boolean proportional, boolean scaleWithResolution) {
		return settings(proportional, scaleWithResolution, 1.0f);
	}

	private static OutlineRuntimeSettings settings(boolean proportional, boolean scaleWithResolution, float minThickness) {
		GlintsmithConfig config = new GlintsmithConfig().sanitize();
		config.outline.proportional = proportional;
		config.outline.scaleWithResolution = scaleWithResolution;
		config.outline.minThickness = minThickness;
		return OutlineRuntimeSettings.from(config);
	}

	@Test
	void referenceIsTheFirstPersonItemAt1080() {
		assertEquals(807.6f, OutlineScale.REFERENCE_PIXELS_PER_UNIT, 0.1f);
		assertEquals(0.649375f, OutlineScale.REFERENCE_ITEM_DISTANCE, 1.0e-6f);
	}

	@Test
	void firstPersonAtRestHasFactorOne() {
		Matrix4f[] cameras = { new Matrix4f(), new Matrix4f().rotationXYZ(0.4f, 2.1f, 0.0f), new Matrix4f().rotationY(-3.0f) };
		for (Matrix4f camera : cameras) {
			float ppu = OutlineScale.pixelsPerUnit(perspective(70.0f, 1920, 1080), firstPersonModelToView(camera), 1080);
			assertEquals(1.0f, OutlineScale.factor(ppu), EPS, "camera " + camera);
		}
		// The vertical FOV is fixed, so the aspect ratio does not matter; the height scales it.
		assertEquals(1.0f, OutlineScale.factor(OutlineScale.pixelsPerUnit(perspective(70.0f, 2560, 1080),
			firstPersonModelToView(new Matrix4f()), 1080)), EPS);
		assertEquals(2.0f, OutlineScale.factor(OutlineScale.pixelsPerUnit(perspective(70.0f, 3840, 2160),
			firstPersonModelToView(new Matrix4f()), 2160)), EPS);
		assertEquals(4.0f / 3.0f, OutlineScale.factor(OutlineScale.pixelsPerUnit(perspective(70.0f, 2560, 1440),
			firstPersonModelToView(new Matrix4f()), 1440)), EPS);
	}

	@Test
	void ownerDefaultIsEightAndAHalfPixelsOnTheFirstPersonItemAt1080() {
		float ppu = OutlineScale.pixelsPerUnit(perspective(70.0f, 1920, 1080), firstPersonModelToView(new Matrix4f()), 1080);
		float thickness = settings(true, true).itemThickness(8.5f, ppu, 1080);
		assertEquals(8.5f, thickness, 0.01f);
		StyleTable table = new StyleTable();
		assertEquals(68, table.thicknessByte(table.indexFor(0x40FFFFFF, thickness, 0.0f)), "8.5 px after quantising");
	}

	@Test
	void leftHandMirrorsTheTranslationOnly() {
		Matrix4f right = OutlineScale.itemPose(new Matrix4f(), new Matrix4f(), FIRST_PERSON, false);
		Matrix4f left = OutlineScale.itemPose(new Matrix4f(), new Matrix4f(), FIRST_PERSON, true);
		assertEquals(-right.m30(), left.m30(), 1.0e-6f);
		assertEquals(right.m31(), left.m31(), 1.0e-6f);
		assertEquals(right.m32(), left.m32(), 1.0e-6f);
		Matrix4f leftArm = new Matrix4f().translate(-0.56f, -0.52f, -0.72f);
		float ppu = OutlineScale.pixelsPerUnit(perspective(70.0f, 1920, 1080),
			OutlineScale.itemPose(new Matrix4f(), leftArm, FIRST_PERSON, true), 1080);
		assertEquals(1.0f, OutlineScale.factor(ppu), EPS, "the off hand is the reference too");
	}

	@Test
	void guiIconsGetSixteenGuiPixelsPerUnit() {
		for (int guiScale = 1; guiScale <= 6; guiScale++) {
			float ppu = OutlineScale.pixelsPerUnit(guiProjection(1920, 1080, guiScale), guiModelToView(40, 60), 1080);
			assertEquals(16.0f * guiScale, ppu, 1.0e-3f, "gui scale " + guiScale);
			assertEquals(OutlineScale.guiPixelsPerUnit(guiScale), ppu, 1.0e-3f);
		}
	}

	@Test
	void guiIconBandIsTheFirstPersonBandScaledToTheIconSize() {
		OutlineRuntimeSettings s = settings(true, true);
		double[][] expected = { // gui scale, 8.5 px look, 16 px look (before the 1 px minimum)
			{ 2, 8.5 * 32.0 / 807.57, 16.0 * 32.0 / 807.57 }, // 32 px icon: 0.34 and 0.63 px, both 1 px
			{ 3, 8.5 * 48.0 / 807.57, 16.0 * 48.0 / 807.57 }, // 48 px icon: 0.51 and 0.95 px, both 1 px
			{ 4, 8.5 * 64.0 / 807.57, 16.0 * 64.0 / 807.57 }, // 64 px icon: 0.67 px (1 px) and 1.27 px
		};
		for (double[] row : expected) {
			float ppu = OutlineScale.pixelsPerUnit(guiProjection(1920, 1080, row[0]), guiModelToView(0, 0), 1080);
			assertEquals(Math.max(1.0, row[1]), s.itemThickness(8.5f, ppu, 1080), 0.01, "8.5 at gui scale " + row[0]);
			assertEquals(Math.max(1.0, row[2]), s.itemThickness(16.0f, ppu, 1080), 0.01, "16 at gui scale " + row[0]);
		}
		// At 4K (auto GUI scale 8) the 128 px icon is 1.35 px proportionally; the 1 px minimum is 2 px there.
		float ppu4k = OutlineScale.pixelsPerUnit(guiProjection(3840, 2160, 8), guiModelToView(0, 0), 2160);
		assertEquals(2.0f, s.itemThickness(8.5f, ppu4k, 2160), 1.0e-4f);
		assertEquals(16.0 * 128.0 / 807.57, s.itemThickness(16.0f, ppu4k, 2160), 0.01, "2.54 px, above the minimum");
	}

	@Test
	void worldItemsScaleWithOneOverDistance() {
		Matrix4f projection = perspective(70.0f, 1920, 1080);
		float near = OutlineScale.pixelsPerUnit(projection, worldModelToView(2.0f, 30.0f, 0.0f, GROUND), 1080);
		for (float distance : new float[] { 4.0f, 8.0f, 16.0f, 64.0f }) {
			float far = OutlineScale.pixelsPerUnit(projection, worldModelToView(distance, 30.0f, 0.0f, GROUND), 1080);
			assertEquals(near * 2.0f / distance, far, far * 1.0e-3f, "distance " + distance);
		}
		// Off the view axis the depth, not the distance, counts (as for the item's own pixels).
		float offAxis = OutlineScale.pixelsPerUnit(projection, worldModelToView(4.0f, 30.0f, 2.0f, GROUND), 1080);
		assertEquals(near / 2.0f, offAxis, offAxis * 1.0e-3f);
		// A dropped item 2 blocks away (ground scale 0.5): 0.5 × 771.2 / 2 px per unit at FOV 70.
		assertEquals(0.5f * 540.0f / (float) Math.tan(Math.toRadians(35.0)) / 2.0f, near, 0.05f);
	}

	@Test
	void displayScaleCounts() {
		Matrix4f projection = perspective(70.0f, 1920, 1080);
		float ground = OutlineScale.pixelsPerUnit(projection, worldModelToView(4.0f, 0.0f, 0.0f, GROUND), 1080);
		float third = OutlineScale.pixelsPerUnit(projection, worldModelToView(4.0f, 0.0f, 0.0f, THIRD_PERSON), 1080);
		// The display translation moves the centre a little, so compare at the measured depth: about 0.85 / 0.5.
		assertEquals(0.85f / 0.5f, third / ground, 0.03f);
	}

	@Test
	void sameSizeOnScreenGivesTheSameBandInEveryPhase() {
		OutlineRuntimeSettings s = settings(true, true);
		// A GUI-style orthographic item 48 px per unit, and a world item placed where it is 48 px per unit too.
		float gui = OutlineScale.pixelsPerUnit(guiProjection(1920, 1080, 3), guiModelToView(0, 0), 1080);
		float distance = 0.5f * 540.0f / (float) Math.tan(Math.toRadians(35.0)) / 48.0f;
		float world = OutlineScale.pixelsPerUnit(perspective(70.0f, 1920, 1080), worldModelToView(distance, 0.0f, 0.0f,
			transform(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.5f)), 1080);
		assertEquals(gui, world, 0.01f);
		assertEquals(s.itemThickness(12.0f, gui, 1080), s.itemThickness(12.0f, world, 1080), 1.0e-3f);
	}

	@Test
	void farItemsKeepAtLeastOnePixel() {
		assertEquals(1.0f, OutlineScale.thickness(8.5f, 1.0f, 1.0f, 1080), 0.0f);
		assertEquals(1.0f, OutlineScale.thickness(16.0f, 0.01f, 1.0f, 1080), 0.0f);
		assertEquals(0.5f, OutlineScale.thickness(0.5f, 1.0f, 1.0f, 1080), 0.0f, "a look thinner than 1 px is its own minimum");
		assertEquals(0.75f, OutlineScale.thickness(0.75f, 10.0f, 1.0f, 1080), 0.0f);
		assertEquals(1.0f, OutlineScale.thickness(8.5f, 1.0f, 1.0f, 0), 0.0f, "minimised window counts as 1080");
		Matrix4f projection = perspective(70.0f, 1920, 1080);
		float veryFar = OutlineScale.pixelsPerUnit(projection, worldModelToView(200.0f, 0.0f, 0.0f, GROUND), 1080);
		assertEquals(1.0f, settings(true, true).itemThickness(8.5f, veryFar, 1080), 0.0f);
	}

	@Test
	void minimumWidthAppliesToGuiIconsAndFarItems() {
		OutlineRuntimeSettings s = settings(true, true, 3.0f);
		float icon = OutlineScale.pixelsPerUnit(guiProjection(1920, 1080, 3), guiModelToView(0, 0), 1080);
		assertEquals(3.0f, s.itemThickness(8.5f, icon, 1080), 1.0e-5f, "48 px icon: 0.51 px raised to 3");
		Matrix4f projection = perspective(70.0f, 1920, 1080);
		float far = OutlineScale.pixelsPerUnit(projection, worldModelToView(40.0f, 0.0f, 0.0f, GROUND), 1080);
		assertEquals(3.0f, s.itemThickness(8.5f, far, 1080), 1.0e-5f);
		float near = OutlineScale.pixelsPerUnit(projection, worldModelToView(1.0f, 0.0f, 0.0f, GROUND), 1080);
		assertEquals(8.5f * near / OutlineScale.REFERENCE_PIXELS_PER_UNIT, s.itemThickness(8.5f, near, 1080), 1.0e-4f,
			"wider items are not touched");
		float reference = OutlineScale.REFERENCE_PIXELS_PER_UNIT;
		assertEquals(8.5f, s.itemThickness(8.5f, reference, 1080), 1.0e-4f, "the first-person item is not touched");
	}

	@Test
	void minimumWidthScalesWithTheWindowHeight() {
		OutlineRuntimeSettings s = settings(true, false, 3.0f);
		float tiny = 1.0f;
		assertEquals(3.0f, s.itemThickness(8.5f, tiny, 1080), 1.0e-5f);
		assertEquals(4.0f, s.itemThickness(8.5f, tiny, 1440), 1.0e-5f);
		assertEquals(6.0f, s.itemThickness(8.5f, tiny, 2160), 1.0e-5f);
		assertEquals(2.0f, s.itemThickness(8.5f, tiny, 720), 1.0e-5f);
	}

	@Test
	void minimumWidthNeverMakesAnItemWiderThanItsOwnReferenceWidth() {
		OutlineRuntimeSettings s = settings(true, true, 8.0f);
		for (int height : new int[] { 720, 1080, 2160 }) {
			float scale = height / 1080.0f;
			assertEquals(2.0f * scale, s.itemThickness(2.0f, 1.0f, height), 1.0e-5f, "T 2 below a minimum of 8");
			assertEquals(8.0f * scale, s.itemThickness(12.0f, 1.0f, height), 1.0e-5f, "T 12 above it");
			// Every item smaller on screen than the reference item at this height.
			for (float ppu = 1.0f; ppu < OutlineScale.REFERENCE_PIXELS_PER_UNIT * scale; ppu *= 1.5f) {
				assertTrue(s.itemThickness(5.0f, ppu, height) <= 5.0f * scale + 1.0e-4f, ppu + "/" + height);
			}
		}
	}

	@Test
	void minimumWidthIsIgnoredWhenProportionalIsOff() {
		for (boolean scaled : new boolean[] { true, false }) {
			OutlineRuntimeSettings s = settings(false, scaled, 8.0f);
			assertEquals(1.0f * s.thicknessScale(2160), s.itemThickness(1.0f, 1.0f, 2160), 0.0f);
			assertEquals(0.5f * s.thicknessScale(1080), s.itemThickness(0.5f, 0.01f, 1080), 0.0f);
		}
	}

	@Test
	void minimumWidthIsClampedWhenTheSettingsAreBuilt() {
		assertEquals(8.0f, settings(true, true, 100.0f).minThickness());
		assertEquals(0.5f, settings(true, true, 0.0f).minThickness());
		assertEquals(0.5f, settings(true, true, Float.NEGATIVE_INFINITY).minThickness());
		assertEquals(1.0f, settings(true, true, Float.NaN).minThickness());
		assertEquals(OutlineScale.DEFAULT_MIN_THICKNESS, OutlineRuntimeSettings.DISABLED.minThickness());
	}

	@Test
	void reachIncludesTheMinimumWidth() {
		OutlineRuntimeSettings s = settings(true, true, 4.0f);
		float icon = OutlineScale.guiPixelsPerUnit(2.0);
		assertEquals(5, s.outlineReachPixels(8.5f, 0.0f, 1080, icon), "ceil(4 + 1)");
		assertEquals(9, s.outlineReachPixels(8.5f, 0.0f, 2160, icon), "ceil(8 + 1) at 2160");
		assertEquals(2, settings(true, true, 1.0f).outlineReachPixels(8.5f, 0.0f, 1080, icon));
	}

	@Test
	void hugeItemsAreCappedByTheStyleTable() {
		Matrix4f projection = perspective(70.0f, 1920, 1080);
		// A dropped item 0.06 blocks from the eye: factor about 8.
		float close = OutlineScale.pixelsPerUnit(projection, worldModelToView(0.06f, 0.0f, 0.0f, GROUND), 1080);
		float thickness = settings(true, true).itemThickness(16.0f, close, 1080);
		assertTrue(thickness > 100.0f);
		StyleTable table = new StyleTable();
		table.indexFor(0xFFFFFFFF, thickness, 1.0f);
		assertEquals(StyleTable.MAX_RADIUS, table.maxRadius());
	}

	@Test
	void switchOffKeepsTheSamePixelsEverywhere() {
		for (boolean scaled : new boolean[] { true, false }) {
			OutlineRuntimeSettings s = settings(false, scaled);
			for (float ppu : new float[] { 1.0f, 64.0f, OutlineScale.REFERENCE_PIXELS_PER_UNIT, 5000.0f, Float.NaN }) {
				for (int height : new int[] { 720, 1080, 2160 }) {
					assertEquals(8.5f * s.thicknessScale(height), s.itemThickness(8.5f, ppu, height), 0.0f,
						scaled + "/" + ppu + "/" + height);
				}
			}
		}
	}

	@Test
	void proportionalIgnoresScaleWithResolution() {
		float ppu = OutlineScale.guiPixelsPerUnit(4.0);
		assertEquals(settings(true, true).itemThickness(8.5f, ppu, 2160), settings(true, false).itemThickness(8.5f, ppu, 2160),
			0.0f);
	}

	@Test
	void unknownSizeCountsAsTheReferenceItem() {
		OutlineRuntimeSettings s = settings(true, false);
		assertEquals(8.5f, s.itemThickness(8.5f, Float.NaN, 1080), 1.0e-6f);
		assertEquals(17.0f, s.itemThickness(8.5f, Float.NaN, 2160), 1.0e-6f);
		assertEquals(8.5f, s.itemThickness(8.5f, 0.0f, 1080), 1.0e-6f);
		assertEquals(8.5f, s.itemThickness(8.5f, -3.0f, 1080), 1.0e-6f);
		assertEquals(8.5f, s.itemThickness(8.5f, Float.POSITIVE_INFINITY, 1080), 1.0e-6f);
		assertEquals(8.5f, s.itemThickness(8.5f, Float.NaN, 0), 1.0e-6f, "minimised window");
	}

	@Test
	void invalidInputsGiveNoMeasure() {
		Matrix4f projection = perspective(70.0f, 1920, 1080);
		Matrix4f item = worldModelToView(3.0f, 0.0f, 0.0f, GROUND);
		assertFalse(Float.isNaN(OutlineScale.pixelsPerUnit(projection, item, 1080)));
		assertTrue(Float.isNaN(OutlineScale.pixelsPerUnit(null, item, 1080)));
		assertTrue(Float.isNaN(OutlineScale.pixelsPerUnit(projection, null, 1080)));
		assertTrue(Float.isNaN(OutlineScale.pixelsPerUnit(projection, item, 0)));
		assertTrue(Float.isNaN(OutlineScale.pixelsPerUnit(projection, item, -1080)));
		Matrix4f behind = new Matrix4f().translation(0.0f, 0.0f, 2.0f);
		assertTrue(Float.isNaN(OutlineScale.pixelsPerUnit(projection, behind, 1080)), "centre behind the eye");
		Matrix4f atEye = new Matrix4f();
		assertTrue(Float.isNaN(OutlineScale.pixelsPerUnit(projection, atEye, 1080)), "centre at the eye");
		Matrix4f flat = new Matrix4f().translation(0.0f, 0.0f, -3.0f).scale(0.0f);
		assertTrue(Float.isNaN(OutlineScale.pixelsPerUnit(projection, flat, 1080)), "degenerate scale");
		Matrix4f broken = new Matrix4f().translation(0.0f, 0.0f, Float.NaN);
		assertTrue(Float.isNaN(OutlineScale.pixelsPerUnit(projection, broken, 1080)));
		Matrix4f infinite = new Matrix4f().translation(0.0f, 0.0f, -3.0f).scale(Float.POSITIVE_INFINITY);
		assertTrue(Float.isNaN(OutlineScale.pixelsPerUnit(projection, infinite, 1080)));
	}

	@Test
	void flattenedModelsKeepTheirSize() {
		// Some mods flatten GUI items in z; the longest model axis still gives the icon's size.
		Matrix4f modelView = new Matrix4f().translation(0.0f, 0.0f, -11000.0f);
		Matrix4f pose = new Matrix4f().translate(8.0f, 8.0f, 150.0f).scale(16.0f, -16.0f, 0.001f);
		assertEquals(48.0f, OutlineScale.pixelsPerUnit(guiProjection(1920, 1080, 3), modelView.mul(pose), 1080), 1.0e-3f);
	}

	@Test
	void reachFollowsTheItemSize() {
		OutlineRuntimeSettings s = settings(true, true);
		assertEquals(10, s.outlineReachPixels(8.5f, 0.0f, 1080, OutlineScale.REFERENCE_PIXELS_PER_UNIT)); // ceil(8.5 + 1)
		assertEquals(10, s.outlineReachPixels(8.5f, 0.0f, 1080), "unknown size: the reference item");
		assertEquals(2, s.outlineReachPixels(8.5f, 0.0f, 1080, OutlineScale.guiPixelsPerUnit(2.0)), "1 px band");
		assertEquals(StyleTable.MAX_RADIUS, s.outlineReachPixels(16.0f, 1.0f, 2160, 4000.0f));
		for (float ppu = 10.0f; ppu < 6000.0f; ppu *= 1.7f) {
			float thickness = s.itemThickness(8.5f, ppu, 1080);
			StyleTable table = new StyleTable();
			table.indexFor(0xFFFFFFFF, thickness, 0.5f);
			assertEquals(table.maxRadius(), s.outlineReachPixels(8.5f, 0.5f, 1080, ppu), "ppu " + ppu);
		}
	}
}
