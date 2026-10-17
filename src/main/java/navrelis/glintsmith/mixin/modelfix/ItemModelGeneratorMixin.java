package navrelis.glintsmith.mixin.modelfix;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import java.util.List;
import java.util.Map;
import navrelis.glintsmith.Glintsmith;
import navrelis.glintsmith.modelfix.GapFix;
import navrelis.glintsmith.modelfix.SpanAccess;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.ItemModelGenerator;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

/**
 * Closes the cracks next to the front and back faces of generated item models.
 */
@Mixin(ItemModelGenerator.class)
public abstract class ItemModelGeneratorMixin {
	/** Nudges every single-face side element inwards and/or outwards, in place. */
	@ModifyReturnValue(method = "createSideElements", at = @At("RETURN"))
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
			glintsmith$fail(t);
		}
		return elements;
	}

	/**
	 * Vanilla merges every edge pixel of a row or column into the first span with the same anchor, even across
	 * transparent gaps. With precise spans only a span that touches or contains the new pixel counts as a match;
	 * otherwise vanilla's own code starts a new span. With the setting off the original anchor is returned untouched.
	 */
	@WrapOperation(
		method = "createOrExpandSpan",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/model/ItemModelGenerator$Span;getAnchor()I")
	)
	private int glintsmith$matchContiguousSpan(
		@Coerce Object span,
		Operation<Integer> original,
		@Local(argsOnly = true, ordinal = 0) int i,
		@Local(argsOnly = true, ordinal = 1) int j
	) {
		int anchor = original.call(span);
		try {
			GapFix.Settings settings = GapFix.effective();
			if (!settings.enabled() || !settings.preciseSpans() || settings.sideExpansion() == 0.0F) {
				return anchor;
			}
			// The anchor is j for UP/DOWN spans (new pixel i) and i for LEFT/RIGHT spans (new pixel j).
			int pixel = anchor == j ? i : j;
			SpanAccess bounds = (SpanAccess) span;
			if (pixel >= bounds.glintsmith$min() - 1 && pixel <= bounds.glintsmith$max() + 1) {
				return anchor;
			}
			return -1;
		} catch (Throwable t) {
			glintsmith$fail(t);
			return anchor;
		}
	}

	@Unique
	private static void glintsmith$fail(Throwable t) {
		if (!GapFix.hasFailed()) {
			Glintsmith.LOGGER.error("Model gap fix failed while generating an item model, disabling it for this session.", t);
			GapFix.markFailed();
		}
	}
}
