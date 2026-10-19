package navrelis.glintsmith.mixin.modelfix;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import navrelis.glintsmith.Glintsmith;
import navrelis.glintsmith.modelfix.GapFix;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.inventory.InventoryMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Scales the UV shrink vanilla applies to every baked face, on the blocks atlas only.
 */
@Mixin(TextureAtlasSprite.class)
public abstract class TextureAtlasSpriteMixin {
	@ModifyReturnValue(method = "uvShrinkRatio", at = @At("RETURN"), require = 0)
	private float glintsmith$scaleShrink(float vanilla) {
		try {
			boolean blocks = ((TextureAtlasSprite) (Object) this).atlasLocation().equals(InventoryMenu.BLOCK_ATLAS);
			return GapFix.shrinkRatio(vanilla, blocks, GapFix.effective());
		} catch (Throwable t) {
			if (!GapFix.hasFailed()) {
				Glintsmith.LOGGER.error("Model gap fix failed in uvShrinkRatio, disabling it for this session.", t);
				GapFix.markFailed();
			}
			return vanilla;
		}
	}
}
