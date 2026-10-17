package navrelis.glintsmith.mixin.modelfix;

import navrelis.glintsmith.modelfix.SpanAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Exposes the bounds of the package-private {@code ItemModelGenerator.Span}.
 */
@Mixin(targets = "net.minecraft.client.renderer.block.model.ItemModelGenerator$Span")
public abstract class SpanMixin implements SpanAccess {
	@Shadow
	public abstract int getMin();

	@Shadow
	public abstract int getMax();

	@Override
	public int glintsmith$min() {
		return this.getMin();
	}

	@Override
	public int glintsmith$max() {
		return this.getMax();
	}
}
