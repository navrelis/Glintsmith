package navrelis.glintsmith.render.glint;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

/**
 * Builds the pooled glint render types. They clone the state of the vanilla glint types (including the vanilla
 * {@code GLINT_TRANSPARENCY} instance and the vanilla shader shard, which keeps shader packs and the outline system
 * working) and replace the texture and texturing shards with ones that read the slot's params at draw time.
 */
final class GlintRenderTypes {
	static final GlintSlotPool.Factory<RenderType> FACTORY = new GlintSlotPool.Factory<>() {
		@Override
		public RenderType createSlotType(GlintSlot<RenderType> slot) {
			GlintKind kind = slot.kind;
			RenderType.CompositeState.CompositeStateBuilder builder = base(kind)
				.setTextureState(new RenderStateShard.EmptyTextureStateShard(slot::setupTexture, () -> {}))
				.setWriteMaskState(RenderStateShard.COLOR_WRITE)
				.setTexturingState(new RenderStateShard.TexturingStateShard(
					"glintsmith_glint_texturing", slot::setupTexturing, slot::clearTexturing));
			return RenderType.create("glintsmith_" + kind.id + "_" + slot.index, DefaultVertexFormat.POSITION_TEX,
				VertexFormat.Mode.QUADS, 1536, false, false, builder.createCompositeState(false));
		}

		@Override
		public RenderType createOffType(GlintSlot<RenderType> slot) {
			GlintKind kind = slot.kind;
			RenderType.CompositeState.CompositeStateBuilder builder = base(kind)
				.setTextureState(new RenderStateShard.TextureStateShard(kind.vanillaTexture(), true, false))
				.setWriteMaskState(new RenderStateShard.WriteMaskStateShard(false, false));
			return RenderType.create("glintsmith_" + kind.id + "_off", DefaultVertexFormat.POSITION_TEX,
				VertexFormat.Mode.QUADS, 1536, false, false, builder.createCompositeState(false));
		}
	};

	private GlintRenderTypes() {
	}

	private static RenderType.CompositeState.CompositeStateBuilder base(GlintKind kind) {
		RenderType.CompositeState.CompositeStateBuilder builder = RenderType.CompositeState.builder()
			.setShaderState(kind.shaderShard())
			.setCullState(RenderStateShard.NO_CULL)
			.setDepthTestState(RenderStateShard.EQUAL_DEPTH_TEST)
			.setTransparencyState(RenderStateShard.GLINT_TRANSPARENCY);
		if (kind.itemEntityTarget) {
			builder.setOutputState(RenderStateShard.ITEM_ENTITY_TARGET);
		}
		if (kind.viewOffset) {
			builder.setLayeringState(RenderStateShard.VIEW_OFFSET_Z_LAYERING);
		}
		return builder;
	}
}
