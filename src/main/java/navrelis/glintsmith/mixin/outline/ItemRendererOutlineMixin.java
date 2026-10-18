package navrelis.glintsmith.mixin.outline;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import navrelis.glintsmith.render.outline.OutlineSystem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

/**
 * O1: renders a qualifying item into the outline tee instead of the real buffer source. Covers held, dropped,
 * framed, worn-on-head and GUI items.
 */
@Mixin(ItemRenderer.class)
public abstract class ItemRendererOutlineMixin {
	@WrapMethod(
		method = "render(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/resources/model/BakedModel;)V",
		require = 0
	)
	private void glintsmith$outlineItem(ItemStack stack, ItemDisplayContext context, boolean leftHand, PoseStack pose,
		MultiBufferSource source, int light, int overlay, BakedModel model, Operation<Void> original) {
		MultiBufferSource tee = OutlineSystem.beginItem(stack, context, leftHand, pose, model, source);
		try {
			original.call(stack, context, leftHand, pose, tee, light, overlay, model);
		} finally {
			OutlineSystem.endItem(tee);
		}
	}
}
