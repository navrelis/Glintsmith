package navrelis.glintsmith.forge.mixin;

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
 * Fires the world render start and last hooks at the same points as the Fabric API events
 * {@code WorldRenderEvents.START} and {@code WorldRenderEvents.LAST} (fabric-rendering-v1 {@code WorldRendererMixin},
 * translated to Mojang names). Forge's RenderLevelStageEvent has no stage at the start of the pass and none at the
 * position of LAST.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererWorldEventsMixin {
	@Final
	@Shadow
	private RenderBuffers renderBuffers;

	/** Fabric: HEAD of "render" (Mojang: renderLevel); the consumers are the main buffer source of the render buffers. */
	@Inject(method = "renderLevel", at = @At("HEAD"), require = 1)
	private void glintsmith$worldStart(DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer,
		LightTexture lightTexture, Matrix4f frustumMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
		GlintsmithClient.onWorldRenderStart(renderBuffers.bufferSource());
	}

	/** Fabric: before the call of "renderChunkDebugInfo" (Mojang: renderDebug) in "render". */
	@Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;renderDebug(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/Camera;)V"), require = 1)
	private void glintsmith$worldLast(CallbackInfo ci) {
		GlintsmithClient.onWorldRenderLast();
	}
}
