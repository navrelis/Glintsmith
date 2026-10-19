package navrelis.glintsmith.mixin.glint.compat;

import navrelis.glintsmith.render.glint.GlintSystem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * G12: the glint swap for ImmediatelyFast's {@code HudBatchingBufferSource}. While a HUD layer is batched (and with its
 * experimental screen batching) ImmediatelyFast puts that source into {@code GuiGraphics}, so the hotbar and other HUD
 * items request their glint from it, not from the main source. It extends {@code BatchableBufferSource} and overrides
 * {@code getBuffer}; the swap runs at its HEAD, before its own checks: render types whose name contains "glint" (the
 * vanilla glint types and ours, all named {@code glintsmith_*glint*}) skip its per-lighting wrapping and go straight to
 * the parent, where G9 sees a type that is no longer a vanilla glint and leaves it alone.
 * {@link GlintSystem#substituteGui} registers the slot type as a fixed buffer of this source; nothing here draws, so
 * the HUD batch is still drawn only by ImmediatelyFast itself (end of the layer or its force-draw).
 * <p>
 * Optional and not remapped, as G9 (verified in the ImmediatelyFast 1.6.15+1.21.1 jar:
 * {@code public class_4588 getBuffer(class_1921)}, the only {@code getBuffer} of the class).
 */
@Pseudo
@Mixin(targets = "net.raphimc.immediatelyfast.feature.batching.HudBatchingBufferSource", remap = false)
public abstract class ImmediatelyFastHudGlintMixin {
	@ModifyVariable(method = "getBuffer", at = @At("HEAD"), argsOnly = true, require = 0, remap = false)
	private RenderType glintsmith$swapGlint(RenderType requested) {
		return GlintSystem.substituteGui((MultiBufferSource.BufferSource) (Object) this, requested);
	}
}
