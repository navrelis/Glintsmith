package navrelis.glintsmith.mixin.outline;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import navrelis.glintsmith.render.outline.OutlineSystem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;

/**
 * O3: renders one armor piece into the outline tee, styled by the item in that slot. The wrap also covers Fabric's
 * {@code ArmorRenderer} hook at the head of the method, so modded armor renderers are captured too.
 */
@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerOutlineMixin {
	@WrapMethod(
		method = "renderArmorPiece(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;ILnet/minecraft/client/model/HumanoidModel;)V",
		require = 0
	)
	private void glintsmith$outlineArmor(PoseStack pose, MultiBufferSource source, LivingEntity entity, EquipmentSlot slot,
		int light, HumanoidModel<?> model, Operation<Void> original) {
		MultiBufferSource tee = OutlineSystem.beginArmor(entity, slot, source);
		try {
			original.call(pose, tee, entity, slot, light, model);
		} finally {
			OutlineSystem.endArmor(tee);
		}
	}
}
