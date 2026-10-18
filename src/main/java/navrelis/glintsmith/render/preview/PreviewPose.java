package navrelis.glintsmith.render.preview;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.block.model.ItemTransform;
import org.joml.Quaternionf;

/**
 * Pose math of the preview: cancelling a display transform and fitting the item into its box as a turntable.
 * Pure functions on a {@link PoseStack}, no rendering.
 */
final class PreviewPose {
	static final float MIN_SCALE = 1.0e-4f;
	private static final float MIN_EXTENT = 1.0e-3f;
	private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

	private PreviewPose() {
	}

	/**
	 * @return whether {@link #applyInverse} can cancel this transform: every component finite and no scale axis
	 *         (nearly) zero
	 */
	static boolean invertible(ItemTransform t) {
		if (t == null) {
			return false;
		}
		return finite(t.rotation.x()) && finite(t.rotation.y()) && finite(t.rotation.z())
			&& finite(t.translation.x()) && finite(t.translation.y()) && finite(t.translation.z())
			&& usableScale(t.scale.x()) && usableScale(t.scale.y()) && usableScale(t.scale.z());
	}

	private static boolean finite(float value) {
		return Float.isFinite(value);
	}

	private static boolean usableScale(float value) {
		return Float.isFinite(value) && Math.abs(value) >= MIN_SCALE;
	}

	/**
	 * Multiplies the inverse of {@code t} onto {@code pose}, so that vanilla's {@code t.apply(false, pose)} that
	 * follows cancels out. Vanilla applies {@code translate · rotationXYZ · scale} (translation already in blocks).
	 * Only call when {@link #invertible} holds.
	 */
	static void applyInverse(ItemTransform t, PoseStack pose) {
		pose.scale(1.0f / t.scale.x(), 1.0f / t.scale.y(), 1.0f / t.scale.z());
		pose.mulPose(new Quaternionf().rotationXYZ(t.rotation.x() * DEG_TO_RAD, t.rotation.y() * DEG_TO_RAD,
			t.rotation.z() * DEG_TO_RAD).conjugate());
		pose.translate(-t.translation.x(), -t.translation.y(), -t.translation.z());
	}

	/**
	 * How to place the item: a uniform scale (GUI pixels per block) and the centre of its bounds.
	 */
	record Fit(float scale, float cx, float cy, float cz) {
	}

	/**
	 * The largest scale at which the item, turned to any yaw at this pitch, stays inside a box of
	 * {@code boxW × boxH} GUI pixels less {@code marginGui} on each side, and within {@code ±maxDepthGui} in depth.
	 * At least 1.
	 * <p>
	 * Any yaw keeps the horizontal extent within the radius {@code r} of the bounds in x/z; the pitch then mixes that
	 * radius with the half height {@code hy} into the vertical and the depth extent.
	 */
	static Fit fit(float minX, float minY, float minZ, float maxX, float maxY, float maxZ,
		float boxW, float boxH, float marginGui, float pitchDeg, float maxDepthGui) {
		float cx = (minX + maxX) * 0.5f;
		float cy = (minY + maxY) * 0.5f;
		float cz = (minZ + maxZ) * 0.5f;
		float hx = Math.abs(maxX - minX) * 0.5f;
		float hy = Math.abs(maxY - minY) * 0.5f;
		float hz = Math.abs(maxZ - minZ) * 0.5f;
		float r = Math.max((float) Math.sqrt(hx * hx + hz * hz), MIN_EXTENT);
		double p = Math.toRadians(Math.abs(pitchDeg));
		float sin = (float) Math.sin(p);
		float cos = (float) Math.cos(p);
		float vertical = Math.max(hy * cos + r * sin, MIN_EXTENT);
		float depth = Math.max(hy * sin + r * cos, MIN_EXTENT);
		float scale = Math.min((boxW * 0.5f - marginGui) / r, (boxH * 0.5f - marginGui) / vertical);
		scale = Math.min(scale, maxDepthGui / depth);
		if (!(scale >= 1.0f)) {
			scale = 1.0f;
		}
		return new Fit(scale, cx, cy, cz);
	}

	/**
	 * The turntable: scale (y flipped, GUI y points down), pitch, yaw, then the bounds centre to the origin. The caller
	 * has already moved the origin to the box centre.
	 */
	static void turntable(PoseStack pose, Fit fit, float yawDeg, float pitchDeg) {
		pose.scale(fit.scale(), -fit.scale(), fit.scale());
		pose.mulPose(Axis.XP.rotationDegrees(pitchDeg));
		pose.mulPose(Axis.YP.rotationDegrees(yawDeg));
		pose.translate(-fit.cx(), -fit.cy(), -fit.cz());
	}
}
