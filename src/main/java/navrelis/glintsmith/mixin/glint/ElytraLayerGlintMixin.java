package navrelis.glintsmith.mixin.glint;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import navrelis.glintsmith.render.glint.GlintContext;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;

/**
 * G3: the glint of the elytra belongs to the stack in the chest slot.
 */
@Mixin(ElytraLayer.class)
public abstract class ElytraLayerGlintMixin {
	@WrapMethod(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V", require = 0)
	private void glintsmith$glintContext(PoseStack pose, MultiBufferSource source, int light, LivingEntity entity,
		float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch,
		Operation<Void> original) {
		GlintContext.pushArmor(entity.getItemBySlot(EquipmentSlot.CHEST));
		try {
			original.call(pose, source, light, entity, limbSwing, limbSwingAmount, partialTick, ageInTicks, netHeadYaw, headPitch);
		} finally {
			GlintContext.pop();
		}
	}
}
