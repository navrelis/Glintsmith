package navrelis.glintsmith.mixin.modelfix;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import java.util.List;
import java.util.Map;
import navrelis.glintsmith.Glintsmith;
import navrelis.glintsmith.modelfix.GapFix;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.ItemModelGenerator;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Closes the cracks next to the front and back faces of generated item models.
 *
 * <p>The precise span handling lives in {@link ItemModelGeneratorSpanMixin}.
 */
@Mixin(ItemModelGenerator.class)
public abstract class ItemModelGeneratorMixin {
	/** Nudges every single-face side element inwards and/or outwards, in place. */
	@ModifyReturnValue(method = "createSideElements", at = @At("RETURN"), require = 0)
	private List<BlockElement> glintsmith$adjustSideElements(List<BlockElement> elements) {
		try {
			GapFix.Settings settings = GapFix.effective();
			if (!settings.enabled() || (settings.sideExpansion() == 0.0F && settings.sideIndent() == 0.0F)) {
				return elements;
			}
			for (BlockElement element : elements) {
				Map<Direction, BlockElementFace> faces = element.faces;
				if (faces.size() != 1) {
					continue;
				}
				Direction face = faces.keySet().iterator().next();
				GapFix.adjustSideElement(face, element.from, element.to, settings.sideExpansion(), settings.sideIndent());
			}
		} catch (Throwable t) {
			if (!GapFix.hasFailed()) {
				Glintsmith.LOGGER.error("Model gap fix failed while generating an item model, disabling it for this session.", t);
				GapFix.markFailed();
			}
		}
		return elements;
	}
}
