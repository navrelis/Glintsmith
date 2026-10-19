package navrelis.glintsmith.render.outline;

import java.util.function.Supplier;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.ShaderInstance;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;

/**
 * Saves and restores the GL state the compositor touches, so the outline passes leave the frame exactly as they
 * found it (whatever phase they run in): framebuffers, viewport, scissor, blend, depth, cull, the active texture
 * unit and the 2D textures bound on the units the outline shaders use, {@code RenderSystem}'s shader and its shader
 * texture 0. Values are read from GL and written back through {@link GlStateManager}, which keeps vanilla's state
 * cache in line. Preallocated, render thread only.
 */
final class GlState {
	/** Texture units the outline shaders bind (four samplers in the composite pass). */
	static final int TEXTURE_UNITS = 4;

	private final int[] viewport = new int[4];
	private final int[] scissorBox = new int[4];
	private final int[] textures = new int[TEXTURE_UNITS];
	private int drawFramebuffer;
	private int readFramebuffer;
	private boolean scissor;
	private boolean blend;
	private int blendSrcRgb;
	private int blendDstRgb;
	private int blendSrcAlpha;
	private int blendDstAlpha;
	private boolean depthTest;
	private int depthFunc;
	private boolean depthMask;
	private boolean cull;
	private int activeTexture;
	private int shaderTexture0;
	private ShaderInstance shader;
	private final Supplier<ShaderInstance> savedShader = () -> shader;

	void save() {
		drawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
		readFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
		GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
		scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
		GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, scissorBox);
		blend = GL11.glIsEnabled(GL11.GL_BLEND);
		blendSrcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
		blendDstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
		blendSrcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
		blendDstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
		depthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
		depthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
		depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
		cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
		activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
		for (int i = 0; i < TEXTURE_UNITS; i++) {
			GL13.glActiveTexture(GL13.GL_TEXTURE0 + i);
			textures[i] = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
		}
		GL13.glActiveTexture(activeTexture);
		shaderTexture0 = RenderSystem.getShaderTexture(0);
		shader = RenderSystem.getShader();
	}

	/** Whether the scissor test was on when the state was saved (GUI scroll areas). */
	boolean scissorEnabled() {
		return scissor;
	}

	/** The saved scissor box: x, y, width, height. */
	int[] scissorBox() {
		return scissorBox;
	}

	void restore() {
		GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFramebuffer);
		GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFramebuffer);
		GlStateManager._viewport(viewport[0], viewport[1], viewport[2], viewport[3]);
		GlStateManager._scissorBox(scissorBox[0], scissorBox[1], scissorBox[2], scissorBox[3]);
		if (scissor) {
			GlStateManager._enableScissorTest();
		} else {
			GlStateManager._disableScissorTest();
		}
		GlStateManager._blendFuncSeparate(blendSrcRgb, blendDstRgb, blendSrcAlpha, blendDstAlpha);
		if (blend) {
			GlStateManager._enableBlend();
		} else {
			GlStateManager._disableBlend();
		}
		GlStateManager._depthFunc(depthFunc);
		GlStateManager._depthMask(depthMask);
		if (depthTest) {
			GlStateManager._enableDepthTest();
		} else {
			GlStateManager._disableDepthTest();
		}
		if (cull) {
			GlStateManager._enableCull();
		} else {
			GlStateManager._disableCull();
		}
		GlStateManager._colorMask(true, true, true, true);
		for (int i = 0; i < TEXTURE_UNITS; i++) {
			GlStateManager._activeTexture(GL13.GL_TEXTURE0 + i);
			GlStateManager._bindTexture(textures[i]);
		}
		GlStateManager._activeTexture(activeTexture);
		RenderSystem.setShaderTexture(0, shaderTexture0);
		RenderSystem.setShader(savedShader);
		shader = null;
	}
}
