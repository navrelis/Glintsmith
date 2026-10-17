package navrelis.glintsmith.mixin.glint;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * GA2: the vanilla main buffer source, bypassing replacements such as Iris's.
 */
@Mixin(RenderBuffers.class)
public interface RenderBuffersAccessor {
	@Accessor("bufferSource")
	MultiBufferSource.BufferSource glintsmith$getBufferSource();
}
