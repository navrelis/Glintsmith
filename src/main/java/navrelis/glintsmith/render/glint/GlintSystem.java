package navrelis.glintsmith.render.glint;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import navrelis.glintsmith.Glintsmith;
import navrelis.glintsmith.config.ConfigManager;
import navrelis.glintsmith.config.GlintsmithConfig;
import navrelis.glintsmith.mixin.glint.BufferSourceAccessor;
import navrelis.glintsmith.mixin.glint.RenderBuffersAccessor;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
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
 */
public final class GlintSystem {
	private static final ResourceLocation RELOAD_ID = ResourceLocation.fromNamespaceAndPath(Glintsmith.MOD_ID, "glint_textures");

	private static final GlintSlotPool<RenderType> POOL = new GlintSlotPool<>(GlintRenderTypes.FACTORY);
	private static final GlintParams SCRATCH = new GlintParams();

	private static volatile GlintRuntimeSettings settings = GlintRuntimeSettings.VANILLA;
	@Nullable
	private static MultiBufferSource.BufferSource main;
	private static boolean initialized;
	private static boolean started;
	private static long frame;
	private static double timeSeconds;

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
			return pick(s, kind, requested);
		} catch (Throwable t) {
			GlintFailsafe.trip("substitute", t);
			return requested;
		}
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
	 * G5c: a batch of {@code type} has been drawn; frees its slot.
	 */
	public static void onBatchDrawn(RenderType type) {
		try {
			POOL.release(type);
		} catch (Throwable t) {
			GlintFailsafe.trip("onBatchDrawn", t);
		}
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
			POOL.releaseStale(frame);
		} catch (Throwable t) {
			GlintFailsafe.trip("onFrameBegin", t);
		}
	}

	// ---------------------------------------------------------------- resolution

	private static RenderType pick(GlintRuntimeSettings s, GlintKind kind, RenderType requested) {
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
			return ensureRegistered(POOL.acquireOff(kind, frame));
		}
		boolean tinted = look.tint() != null;
		int tint = tinted ? RuleEngine.glintTint(stack, look, timeSeconds) | 0xFF000000 : GlintParams.NO_TINT;
		SCRATCH.set(GlintTextures.resolve(look.texture(), kind.entityFamily, tinted), tint,
			look.speed(), look.scale(), look.strength(), look.rotation());
		if (SCRATCH.isVanilla(kind)) {
			return requested;
		}
		GlintSlot<RenderType> slot = POOL.acquire(kind, SCRATCH, frame);
		if (slot == null) {
			return requested;
		}
		return ensureRegistered(slot);
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
