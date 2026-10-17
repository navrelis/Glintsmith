package navrelis.glintsmith.mixin.outline;

import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * A2: the shards of a composite state that decide whether a render type can be outlined (texture, glint detection,
 * invisible depth-only geometry), and its layering, which the mask copies so the item depth in the mask matches the
 * item depth in the scene.
 */
@Mixin(RenderType.CompositeState.class)
public interface CompositeStateAccessor {
	@Accessor("textureState")
	RenderStateShard.EmptyTextureStateShard glintsmith$outlineTextureState();

	@Accessor("transparencyState")
	RenderStateShard.TransparencyStateShard glintsmith$outlineTransparencyState();

	@Accessor("texturingState")
	RenderStateShard.TexturingStateShard glintsmith$outlineTexturingState();

	@Accessor("writeMaskState")
	RenderStateShard.WriteMaskStateShard glintsmith$outlineWriteMaskState();

	@Accessor("layeringState")
	RenderStateShard.LayeringStateShard glintsmith$outlineLayeringState();
}
