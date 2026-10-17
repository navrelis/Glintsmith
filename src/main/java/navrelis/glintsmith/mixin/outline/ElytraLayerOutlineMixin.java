package navrelis.glintsmith.mixin.outline;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import navrelis.glintsmith.render.outline.OutlineSystem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;

/**
 * O4: renders the elytra into the outline tee, styled by the chest item, as an ARMOR target.
 */
@Mixin(ElytraLayer.class)
public abstract class ElytraLayerOutlineMixin {
	@WrapMethod(
		method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
		require = 0
	)
	private void glintsmith$outlineElytra(PoseStack pose, MultiBufferSource source, int light, LivingEntity entity,
		float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch,
		Operation<Void> original) {
		MultiBufferSource tee = OutlineSystem.beginArmor(entity, EquipmentSlot.CHEST, source);
		try {
			original.call(pose, tee, light, entity, limbSwing, limbSwingAmount, partialTick, ageInTicks, netHeadYaw, headPitch);
		} finally {
			OutlineSystem.endArmor(tee);
		}
	}
}
