package navrelis.glintsmith.mixin.outline;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import navrelis.glintsmith.render.outline.OutlineSystem;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

/**
 * O2: remembers who holds the item being drawn (O2a, for "only local player") and opens the first-person HAND phase
 * around {@code renderHandsWithItems} (O2b). Under a shader pack O2b hands the wrapped body (vanilla, and a
 * first-person animation mod's code injected into it, such as Punchy's arm renderer at HEAD) a buffer source that
 * records the arm geometry as occluders for the outline.
 */
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererOutlineMixin {
	@WrapMethod(
		method = "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
		require = 0
	)
	private void glintsmith$outlineHolder(LivingEntity holder, ItemStack stack, ItemDisplayContext context, boolean leftHand,
		PoseStack pose, MultiBufferSource source, int light, Operation<Void> original) {
		OutlineSystem.pushHolder(holder);
		try {
			original.call(holder, stack, context, leftHand, pose, source, light);
		} finally {
			OutlineSystem.popHolder();
		}
	}

	@WrapMethod(
		method = "renderHandsWithItems(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/player/LocalPlayer;I)V",
		require = 0
	)
	private void glintsmith$outlineHands(float partialTick, PoseStack pose, MultiBufferSource.BufferSource source,
		LocalPlayer player, int light, Operation<Void> original) {
		// Under a shader pack the hands get a source that also records their geometry as outline occluders.
		MultiBufferSource.BufferSource hands = OutlineSystem.beginHands(source);
		try {
			original.call(partialTick, pose, hands, player, light);
		} finally {
			OutlineSystem.endHands();
		}
	}
}
