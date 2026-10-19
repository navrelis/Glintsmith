package navrelis.glintsmith.mixin.glint;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import navrelis.glintsmith.render.glint.GlintContext;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

/**
 * G1: tells the glint system which stack and display context the glint types requested below belong to.
 */
@Mixin(ItemRenderer.class)
public abstract class ItemRendererGlintMixin {
	@WrapMethod(method = "render(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/resources/model/BakedModel;)V", require = 0)
	private void glintsmith$glintContext(ItemStack stack, ItemDisplayContext context, boolean leftHand, PoseStack pose,
		MultiBufferSource source, int light, int overlay, BakedModel model, Operation<Void> original) {
		GlintContext.pushItem(stack, context);
		try {
			original.call(stack, context, leftHand, pose, source, light, overlay, model);
		} finally {
			GlintContext.pop();
		}
	}
}
