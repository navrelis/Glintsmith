package navrelis.glintsmith.mixin.outline;

import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * A2: the shards of a composite state that decide whether a render type can be outlined (texture, glint detection,
 * invisible depth-only geometry), and its layering, which the mask copies so the item depth in the mask matches the
 * item depth in the scene. The depth test and the write mask also tell whether the type's geometry hides what lies
 * behind it (first-person occluders under a shader pack). The cull state is copied too: a mask that drew back faces
 * the source culls would seed outlines on pixels the item never covers.
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

	@Accessor("depthTestState")
	RenderStateShard.DepthTestStateShard glintsmith$outlineDepthTestState();

	@Accessor("cullState")
	RenderStateShard.CullStateShard glintsmith$outlineCullState();
}
