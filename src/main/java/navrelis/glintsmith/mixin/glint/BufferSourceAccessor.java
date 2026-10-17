package navrelis.glintsmith.mixin.glint;

import java.util.SequencedMap;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * GA1: the fixed buffers of a buffer source, to register the glint slot types.
 */
@Mixin(MultiBufferSource.BufferSource.class)
public interface BufferSourceAccessor {
	@Accessor("fixedBuffers")
	SequencedMap<RenderType, ByteBufferBuilder> glintsmith$getFixedBuffers();
}
