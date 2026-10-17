package navrelis.glintsmith.mixin.outline;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import navrelis.glintsmith.render.outline.OutlineSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;

/**
 * O5: one GUI phase per item. Every public {@code renderItem} / {@code renderFakeItem} overload ends in this private
 * method, which renders the item and flushes it; the outline is composited right after.
 */
@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsOutlineMixin {
	@WrapMethod(
		method = "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;IIII)V",
		require = 0
	)
	private void glintsmith$outlineGuiItem(LivingEntity entity, Level level, ItemStack stack, int x, int y, int seed, int z,
		Operation<Void> original) {
		OutlineSystem.beginGuiItem();
		try {
			original.call(entity, level, stack, x, y, seed, z);
		} finally {
			OutlineSystem.endGuiItem();
		}
	}
}
