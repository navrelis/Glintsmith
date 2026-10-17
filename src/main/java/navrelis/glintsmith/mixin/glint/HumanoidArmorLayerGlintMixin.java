package navrelis.glintsmith.mixin.glint;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import navrelis.glintsmith.render.glint.GlintContext;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;

/**
 * G2: the glint of a worn armor piece belongs to the stack in that slot.
 */
@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerGlintMixin {
	@SuppressWarnings("rawtypes")
	@WrapMethod(method = "renderArmorPiece(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;ILnet/minecraft/client/model/HumanoidModel;)V", require = 0)
	private void glintsmith$glintContext(PoseStack pose, MultiBufferSource source, LivingEntity entity, EquipmentSlot slot,
		int light, HumanoidModel model, Operation<Void> original) {
		GlintContext.pushArmor(entity.getItemBySlot(slot));
		try {
			original.call(pose, source, entity, slot, light, model);
		} finally {
			GlintContext.pop();
		}
	}
}
