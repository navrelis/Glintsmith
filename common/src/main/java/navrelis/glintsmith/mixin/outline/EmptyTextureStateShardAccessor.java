package navrelis.glintsmith.mixin.outline;

import java.util.Optional;

import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * A3: the texture a texture shard binds, if it is a plain texture (subclasses override {@code cutoutTexture}).
 */
@Mixin(RenderStateShard.EmptyTextureStateShard.class)
public interface EmptyTextureStateShardAccessor {
	@Invoker("cutoutTexture")
	Optional<ResourceLocation> glintsmith$outlineCutoutTexture();
}
