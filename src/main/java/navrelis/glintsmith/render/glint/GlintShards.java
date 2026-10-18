package navrelis.glintsmith.render.glint;

import org.joml.Matrix4f;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * The bodies of the shards of a glint slot: they bind the texture, set the texture matrix and the shader colour
 * (tint) and glint alpha (strength) right before a batch is drawn, and undo it afterwards. Render thread only.
 * <p>
 * Nothing here throws. A failure trips {@link GlintFailsafe} and falls back to the vanilla binding and matrix.
 * No allocation in the normal path.
 */
final class GlintShards {
	private static final Matrix4f MATRIX = new Matrix4f();
	private static final float[] SAVED_COLOR = new float[4];
	private static boolean saved;

	private GlintShards() {
	}

	static void setupTexture(GlintSlot<?> slot) {
		try {
			ResourceLocation texture = slot.params.texture;
			if (slot.isOff() || texture == null || GlintFailsafe.tripped()) {
				texture = slot.kind.vanillaTexture();
			}
			bind(texture);
		} catch (Throwable t) {
			GlintFailsafe.trip("glint texture shard", t);
			try {
				bind(slot.kind.vanillaTexture());
			} catch (Throwable ignored) {
				// nothing more to do
			}
		}
	}

	private static void bind(ResourceLocation texture) {
		AbstractTexture tex = Minecraft.getInstance().getTextureManager().getTexture(texture);
		tex.setFilter(true, false); // like the vanilla glint: blur, no mipmap
		RenderSystem.setShaderTexture(0, tex.getId());
	}

	static void setupTexturing(GlintSlot<?> slot) {
		try {
			if (GlintFailsafe.tripped() || slot.isOff()) {
				vanillaTexturing(slot.kind);
				return;
			}
			Options options = Minecraft.getInstance().options;
			GlintParams p = slot.params;

			long shifted = (long) (Util.getMillis() * options.glintSpeed().get() * 8.0 * p.speed);
			float g = (float) (shifted % 110000L) / 110000.0F;
			float h = (float) (shifted % 30000L) / 30000.0F;
			float s = slot.kind.baseScale / Math.max(0.05F, p.scale); // scale 2 = the pattern is twice as large
			// The atlas factor divides u and v before the rotation, i.e. in atlas UV space (it may differ per axis);
			// with factor 1 this is the uniform scale(s).
			MATRIX.translation(-g, h, 0.0F)
				.rotateZ(p.rotationDeg * Mth.DEG_TO_RAD)
				.scale(s / p.atlasU, s / p.atlasV, s);
			RenderSystem.setTextureMatrix(MATRIX);

			float[] current = RenderSystem.getShaderColor();
			System.arraycopy(current, 0, SAVED_COLOR, 0, 4);
			saved = true;
			int c = p.tintArgb;
			// The glint blend squares the colour, so the square root keeps the tint linear in the result.
			RenderSystem.setShaderColor(
				SAVED_COLOR[0] * (float) Math.sqrt(((c >> 16) & 255) / 255.0),
				SAVED_COLOR[1] * (float) Math.sqrt(((c >> 8) & 255) / 255.0),
				SAVED_COLOR[2] * (float) Math.sqrt((c & 255) / 255.0),
				SAVED_COLOR[3]);
			RenderSystem.setShaderGlintAlpha(options.glintStrength().get() * p.strength);
		} catch (Throwable t) {
			GlintFailsafe.trip("glint texturing shard", t);
			try {
				vanillaTexturing(slot.kind);
			} catch (Throwable ignored) {
				// nothing more to do
			}
		}
	}

	static void clearTexturing() {
		try {
			RenderSystem.resetTextureMatrix();
			if (saved) {
				RenderSystem.setShaderColor(SAVED_COLOR[0], SAVED_COLOR[1], SAVED_COLOR[2], SAVED_COLOR[3]);
				saved = false;
			}
			RenderSystem.setShaderGlintAlpha(Minecraft.getInstance().options.glintStrength().get());
		} catch (Throwable t) {
			GlintFailsafe.trip("glint texturing shard", t);
		}
	}

	/** The matrix of {@code RenderStateShard.setupGlintTexturing}, which is private. */
	private static void vanillaTexturing(GlintKind kind) {
		long shifted = (long) (Util.getMillis() * Minecraft.getInstance().options.glintSpeed().get() * 8.0);
		float g = (float) (shifted % 110000L) / 110000.0F;
		float h = (float) (shifted % 30000L) / 30000.0F;
		Matrix4f matrix = new Matrix4f().translation(-g, h, 0.0F);
		matrix.rotateZ((float) (Math.PI / 18)).scale(kind.baseScale);
		RenderSystem.setTextureMatrix(matrix);
	}
}
