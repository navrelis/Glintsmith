package navrelis.glintsmith.render.glint;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * The five vanilla glint render types. The enum only holds plain metadata so it can be used without a game
 * (render types and shader shards are looked up lazily by {@link #vanilla()} and {@link #shaderShard()}).
 */
enum GlintKind {
	GLINT("glint", 8.0f, false, false, false),
	GLINT_TRANSLUCENT("glint_translucent", 8.0f, false, true, false),
	ENTITY_GLINT("entity_glint", 0.16f, true, true, false),
	ENTITY_GLINT_DIRECT("entity_glint_direct", 0.16f, true, false, false),
	ARMOR_ENTITY_GLINT("armor_entity_glint", 0.16f, true, false, true);

	static final int COUNT = values().length;

	/** Short name used in render type names. */
	final String id;
	/** Texture matrix scale of the vanilla texturing shard. */
	final float baseScale;
	/** True for the kinds that use {@code enchanted_glint_entity.png}. */
	final boolean entityFamily;
	/** Draws into the item entity target (Fabulous). */
	final boolean itemEntityTarget;
	/** Uses the view offset layering. */
	final boolean viewOffset;

	GlintKind(String id, float baseScale, boolean entityFamily, boolean itemEntityTarget, boolean viewOffset) {
		this.id = id;
		this.baseScale = baseScale;
		this.entityFamily = entityFamily;
		this.itemEntityTarget = itemEntityTarget;
		this.viewOffset = viewOffset;
	}

	/**
	 * @return the vanilla glint texture of this kind
	 */
	ResourceLocation vanillaTexture() {
		return entityFamily ? GlintTextures.VANILLA_ENTITY : GlintTextures.VANILLA_ITEM;
	}

	/**
	 * @return the vanilla shader shard of this kind (a vanilla instance, so Iris recognises the programs)
	 */
	RenderStateShard.ShaderStateShard shaderShard() {
		return switch (this) {
			case GLINT -> RenderStateShard.RENDERTYPE_GLINT_SHADER;
			case GLINT_TRANSLUCENT -> RenderStateShard.RENDERTYPE_GLINT_TRANSLUCENT_SHADER;
			case ENTITY_GLINT -> RenderStateShard.RENDERTYPE_ENTITY_GLINT_SHADER;
			case ENTITY_GLINT_DIRECT -> RenderStateShard.RENDERTYPE_ENTITY_GLINT_DIRECT_SHADER;
			case ARMOR_ENTITY_GLINT -> RenderStateShard.RENDERTYPE_ARMOR_ENTITY_GLINT_SHADER;
		};
	}

	/**
	 * @return the vanilla render type of this kind, captured once
	 */
	RenderType vanilla() {
		return switch (this) {
			case GLINT -> Vanilla.GLINT;
			case GLINT_TRANSLUCENT -> Vanilla.GLINT_TRANSLUCENT;
			case ENTITY_GLINT -> Vanilla.ENTITY_GLINT;
			case ENTITY_GLINT_DIRECT -> Vanilla.ENTITY_GLINT_DIRECT;
			case ARMOR_ENTITY_GLINT -> Vanilla.ARMOR_ENTITY_GLINT;
		};
	}

	/**
	 * @return the kind of a vanilla glint render type, or null for any other type (five identity compares)
	 */
	@Nullable
	static GlintKind of(RenderType type) {
		if (type == Vanilla.GLINT) {
			return GLINT;
		}
		if (type == Vanilla.ARMOR_ENTITY_GLINT) {
			return ARMOR_ENTITY_GLINT;
		}
		if (type == Vanilla.ENTITY_GLINT_DIRECT) {
			return ENTITY_GLINT_DIRECT;
		}
		if (type == Vanilla.GLINT_TRANSLUCENT) {
			return GLINT_TRANSLUCENT;
		}
		if (type == Vanilla.ENTITY_GLINT) {
			return ENTITY_GLINT;
		}
		return null;
	}

	/**
	 * Loads {@link Vanilla} so the vanilla instances are captured now (call from the render thread at start-up).
	 */
	static void captureVanilla() {
		Vanilla.GLINT.toString();
	}

	/** The vanilla instances, captured when this holder class is first used. */
	static final class Vanilla {
		static final RenderType GLINT = RenderType.glint();
		static final RenderType GLINT_TRANSLUCENT = RenderType.glintTranslucent();
		static final RenderType ENTITY_GLINT = RenderType.entityGlint();
		static final RenderType ENTITY_GLINT_DIRECT = RenderType.entityGlintDirect();
		static final RenderType ARMOR_ENTITY_GLINT = RenderType.armorEntityGlint();

		private Vanilla() {
		}
	}
}
