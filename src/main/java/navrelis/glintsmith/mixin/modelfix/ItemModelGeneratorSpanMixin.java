package navrelis.glintsmith.mixin.modelfix;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import navrelis.glintsmith.Glintsmith;
import navrelis.glintsmith.modelfix.GapFix;
import navrelis.glintsmith.modelfix.SpanAccess;
import net.minecraft.client.renderer.block.model.ItemModelGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

/**
 * Precise side spans for generated item models.
 *
 * <p>Kept apart from {@link ItemModelGeneratorMixin} because the mod 'modelfix' overwrites {@code createOrExpandSpan}.
 * Mixin cannot inject into a method another mixin merged, so {@code GlintsmithMixinPlugin} leaves this class out when
 * that mod is installed.
 */
@Mixin(ItemModelGenerator.class)
public abstract class ItemModelGeneratorSpanMixin {
	/**
	 * Vanilla merges every edge pixel of a row or column into the first span with the same anchor, even across
	 * transparent gaps. With precise spans only a span that touches or contains the new pixel counts as a match;
	 * otherwise vanilla's own code starts a new span. With the setting off the original anchor is returned untouched.
	 */
	@WrapOperation(
		method = "createOrExpandSpan",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/model/ItemModelGenerator$Span;getAnchor()I"),
		require = 0
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
			if (!GapFix.hasFailed()) {
				Glintsmith.LOGGER.error("Model gap fix failed while generating an item model, disabling it for this session.", t);
				GapFix.markFailed();
			}
			return anchor;
		}
	}
}
