package navrelis.glintsmith.render.outline;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import navrelis.glintsmith.mixin.outline.CompositeRenderTypeAccessor;
import navrelis.glintsmith.mixin.outline.CompositeStateAccessor;
import navrelis.glintsmith.mixin.outline.EmptyTextureStateShardAccessor;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Decides which render types can be captured into the outline mask and builds the matching mask render types.
 * <p>
 * A source type is maskable when it is a composite type with a known texture (or no UV at all), quads or triangles,
 * draws colour, and is not a glint or outline type. The decision is made once per type and cached by identity.
 * <p>
 * Every source type gets its own mask types (one depth-tested, one flat), never shared with another source type even
 * if the texture is the same. Otherwise two source buffers used at the same time (for example through a
 * {@code VertexMultiConsumer}) would interleave their vertices in one mask buffer and break its primitives.
 * <p>
 * The depth-tested type does not test against the scene: it is drawn into a mask depth cleared to the far plane, with
 * depth writes, so the mask keeps every item pixel (also hidden ones) with the depth of the nearest item surface
 * (packed into the colour by {@code mask.fsh}). It repeats the source's layering, so that depth equals the item's
 * depth in the scene and the passes can tell visible from hidden item pixels ({@link OutlineDepth}). The flat type
 * (GUI) has no depth test and no depth write.
 * Render thread only.
 */
final class MaskTypes {
	/** 1x1 white texture used for source formats without UV0; registered by {@link OutlineSystem}. */
	static final ResourceLocation WHITE = ResourceLocation.fromNamespaceAndPath("glintsmith", "outline/white");
	static final String MASK_NAME = "glintsmith_outline_mask";
	/** The identity cache is dropped above this size (protects against mods creating types every frame). */
	static final int MAX_CACHED_SOURCES = 4096;

	private static final RenderStateShard.ShaderStateShard MASK_SHADER = new RenderStateShard.ShaderStateShard(OutlineShaders::mask);
	private static final RenderStateShard.OutputStateShard MASK_TARGET = new RenderStateShard.OutputStateShard(
		"glintsmith_outline_mask_target", OutlineTargets::bindMaskForWrite, () -> {
		});

	private static final Map<RenderType, Entry> SOURCES = new IdentityHashMap<>();

	private MaskTypes() {
	}

	/**
	 * What the mask needs to know about one source type.
	 */
	static final class Entry {
		static final Entry NOT_MASKABLE = new Entry(null, null, false, RenderStateShard.NO_LAYERING);

		final ResourceLocation texture;
		final VertexFormat.Mode mode;
		/** The source format has no UV0: the mask writes a fixed UV into the white texture. */
		final boolean defaultUv;
		/** The source's layering, repeated by the depth-tested mask type (for example the armor's view offset). */
		final RenderStateShard.LayeringStateShard layering;
		private RenderType depthTested;
		private RenderType flat;

		Entry(ResourceLocation texture, VertexFormat.Mode mode, boolean defaultUv, RenderStateShard.LayeringStateShard layering) {
			this.texture = texture;
			this.mode = mode;
			this.defaultUv = defaultUv;
			this.layering = layering == null ? RenderStateShard.NO_LAYERING : layering;
		}

		boolean maskable() {
			return texture != null;
		}

		/**
		 * @return the mask type for this source, or null if the source is not maskable
		 */
		@Nullable
		RenderType maskType(boolean depth) {
			if (texture == null) {
				return null;
			}
			RenderType type = depth ? depthTested : flat;
			if (type == null) {
				type = create(texture, mode, depth, layering);
				if (depth) {
					depthTested = type;
				} else {
					flat = type;
				}
			}
			return type;
		}
	}

	/**
	 * @return the cached decision for a source type; never null
	 */
	static Entry lookup(RenderType source) {
		Entry entry = SOURCES.get(source);
		if (entry == null) {
			if (SOURCES.size() >= MAX_CACHED_SOURCES) {
				SOURCES.clear();
			}
			entry = decide(source);
			SOURCES.put(source, entry);
		}
		return entry;
	}

	/**
	 * @return the mask type for a source type, or null if it cannot be outlined
	 */
	@Nullable
	static RenderType forSource(RenderType source, boolean depthTested) {
		return lookup(source).maskType(depthTested);
	}

	/**
	 * @return whether the source format has no UV0, so the mask writes a default UV
	 */
	static boolean needsDefaultUv(RenderType source) {
		return lookup(source).defaultUv;
	}

	/**
	 * Drops every cached decision together with its mask types.
	 */
	static void clearCaches() {
		SOURCES.clear();
	}

	/** Number of cached source decisions. For tests. */
	static int cachedSourceCount() {
		return SOURCES.size();
	}

	private static Entry decide(RenderType source) {
		if (!(source instanceof RenderType.CompositeRenderType composite)) {
			return Entry.NOT_MASKABLE;
		}
		VertexFormat format = source.format();
		if (!shapeEligible(source.isOutline(), source.mode(), format.contains(VertexFormatElement.POSITION))) {
			return Entry.NOT_MASKABLE;
		}
		RenderType.CompositeState state = ((CompositeRenderTypeAccessor) (Object) composite).glintsmith$outlineState();
		CompositeStateAccessor shards = (CompositeStateAccessor) (Object) state;
		if (isGlint(shards.glintsmith$outlineTransparencyState(), shards.glintsmith$outlineTexturingState(), nameOf(source.toString()))) {
			return Entry.NOT_MASKABLE;
		}
		if (shards.glintsmith$outlineWriteMaskState() == RenderStateShard.DEPTH_WRITE) {
			return Entry.NOT_MASKABLE; // invisible geometry such as the water mask
		}
		boolean hasUv = format.contains(VertexFormatElement.UV0);
		RenderStateShard.EmptyTextureStateShard textureShard = shards.glintsmith$outlineTextureState();
		Optional<ResourceLocation> texture = textureShard == null
			? Optional.empty()
			: ((EmptyTextureStateShardAccessor) textureShard).glintsmith$outlineCutoutTexture();
		ResourceLocation chosen = textureFor(hasUv, texture);
		if (chosen == null) {
			return Entry.NOT_MASKABLE;
		}
		return new Entry(chosen, source.mode(), !hasUv, shards.glintsmith$outlineLayeringState());
	}

	/**
	 * Geometry check: no outline types, only filled primitives that the mask can draw, a position element.
	 */
	static boolean shapeEligible(boolean isOutline, VertexFormat.Mode mode, boolean hasPosition) {
		return !isOutline && hasPosition && (mode == VertexFormat.Mode.QUADS || mode == VertexFormat.Mode.TRIANGLES);
	}

	/**
	 * Every glint is excluded: vanilla's, mods' and Glintsmith's own slots all use the glint transparency or a glint
	 * texturing shard, or carry "glint" in their name.
	 */
	static boolean isGlint(RenderStateShard.TransparencyStateShard transparency, RenderStateShard.TexturingStateShard texturing,
		String name) {
		if (transparency == RenderStateShard.GLINT_TRANSPARENCY) {
			return true;
		}
		if (texturing == RenderStateShard.GLINT_TEXTURING || texturing == RenderStateShard.ENTITY_GLINT_TEXTURING) {
			return true;
		}
		return name != null && name.contains("glint");
	}

	/**
	 * The texture the mask samples: the source texture, the white texture for formats without UV0, or null when a
	 * textured format has no known texture (a quad-shaped outline would be wrong).
	 */
	@Nullable
	static ResourceLocation textureFor(boolean hasUv, Optional<ResourceLocation> texture) {
		if (!hasUv) {
			return WHITE;
		}
		return texture.orElse(null);
	}

	/**
	 * The name part of {@code "RenderType[name:state]"}; the whole string if it has another shape.
	 */
	static String nameOf(String typeString) {
		if (typeString == null) {
			return "";
		}
		String prefix = "RenderType[";
		if (typeString.startsWith(prefix)) {
			int end = typeString.indexOf(':', prefix.length());
			if (end > 0) {
				return typeString.substring(prefix.length(), end);
			}
		}
		return typeString;
	}

	private static RenderType create(ResourceLocation texture, VertexFormat.Mode mode, boolean depth,
		RenderStateShard.LayeringStateShard layering) {
		// Binds the texture without touching its filter, unlike TextureStateShard.
		RenderStateShard.EmptyTextureStateShard textureShard = new RenderStateShard.EmptyTextureStateShard(
			() -> RenderSystem.setShaderTexture(0, texture), () -> {
			});
		return RenderType.create(MASK_NAME, DefaultVertexFormat.POSITION_TEX_COLOR, mode, 1536, false, false,
			RenderType.CompositeState.builder()
				.setShaderState(MASK_SHADER)
				.setTextureState(textureShard)
				.setTransparencyState(RenderStateShard.NO_TRANSPARENCY)
				.setCullState(RenderStateShard.NO_CULL)
				.setDepthTestState(depth ? RenderStateShard.LEQUAL_DEPTH_TEST : RenderStateShard.NO_DEPTH_TEST)
				.setLayeringState(depth ? layering : RenderStateShard.NO_LAYERING)
				.setWriteMaskState(depth ? RenderStateShard.COLOR_DEPTH_WRITE : RenderStateShard.COLOR_WRITE)
				.setOutputState(MASK_TARGET)
				.createCompositeState(false));
	}
}
