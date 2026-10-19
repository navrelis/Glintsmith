package navrelis.glintsmith.mixin.glint;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import navrelis.glintsmith.render.glint.GlintContext;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;

/**
 * G8: marks the first-person hand pass, so that held items drawn inside it (also those that first-person animation
 * mods draw with a third-person display context) are classified as first person. Kept apart from the outline hook on
 * purpose: the glint must not depend on outlines being enabled.
 */
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererGlintMixin {
	@WrapMethod(
		method = "renderHandsWithItems(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/player/LocalPlayer;I)V",
		require = 0
	)
	private void glintsmith$glintHands(float partialTick, PoseStack pose, MultiBufferSource.BufferSource source,
		LocalPlayer player, int light, Operation<Void> original) {
		GlintContext.beginHands();
		try {
			original.call(partialTick, pose, source, player, light);
		} finally {
			GlintContext.endHands();
		}
	}
}
