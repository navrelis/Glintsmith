package navrelis.glintsmith.render.outline;

import net.minecraft.client.renderer.block.model.ItemTransform;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

/**
 * The math of proportional outlines ({@code outline.proportional}): how big an item is on screen, and how wide its
 * outline therefore gets. Pure functions on JOML matrices, no GL, no allocation.
 * <p>
 * The measure is the item's size on screen per model unit (one block, the size of a flat item's 16x16 sprite), in
 * framebuffer pixels, at the item's centre: {@link #pixelsPerUnit}. A thickness {@code T} means "T pixels on the
 * item held in first person at the reference setup" (framebuffer height {@link #REFERENCE_HEIGHT}, the fixed 70°
 * hand projection, the hand at rest). Any other item gets {@code T × pixelsPerUnit / }{@link #REFERENCE_PIXELS_PER_UNIT},
 * so the band keeps the same width relative to the item wherever and however large the item is drawn: a 48 px
 * inventory icon gets a thin band, a dropped item gets a band that shrinks with distance like the item does.
 */
public final class OutlineScale {
	/** Framebuffer height of the reference setup. */
	public static final float REFERENCE_HEIGHT = 1080.0f;
	/** Vertical field of view of the hand projection; vanilla ignores the FOV option for the hand. */
	public static final float REFERENCE_HAND_FOV_DEGREES = 70.0f;
	/**
	 * Distance of the first-person right-hand item's centre from the eye at rest, in blocks: the arm offset
	 * {@code z = -0.72} of {@code ItemInHandRenderer.applyItemArmTransform} plus the display translation
	 * {@code z = 1.13 / 16} that {@code item/generated} and {@code item/handheld} use for {@code firstperson_righthand}.
	 */
	public static final float REFERENCE_ITEM_DISTANCE = 0.72f - 1.13f / 16.0f;
	/** Display scale of {@code firstperson_righthand} in {@code item/generated} and {@code item/handheld}. */
	public static final float REFERENCE_DISPLAY_SCALE = 0.68f;
	/**
	 * Size on screen of one model unit of the first-person held item at the reference setup, in framebuffer pixels:
	 * {@code 0.68 × (1080 / 2) / tan(35°) / 0.649375 ≈ 807.6}.
	 */
	public static final float REFERENCE_PIXELS_PER_UNIT = (float) (REFERENCE_DISPLAY_SCALE * (REFERENCE_HEIGHT / 2.0)
		/ Math.tan(Math.toRadians(REFERENCE_HAND_FOV_DEGREES / 2.0)) / REFERENCE_ITEM_DISTANCE);
	/**
	 * Default narrowest proportional band ({@code outline.minThickness}), in pixels at a framebuffer height of
	 * {@link #REFERENCE_HEIGHT}: one full pixel ring (diagonals included) around the silhouette. Below about one pixel
	 * far and tiny items would lose their outline, because the composite measures distances from pixel centres.
	 */
	public static final float DEFAULT_MIN_THICKNESS = 1.0f;
	/** Lower end of {@code outline.minThickness}. */
	public static final float MIN_THICKNESS_LOW = 0.5f;
	/** Upper end of {@code outline.minThickness}. */
	public static final float MIN_THICKNESS_HIGH = 8.0f;
	/** A GUI item is drawn 16 GUI pixels per model unit. */
	public static final float GUI_ITEM_SIZE = 16.0f;
	/** Clip w at or below this means the item's centre is at or behind the eye: no usable measure. */
	private static final float MIN_W = 1.0e-4f;
	private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

	private OutlineScale() {
	}

	/**
	 * The item's size on screen per model unit, at its centre.
	 * <p>
	 * {@code modelToView} maps the item's model space, centred on the origin (after the display transform and the
	 * {@code -0.5} shift vanilla applies), to view space; {@code projection} maps view space to clip space. The size is
	 * the longest model axis in view space ({@code s}, rotation-free) times the projection's vertical pixels per view
	 * unit at the centre's clip {@code w}: {@code s × |row1(P).xyz| × (height / 2) / w}. The vertical axis keeps it
	 * independent of the aspect ratio. Orthographic projections ({@code w = 1}) give {@code s × guiScale} in the GUI.
	 *
	 * @param framebufferHeight height of the target the item is drawn to, in pixels
	 * @return pixels per model unit, or {@code NaN} if there is no usable measure (invalid height or matrices, the
	 *         centre at or behind the eye, a degenerate model scale)
	 */
	public static float pixelsPerUnit(Matrix4fc projection, Matrix4fc modelToView, int framebufferHeight) {
		if (projection == null || modelToView == null || framebufferHeight <= 0) {
			return Float.NaN;
		}
		float cx = modelToView.m30();
		float cy = modelToView.m31();
		float cz = modelToView.m32();
		float cw = modelToView.m33();
		float w = projection.m03() * cx + projection.m13() * cy + projection.m23() * cz + projection.m33() * cw;
		if (!(w > MIN_W) || !Float.isFinite(w)) {
			return Float.NaN;
		}
		float rowY = length(projection.m01(), projection.m11(), projection.m21());
		float scale = Math.max(length(modelToView.m00(), modelToView.m01(), modelToView.m02()),
			Math.max(length(modelToView.m10(), modelToView.m11(), modelToView.m12()),
				length(modelToView.m20(), modelToView.m21(), modelToView.m22())));
		float pixels = scale * rowY * (framebufferHeight * 0.5f) / w;
		return pixels > 0.0f && Float.isFinite(pixels) ? pixels : Float.NaN;
	}

	/**
	 * @return the proportional factor of an item {@code pixelsPerUnit} large: 1 at the first-person reference
	 */
	public static float factor(float pixelsPerUnit) {
		return pixelsPerUnit / REFERENCE_PIXELS_PER_UNIT;
	}

	/**
	 * The proportional thickness of a look on an item, in framebuffer pixels, before the style table quantises it and
	 * caps it at {@link StyleTable#MAX_RADIUS}: {@code thickness × factor}, at least
	 * {@code min(minThickness, thickness) × framebufferHeight / 1080}. The minimum is in pixels at 1080 like the
	 * thickness, so it grows with the window (a 1 px line at 4K would be too thin), and it never makes an item wider
	 * than the look's own width on the reference item.
	 *
	 * @param thickness the look's thickness (pixels on the first-person reference item), greater than 0
	 * @param pixelsPerUnit {@link #pixelsPerUnit}; must be finite and greater than 0
	 * @param minThickness {@code outline.minThickness}, pixels at 1080
	 * @param framebufferHeight height of the main render target; 0 or less (minimised window) counts as 1080
	 */
	public static float thickness(float thickness, float pixelsPerUnit, float minThickness, int framebufferHeight) {
		float scaled = thickness * factor(pixelsPerUnit);
		float min = Math.min(minThickness, thickness) * heightScale(framebufferHeight);
		return scaled >= min ? scaled : min;
	}

	/**
	 * @return {@code framebufferHeight / 1080}, or 1 for a height of 0 or less (minimised window)
	 */
	public static float heightScale(int framebufferHeight) {
		return framebufferHeight > 0 ? framebufferHeight / REFERENCE_HEIGHT : 1.0f;
	}

	/**
	 * The size per model unit of a GUI item ({@code GuiGraphics.renderItem}: 16 GUI pixels per unit, display transform
	 * {@code gui} of {@code item/generated} is the identity).
	 */
	public static float guiPixelsPerUnit(double guiScale) {
		return (float) (GUI_ITEM_SIZE * guiScale);
	}

	/**
	 * Writes into {@code out} the pose an item model is drawn with: {@code pose} followed by the display transform
	 * exactly as {@code ItemTransform.apply} does it (translation, rotation XYZ, scale; mirrored for the left hand).
	 * The {@code -0.5} shift that follows in vanilla moves the model's centre onto the origin, so the origin of the
	 * result is the item's centre.
	 *
	 * @param transform the model's display transform for the context, or null for none
	 * @return {@code out}
	 */
	public static Matrix4f itemPose(Matrix4f out, Matrix4fc pose, @Nullable ItemTransform transform, boolean leftHand) {
		out.set(pose);
		if (transform == null || transform == ItemTransform.NO_TRANSFORM) {
			return out;
		}
		float rx = transform.rotation.x();
		float ry = transform.rotation.y();
		float rz = transform.rotation.z();
		if (leftHand) {
			ry = -ry;
			rz = -rz;
		}
		out.translate(leftHand ? -transform.translation.x() : transform.translation.x(), transform.translation.y(),
			transform.translation.z());
		out.rotateXYZ(rx * DEG_TO_RAD, ry * DEG_TO_RAD, rz * DEG_TO_RAD);
		out.scale(transform.scale.x(), transform.scale.y(), transform.scale.z());
		return out;
	}

	private static float length(float x, float y, float z) {
		return (float) Math.sqrt(x * x + y * y + z * z);
	}
}
