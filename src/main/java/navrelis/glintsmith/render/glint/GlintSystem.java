package navrelis.glintsmith.render.glint;

import java.util.SequencedMap;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import navrelis.glintsmith.Glintsmith;
import navrelis.glintsmith.config.ConfigManager;
import navrelis.glintsmith.config.GlintsmithConfig;
import navrelis.glintsmith.mixin.glint.BufferSourceAccessor;
import navrelis.glintsmith.mixin.glint.RenderBuffersAccessor;
import navrelis.glintsmith.render.outline.compat.IrisCompat;
import navrelis.glintsmith.style.GlintCategory;
import navrelis.glintsmith.style.GlintLook;
import navrelis.glintsmith.style.ResolvedStyle;
import navrelis.glintsmith.style.RuleEngine;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Static facade of the glint system (render thread only). The mixins call it; nothing here throws: any failure trips
 * {@link GlintFailsafe} and the vanilla glint is drawn from then on.
 * <p>
 * How it works: the five vanilla glint render types requested from the main buffer source are swapped for pooled
 * "slot" render types ({@link GlintSlotPool}) whose texture, texture matrix, tint and strength are read at draw time.
 * Slots are registered as fixed buffers of the main source, flushed right after the vanilla glint, and released when
 * their batch has been drawn.
 * <p>
 * With Iris installed, the world pass requests its glint from Iris's fully buffered source instead of the main one;
 * {@link #substituteBuffered} (G7) swaps the types there, with pinned slots that live until the next frame.
 * <p>
 * With ImmediatelyFast installed, the main source itself is ImmediatelyFast's {@code BatchableBufferSource}, which
 * overrides {@code getBuffer} and {@code endBatch(RenderType)} without calling the vanilla bodies: G9 runs
 * {@link #substitute} from its {@code getBuffer}, and {@link #onBatchDrawn} does the G5b flush for it. The GUI and HUD
 * draw into two other ImmediatelyFast sources, which {@link #substituteGui} (G11, G12) covers.
 * <p>
 * Customised glints drawn with block-atlas UVs get the atlas factor of {@link GlintAtlasScale}, read from the block
 * atlas after every resource reload (G10).
 */
public final class GlintSystem {
	private static final ResourceLocation RELOAD_ID = ResourceLocation.fromNamespaceAndPath(Glintsmith.MOD_ID, "glint_textures");

	private static final GlintSlotPool<RenderType> POOL = new GlintSlotPool<>(GlintRenderTypes.FACTORY);
	private static final GlintParams SCRATCH = new GlintParams();

	private static volatile GlintRuntimeSettings settings = GlintRuntimeSettings.VANILLA;
	@Nullable
	private static MultiBufferSource.BufferSource main;
	/** ImmediatelyFast's non-batching GUI source (a plain vanilla {@code BufferSource}), captured by G11; null without it. */
	@Nullable
	private static MultiBufferSource.BufferSource guiSource;
	private static boolean initialized;
	private static boolean started;
	private static long frame;
	private static double timeSeconds;
	/**
	 * A shader pack was in use at the start of this frame (G7 stays off then). True until G6 has run once: the safe
	 * side, so G7 never runs under a pack if G6 is missing.
	 */
	private static boolean shaderPack = true;

	private GlintSystem() {
	}

	/**
	 * Registers the events, the resource reload listener and the config listener. Call once from
	 * {@code onInitializeClient}, after the config manager is available. Never throws.
	 */
	public static void init() {
		try {
			if (initialized) {
				return;
			}
			initialized = true;
			settings = GlintRuntimeSettings.from(ConfigManager.current());
			ClientLifecycleEvents.CLIENT_STARTED.register(GlintSystem::onClientStarted);
			ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new TextureReloadListener());
			ConfigManager.instance().addListener(GlintSystem::onConfigChanged);
		} catch (Throwable t) {
			GlintFailsafe.trip("init", t);
		}
	}

	// ---------------------------------------------------------------- mixin entry points

	/**
	 * G5a: the render type to use instead of {@code requested} when it is requested from {@code source}.
	 *
	 * @return {@code requested} unless it is a vanilla glint type of the main source that has a custom look
	 */
	public static RenderType substitute(MultiBufferSource.BufferSource source, RenderType requested) {
		try {
			GlintRuntimeSettings s = settings;
			if (!s.anyCustom() || source != main || GlintFailsafe.tripped()) {
				return requested;
			}
			GlintKind kind = GlintKind.of(requested);
			if (kind == null || !RenderSystem.isOnRenderThread()) {
				return requested;
			}
			return pick(s, kind, requested, false);
		} catch (Throwable t) {
			GlintFailsafe.trip("substitute", t);
			return requested;
		}
	}

	/**
	 * G7: the render type to use instead of {@code requested} when it is requested from Iris's
	 * {@code FullyBufferedMultiBufferSource}. Iris installs that source as {@code RenderBuffers.bufferSource()} for the
	 * whole of {@code LevelRenderer.renderLevel} whenever Iris is installed, shaders on or off, and its
	 * {@code getBuffer} never reaches the vanilla {@code BufferSource.getBuffer} (G5a). Without this hook every glint
	 * drawn in the world (armor, held items of entities, item frames, dropped items) keeps the vanilla look.
	 * <p>
	 * Why the {@code source == main} rule of {@link #substitute} (DESIGN 5.2, point 2) is not needed here: that rule
	 * exists because a vanilla {@code BufferSource} holds non-fixed types in one shared builder and flushes it whenever
	 * another non-fixed type is requested, which would draw our glint before its base geometry or close a builder that
	 * is still written. Iris's source keeps a separate segment per render type for the whole level pass, its
	 * {@code endBatch(RenderType)} does nothing, and it draws only in {@code endBatch()} / {@code endBatchWithType},
	 * ordered by transparency class: our slot types reuse the vanilla {@code GLINT_TRANSPARENCY} shard, so Iris
	 * classifies them as {@code DECAL} like the vanilla glint and draws them after the {@code OPAQUE},
	 * {@code OPAQUE_DECAL} and {@code GENERAL_TRANSPARENT} base layers, exactly where it draws the vanilla glint.
	 * <p>
	 * Slot lifetime: Iris draws through {@code RenderType.draw(MeshData)}, so G5c never frees these slots. They are
	 * acquired {@linkplain GlintSlot#pinned pinned}: no G5c of the main source can free them either, so their params
	 * cannot be handed to another look before Iris has drawn them. Iris flushes inside {@code renderLevel}, in the same
	 * frame; G6 frees and unpins them at the next frame start. Slots are not registered in the main source here (Iris
	 * keeps its own buffers); G5b skips unregistered slots.
	 * <p>
	 * Under an active shader pack this hook stays out of the way (the vanilla look, as before), because the slot types
	 * would then go through Iris's entity wrapping and the pack's glint program; that path is untested.
	 *
	 * @return {@code requested} unless it is a vanilla glint type that has a custom look
	 */
	public static RenderType substituteBuffered(RenderType requested) {
		try {
			GlintRuntimeSettings s = settings;
			if (!bufferedSwapAllowed(s.anyCustom(), main != null, shaderPack, GlintFailsafe.tripped())) {
				return requested;
			}
			GlintKind kind = GlintKind.of(requested);
			if (kind == null || !RenderSystem.isOnRenderThread()) {
				return requested;
			}
			return pick(s, kind, requested, true);
		} catch (Throwable t) {
			GlintFailsafe.trip("substituteBuffered", t);
			return requested;
		}
	}

	/**
	 * G11/G12: the render type to use instead of {@code requested} when it is requested from one of ImmediatelyFast's
	 * GUI sources. With ImmediatelyFast, {@code GuiGraphics} draws into those instead of the main source: in
	 * {@code GameRenderer.render} it gets the non-batching {@code BufferSource} of {@code BatchingBuffers} (a plain
	 * vanilla instance, so G5a runs for it and hands it here, see {@link #isGuiSource}), and while a HUD layer is batched
	 * that is swapped for the {@code HudBatchingBufferSource} (G12). Neither is the main source, so G5a and G9 leave
	 * their glints vanilla.
	 * <p>
	 * Same rules as G5a, with {@code source} in the role of main (DESIGN 5.2, point 8): the slot type is registered as a
	 * fixed buffer of {@code source} itself, so it never goes through the shared buffer and is drawn after the vanilla
	 * fixed types (the vanilla glint and the item sheets) by that source's own {@code endBatch()}: per item for the
	 * non-batching source, at the end of the HUD layer (or when ImmediatelyFast force-draws the HUD batch) for the HUD
	 * source. Nothing here draws or flushes. Both draw through the vanilla private {@code endBatch(RenderType,
	 * BufferBuilder)}, so G5c releases the slots.
	 *
	 * @return {@code requested} unless it is a vanilla glint type that has a custom look
	 */
	public static RenderType substituteGui(MultiBufferSource.BufferSource source, RenderType requested) {
		try {
			GlintRuntimeSettings s = settings;
			if (!s.anyCustom() || main == null || source == main || GlintFailsafe.tripped()) {
				return requested;
			}
			GlintKind kind = GlintKind.of(requested);
			if (kind == null || !RenderSystem.isOnRenderThread()) {
				return requested;
			}
			return pick(s, kind, requested, source, false);
		} catch (Throwable t) {
			GlintFailsafe.trip("substituteGui", t);
			return requested;
		}
	}

	/**
	 * @return whether G5a must hand a request of {@code source} to {@link #substituteGui} instead of {@link #substitute}:
	 *         only for ImmediatelyFast's non-batching GUI source captured by G11 (one identity compare)
	 */
	public static boolean isGuiSource(MultiBufferSource.BufferSource source) {
		return source == guiSource && source != null;
	}

	/**
	 * G11: ImmediatelyFast handed out its non-batching GUI source (once per frame). Never throws.
	 */
	public static void onGuiSource(@Nullable MultiBufferSource.BufferSource source) {
		if (source != null && source != main) {
			guiSource = source;
		}
	}

	/** Test hook. */
	static void resetGuiSourceForTests() {
		guiSource = null;
	}

	/**
	 * G10: a texture atlas has been uploaded (after it was stitched, on every resource reload). For the block atlas,
	 * stores its size and the size of the reference sprite for the atlas factor.
	 */
	public static void onAtlasUploaded(ResourceLocation atlas, SpriteLoader.Preparations preparations) {
		try {
			if (!InventoryMenu.BLOCK_ATLAS.equals(atlas)) {
				return;
			}
			TextureAtlasSprite sprite = preparations.regions().get(GlintAtlasScale.REFERENCE_SPRITE_ID);
			int spriteWidth = sprite == null ? 0 : sprite.contents().width();
			int spriteHeight = sprite == null ? 0 : sprite.contents().height();
			if (GlintAtlasScale.update(preparations.width(), preparations.height(), spriteWidth, spriteHeight)) {
				Glintsmith.LOGGER.info("Glint atlas factor {} x {} (block atlas {}x{}, reference sprite {}x{}).",
					GlintAtlasScale.u(), GlintAtlasScale.v(), preparations.width(), preparations.height(), spriteWidth, spriteHeight);
			}
		} catch (Throwable t) {
			GlintFailsafe.trip("atlas size", t);
		}
	}

	/**
	 * Pure decision of G7 before the render type is looked at.
	 *
	 * @param anyCustom  whether any category or rule has a non-vanilla look
	 * @param started    whether the client has started (main source captured, textures created)
	 * @param shaderPack whether a shader pack was in use at the start of this frame
	 * @param tripped    whether the glint failsafe has tripped
	 */
	static boolean bufferedSwapAllowed(boolean anyCustom, boolean started, boolean shaderPack, boolean tripped) {
		return anyCustom && started && !shaderPack && !tripped;
	}

	/**
	 * G5b: after {@code endBatch(type)}. When the main source has drawn the vanilla {@code entityGlintDirect} (the last
	 * vanilla glint), draws every glint slot that has geometry, so the slots come right after the vanilla glint.
	 */
	public static void afterEndBatch(MultiBufferSource.BufferSource source, RenderType type) {
		try {
			// The source is compared first: it is the cheaper test and it keeps the vanilla types untouched until the
			// main source has been captured.
			if (source != main || main == null || type != GlintKind.Vanilla.ENTITY_GLINT_DIRECT) {
				return;
			}
			for (int i = 0; i < POOL.count(); i++) {
				GlintSlot<RenderType> slot = POOL.at(i);
				if (slot.inUse && slot.registered) {
					source.endBatch(slot.type);
				}
			}
		} catch (Throwable t) {
			GlintFailsafe.trip("afterEndBatch", t);
		}
	}

	/**
	 * G5c: a batch of {@code type} has been drawn by {@code source}; frees its slot. When the main source is a subclass
	 * (ImmediatelyFast's {@code BatchableBufferSource}, G9) whose public {@code endBatch(RenderType)} does not run the
	 * vanilla body, G5b never fires; the subclass still draws through the vanilla private method this hook sits in, so
	 * the G5b flush is done from here for it.
	 */
	public static void onBatchDrawn(MultiBufferSource.BufferSource source, RenderType type) {
		try {
			POOL.release(type);
			// The source is compared first, as in G5b: it keeps the vanilla types untouched until main is captured.
			if (source == main && main != null && flushesFromDraw(source, main, type == GlintKind.Vanilla.ENTITY_GLINT_DIRECT)) {
				afterEndBatch(source, type);
			}
		} catch (Throwable t) {
			GlintFailsafe.trip("onBatchDrawn", t);
		}
	}

	/**
	 * Pure decision of G5c: whether drawing a batch must also do the G5b flush. Only for the main source, only for the
	 * vanilla {@code entityGlintDirect} batch, and only when the source is not a plain vanilla {@code BufferSource}
	 * (for that class G5b runs at the end of the public {@code endBatch(RenderType)}).
	 *
	 * @param source             the source that drew the batch
	 * @param main               the captured main source, null before the client has started
	 * @param entityGlintDirect  whether the drawn type is the vanilla {@code entityGlintDirect}
	 */
	static boolean flushesFromDraw(@Nullable MultiBufferSource.BufferSource source,
		@Nullable MultiBufferSource.BufferSource main, boolean entityGlintDirect) {
		return entityGlintDirect && main != null && source == main && source.getClass() != MultiBufferSource.BufferSource.class;
	}

	/**
	 * G6: start of a frame. Advances the frame counter and frame time and frees slots that were never drawn.
	 */
	public static void onFrameBegin() {
		try {
			GlintContext.resetHands();
			if (initialized && !started) {
				onClientStarted(Minecraft.getInstance());
			}
			frame++;
			timeSeconds = Util.getMillis() / 1000.0;
			// False without Iris; true on any Iris API error (the safe side: G7 off).
			shaderPack = IrisCompat.queryShaderPack();
			POOL.releaseStale(frame);
		} catch (Throwable t) {
			GlintFailsafe.trip("onFrameBegin", t);
		}
	}

	// ---------------------------------------------------------------- resolution

	/**
	 * @param buffered true for Iris's fully buffered source (G7): the slot is pinned and not registered in the main
	 *                 source; false for the main source (G5a): the slot is registered as a fixed buffer of main
	 */
	private static RenderType pick(GlintRuntimeSettings s, GlintKind kind, RenderType requested, boolean buffered) {
		return pick(s, kind, requested, buffered ? null : main, buffered);
	}

	/**
	 * @param registerIn the source whose fixed buffers get the slot type: main (G5a), an ImmediatelyFast GUI source
	 *                   (G11/G12), or null for Iris's fully buffered source (G7)
	 * @param pin        whether the slot is acquired pinned (G7)
	 */
	private static RenderType pick(GlintRuntimeSettings s, GlintKind kind, RenderType requested,
		@Nullable MultiBufferSource.BufferSource registerIn, boolean pin) {
		GlintContext.Frame f = GlintContext.top();
		GlintCategory category = GlintCategories.decide(kind, f);

		GlintLook look;
		ItemStack stack;
		if (f == null || f.stack == null || f.stack.isEmpty()) {
			look = s.get(category).categoryLook();
			stack = ItemStack.EMPTY;
		} else {
			look = lookOf(f, category);
			if (look == null) {
				// The rules and the category defaults add up to the vanilla look for this stack.
				return requested;
			}
			stack = f.stack;
		}

		if (!look.enabled()) {
			// The off type has no params to protect (fixed texture, no colour or depth writes), so it is never pinned.
			GlintSlot<RenderType> off = POOL.acquireOff(kind, frame);
			return register(off, registerIn);
		}
		boolean tinted = look.tint() != null;
		int tint = tinted ? RuleEngine.glintTint(stack, look, timeSeconds) | 0xFF000000 : GlintParams.NO_TINT;
		SCRATCH.set(GlintTextures.resolve(look.texture(), kind.entityFamily, tinted), tint,
			look.speed(), look.scale(), look.strength(), look.rotation());
		// Checked before the atlas factor: a look that is vanilla stays the vanilla type, whatever the atlas.
		if (SCRATCH.isVanilla(kind)) {
			return requested;
		}
		if (s.atlasScale() && GlintCategories.blockAtlasUvs(kind, f, GlintContext.isDecalFoil(stack))) {
			SCRATCH.setAtlas(GlintAtlasScale.u(), GlintAtlasScale.v());
		}
		GlintSlot<RenderType> slot = POOL.acquire(kind, SCRATCH, frame, pin);
		if (slot == null) {
			return requested;
		}
		return register(slot, registerIn);
	}

	/**
	 * Registers a slot type as a fixed buffer of {@code source} if it is not one yet. For main the slot keeps a flag
	 * (G5b flushes only those slots); the ImmediatelyFast GUI sources are asked directly, which also covers types they
	 * copied from main's fixed map when ImmediatelyFast created them. Null (Iris) registers nothing.
	 */
	private static RenderType register(GlintSlot<RenderType> slot, @Nullable MultiBufferSource.BufferSource source) {
		if (source == null) {
			return slot.type;
		}
		if (source == main) {
			return ensureRegistered(slot);
		}
		SequencedMap<RenderType, ByteBufferBuilder> fixed = ((BufferSourceAccessor) source).glintsmith$getFixedBuffers();
		if (!fixed.containsKey(slot.type)) {
			fixed.put(slot.type, new ByteBufferBuilder(slot.type.bufferSize()));
		}
		return slot.type;
	}

	@Nullable
	private static GlintLook lookOf(GlintContext.Frame f, GlintCategory category) {
		if (f.isResolved(category)) {
			return f.looks[category.ordinal()];
		}
		ResolvedStyle style = RuleEngine.resolve(f.stack, GlintCategories.targetFor(category, f), category);
		GlintLook look = style == null ? null : style.glint();
		f.cache(category, look);
		return look;
	}

	/**
	 * Adds the slot's type to the fixed buffers of the main source (once), so it is drawn by {@code endBatch()} after the
	 * vanilla types and never through the shared buffer.
	 */
	private static RenderType ensureRegistered(GlintSlot<RenderType> slot) {
		if (!slot.registered) {
			MultiBufferSource.BufferSource source = main;
			((BufferSourceAccessor) source).glintsmith$getFixedBuffers().put(slot.type, new ByteBufferBuilder(slot.type.bufferSize()));
			slot.registered = true;
		}
		return slot.type;
	}

	// ---------------------------------------------------------------- lifecycle

	private static void onClientStarted(Minecraft client) {
		if (started) {
			return;
		}
		started = true;
		try {
			main = ((RenderBuffersAccessor) client.renderBuffers()).glintsmith$getBufferSource();
			GlintKind.captureVanilla();
			GlintTextures.init();
			GlintTextures.onConfigChanged(ConfigManager.current());
		} catch (Throwable t) {
			GlintFailsafe.trip("start", t);
		}
	}

	private static void onConfigChanged(GlintsmithConfig config) {
		try {
			settings = GlintRuntimeSettings.from(config);
			if (!started) {
				return;
			}
			if (RenderSystem.isOnRenderThread()) {
				GlintTextures.onConfigChanged(config);
			} else {
				RenderSystem.recordRenderCall(() -> {
					try {
						GlintTextures.onConfigChanged(config);
					} catch (Throwable t) {
						GlintFailsafe.trip("config change", t);
					}
				});
			}
		} catch (Throwable t) {
			GlintFailsafe.trip("config change", t);
		}
	}

	/** Rebuilds the textures that depend on resource packs. */
	private static final class TextureReloadListener implements SimpleSynchronousResourceReloadListener {
		@Override
		public ResourceLocation getFabricId() {
			return RELOAD_ID;
		}

		@Override
		public void onResourceManagerReload(ResourceManager manager) {
			try {
				if (started) {
					GlintTextures.reload(manager);
				}
			} catch (Throwable t) {
				GlintFailsafe.trip("resource reload", t);
			}
		}
	}
}
