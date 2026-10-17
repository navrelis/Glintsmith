package navrelis.glintsmith.mixin.outline;

import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * A1: reads the composite state of a render type, so the outline mask can reuse its texture.
 */
@Mixin(RenderType.CompositeRenderType.class)
public interface CompositeRenderTypeAccessor {
	@Invoker("state")
	RenderType.CompositeState glintsmith$outlineState();
}
