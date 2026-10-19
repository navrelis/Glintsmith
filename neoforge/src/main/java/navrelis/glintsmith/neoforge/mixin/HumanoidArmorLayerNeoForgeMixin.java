package navrelis.glintsmith.neoforge.mixin;

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
 * O3 on NeoForge. NeoForge patches {@code HumanoidArmorLayer}: {@code render} calls a new overload of
 * {@code renderArmorPiece} that takes six more floats (the head and body rotations), and the vanilla six argument method
 * is only a wrapper that nothing in the layer calls any more. The common mixin
 * ({@code HumanoidArmorLayerOutlineMixin}) wraps the vanilla method, so worn armor got no outline. This wraps the
 * overload that is actually used. The wrapper may call it, so both can be active in one render; the outline system
 * handles the nesting ({@code TeeBufferSource.enterNested}).
 */
@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerNeoForgeMixin {
	@WrapMethod(
		method = "renderArmorPiece(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;ILnet/minecraft/client/model/HumanoidModel;FFFFFF)V",
		require = 0
	)
	private void glintsmith$outlineArmorNeoForge(PoseStack pose, MultiBufferSource source, LivingEntity entity, EquipmentSlot slot,
		int light, HumanoidModel<?> model, float a, float b, float c, float d, float e, float f, Operation<Void> original) {
		MultiBufferSource tee = OutlineSystem.beginArmor(entity, slot, pose, source);
		try {
			original.call(pose, tee, entity, slot, light, model, a, b, c, d, e, f);
		} finally {
			OutlineSystem.endArmor(tee);
		}
	}
}
