package navrelis.glintsmith.mixin.glint;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import navrelis.glintsmith.render.glint.GlintContext;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

/**
 * G4: glints drawn by item renderers without a model (shield, trident, banners, mod renderers) use the entity category.
 * Fabric's builtin item renderers run inside this method, so they are covered too.
 */
@Mixin(BlockEntityWithoutLevelRenderer.class)
public abstract class BlockEntityWithoutLevelRendererGlintMixin {
	@WrapMethod(method = "renderByItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V", require = 0)
	private void glintsmith$glintContext(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource source,
		int light, int overlay, Operation<Void> original) {
		GlintContext.pushBewlr(stack, context);
		try {
			original.call(stack, context, pose, source, light, overlay);
		} finally {
			GlintContext.pop();
		}
	}
}
