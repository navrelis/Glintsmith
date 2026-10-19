package navrelis.glintsmith.neoforge.mixin;

import navrelis.glintsmith.GlintsmithClient;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderBuffers;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * World render start and last, at the same points Fabric API's {@code WorldRendererMixin} (fabric-rendering-v1) fires
 * {@code WorldRenderEvents.START} and {@code LAST}. NeoForge's {@code RenderLevelStageEvent} has no stage at either
 * point (its last stage, {@code AFTER_WEATHER}, comes before the world border and the transparency chain, and nothing
 * fires at the head of the method).
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererHooksMixin {
	@Final
	@Shadow
	private RenderBuffers renderBuffers;

	/** Fabric: HEAD of {@code render}; the consumers are the main buffer source of the render buffers. */
	@Inject(method = "renderLevel", at = @At("HEAD"))
	private void glintsmith$onWorldRenderStart(DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera,
			GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f frustumMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
		GlintsmithClient.onWorldRenderStart(this.renderBuffers.bufferSource());
	}

	/** Fabric: before {@code renderChunkDebugInfo} (Mojang name {@code renderDebug}), the last call of {@code render}. */
	@Inject(
		method = "renderLevel",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/LevelRenderer;renderDebug(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/Camera;)V"
		)
	)
	private void glintsmith$onWorldRenderLast(DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera,
			GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f frustumMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
		GlintsmithClient.onWorldRenderLast();
	}
}
