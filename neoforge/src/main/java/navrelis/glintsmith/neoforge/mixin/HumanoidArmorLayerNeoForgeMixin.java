package navrelis.glintsmith.neoforge.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import navrelis.glintsmith.render.glint.GlintContext;
import navrelis.glintsmith.render.outline.OutlineSystem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;

/**
 * O3 (outline) and G2 (glint) of worn armor on NeoForge. NeoForge patches {@code HumanoidArmorLayer}: {@code render}
 * calls a new overload of {@code renderArmorPiece} that takes six more floats (the head and body rotations), and the
 * vanilla six argument method is only a wrapper that nothing in the layer calls any more. The common mixins
 * ({@code HumanoidArmorLayerOutlineMixin}, {@code HumanoidArmorLayerGlintMixin}) wrap the vanilla method, so worn armor
 * got neither the outline nor the ARMOR glint layer settings. These wrap the overload that is actually used, with exactly
 * what the common ones do. The vanilla wrapper may call the overload, so both can be active in one render: the outline
 * system handles the nesting ({@code TeeBufferSource.enterNested}) and the glint context is a stack, so a second push of
 * the same stack is harmless.
 */
@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerNeoForgeMixin {
	private static final String OVERLOAD = "renderArmorPiece(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;ILnet/minecraft/client/model/HumanoidModel;FFFFFF)V";

	@WrapMethod(method = OVERLOAD, require = 0)
	private void glintsmith$outlineArmorNeoForge(PoseStack pose, MultiBufferSource source, LivingEntity entity, EquipmentSlot slot,
		int light, HumanoidModel<?> model, float a, float b, float c, float d, float e, float f, Operation<Void> original) {
		MultiBufferSource tee = OutlineSystem.beginArmor(entity, slot, pose, source);
		try {
			original.call(pose, tee, entity, slot, light, model, a, b, c, d, e, f);
		} finally {
			OutlineSystem.endArmor(tee);
		}
	}

	@WrapMethod(method = OVERLOAD, require = 0)
	private void glintsmith$glintContextNeoForge(PoseStack pose, MultiBufferSource source, LivingEntity entity, EquipmentSlot slot,
		int light, HumanoidModel<?> model, float a, float b, float c, float d, float e, float f, Operation<Void> original) {
		GlintContext.pushArmor(entity.getItemBySlot(slot));
		try {
			original.call(pose, source, entity, slot, light, model, a, b, c, d, e, f);
		} finally {
			GlintContext.pop();
		}
	}
}
