package navrelis.glintsmith.render.outline;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.platform.NativeImage;
import navrelis.glintsmith.config.ConfigManager;
import navrelis.glintsmith.render.outline.compat.IrisCompat;
import navrelis.glintsmith.style.OutlineLook;
import navrelis.glintsmith.style.OutlineTarget;
import navrelis.glintsmith.style.ResolvedStyle;
import navrelis.glintsmith.style.RuleEngine;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Static entry point of the outline renderer, called from the outline mixins and the Fabric events registered by
 * {@link #init()}. Render thread only.
 * <p>
 * Flow: a phase (world, first-person hand, one GUI item) is opened around the vanilla code that draws items. While
 * it is open, a qualifying item render gets a {@link TeeBufferSource} instead of its buffer source, which copies the
 * item's geometry into the phase's mask. When vanilla has flushed that geometry, {@link OutlineCompositor} turns the
 * mask into outlines on the main target.
 * <p>
 * Every entry point catches {@link Throwable}, trips the {@link OutlineFailsafe} and falls back to vanilla
 * behaviour. Nothing here runs before {@link #init()}; with outlines off the hooks return immediately.
 */
public final class OutlineSystem {
	private static final Logger LOGGER = LoggerFactory.getLogger("Glintsmith/Outline");
	private static final int MAX_HOLDERS = 16;

	private static volatile OutlineRuntimeSettings settings = OutlineRuntimeSettings.DISABLED;
	private static boolean initialized;
	@Nullable
	private static PhaseState world;
	@Nullable
	private static PhaseState hand;
	@Nullable
	private static PhaseState gui;

	private static final PhaseState[] OPEN = new PhaseState[3];
	private static int openCount;
	@Nullable
	private static PhaseState current;
	private static final TeePool TEES = new TeePool();

	private static final LivingEntity[] HOLDERS = new LivingEntity[MAX_HOLDERS];
	private static int holderDepth;
	private static int guiDepth;

	@Nullable
	private static MultiBufferSource.BufferSource mainSource;
	private static boolean inFullFlush;
	private static boolean shaderPack;
	private static boolean packNoticeLogged;
	private static long frame;
	private static boolean frameHook;
	private static double timeSeconds;
	private static boolean handPending;
	private static long handMaskFrame = -1L;
	private static boolean targetsReleasedAfterTrip;

	private OutlineSystem() {
	}

	/**
	 * Registers the Fabric events, the shader reload listener and the config listener. Call once from the client
	 * initialiser, after the config manager exists. Safe to call twice.
	 */
	public static void init() {
		if (initialized) {
			return;
		}
		initialized = true;
		try {
			OutlineFailsafe.setOnTrip(OutlineSystem::abandon);
			ConfigManager manager = ConfigManager.instance();
			settings = OutlineRuntimeSettings.from(manager.get());
			manager.addListener(config -> settings = OutlineRuntimeSettings.from(config));
			WorldRenderEvents.START.register(OutlineSystem::onWorldStart);
			WorldRenderEvents.LAST.register(OutlineSystem::onWorldLast);
			ClientLifecycleEvents.CLIENT_STARTED.register(OutlineSystem::onClientStarted);
			ClientLifecycleEvents.CLIENT_STOPPING.register(OutlineSystem::onClientStopping);
			ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new ShaderReloadListener());
		} catch (Throwable t) {
			OutlineFailsafe.trip("outline init", t);
		}
	}

	/**
	 * @return whether an Iris shader pack is in use right now (false without Iris)
	 */
	public static boolean shaderPackActive() {
		return IrisCompat.queryShaderPack();
	}

	/**
	 * @return the settings currently in effect
	 */
	public static OutlineRuntimeSettings settings() {
		return settings;
	}

	private static boolean ready() {
		return world != null && !OutlineFailsafe.tripped() && settings.enabled();
	}

	// ---- O1: ItemRenderer.render

	/**
	 * @return the buffer source to render the item with: a tee if the item gets an outline, otherwise {@code source}
	 */
	public static MultiBufferSource beginItem(ItemStack stack, ItemDisplayContext context, MultiBufferSource source) {
		if (source instanceof TeeBufferSource tee) {
			// Nested render into an outer tee: it joins the outer item's outline and must not release it.
			tee.enterNested();
			return source;
		}
		PhaseState phase = current;
		if (phase == null || OutlineFailsafe.tripped()) {
			return source;
		}
		try {
			if (stack == null || stack.isEmpty() || source == null) {
				return source;
			}
			OutlineTarget target = OutlineContexts.forPhase(phase.phase, context);
			if (target == null) {
				return source;
			}
			return capture(phase, stack, target, source, topHolder());
		} catch (Throwable t) {
			OutlineFailsafe.trip("outline item capture", t);
			return source;
		}
	}

	/**
	 * Ends an item render started with {@link #beginItem} (or {@link #beginArmor}).
	 *
	 * @param returnedByBegin exactly what the begin call returned
	 */
	public static void endItem(MultiBufferSource returnedByBegin) {
		if (returnedByBegin instanceof TeeBufferSource tee) {
			try {
				if (tee.exitNested()) {
					return;
				}
				TEES.release(tee);
			} catch (Throwable t) {
				OutlineFailsafe.trip("outline item release", t);
			}
		}
	}

	// ---- O2: ItemInHandRenderer

	/**
	 * Pushes the entity whose held item is being drawn (for "only local player").
	 */
	public static void pushHolder(@Nullable LivingEntity holder) {
		if (holderDepth < MAX_HOLDERS) {
			HOLDERS[holderDepth] = holder;
		}
		holderDepth++;
	}

	public static void popHolder() {
		if (holderDepth > 0) {
			holderDepth--;
			if (holderDepth < MAX_HOLDERS) {
				HOLDERS[holderDepth] = null;
			}
		}
	}

	@Nullable
	private static LivingEntity topHolder() {
		int depth = holderDepth;
		return depth > 0 && depth <= MAX_HOLDERS ? HOLDERS[depth - 1] : null;
	}

	/**
	 * Opens the HAND phase around {@code renderHandsWithItems}.
	 */
	public static void beginHands() {
		PhaseState phase = hand;
		if (phase == null || !ready()) {
			return;
		}
		try {
			if (phase.open || !settings.handPhaseAllowed(shaderPack) || (!shaderPack && OutlineCompositor.depthCopyBroken())) {
				return;
			}
			if (!handPending) {
				phase.discard();
			}
			open(phase);
		} catch (Throwable t) {
			OutlineFailsafe.trip("outline hand begin", t);
		}
	}

	/**
	 * Closes the HAND phase: composites at once, or under a shader pack draws the mask and defers the composite to
	 * {@link #onLevelRendered()}.
	 */
	public static void endHands() {
		PhaseState phase = hand;
		if (phase == null || !phase.open) {
			return;
		}
		try {
			close(phase);
			if (OutlineFailsafe.tripped()) {
				return;
			}
			if (shaderPack) {
				boolean firstFlush = handMaskFrame != frame;
				if (OutlineCompositor.drawMaskOnly(phase, firstFlush)) {
					handPending = true;
					handMaskFrame = frame;
				}
			} else {
				OutlineCompositor.composite(phase, OutlineCompositor.MaskDepth.COPY, true, 0.0f, 0.0f);
			}
		} catch (Throwable t) {
			OutlineFailsafe.trip("outline hand composite", t);
		}
	}

	// ---- O3, O4: armor and elytra

	/**
	 * @return the buffer source for an armor piece (or the elytra) worn by {@code entity} in {@code slot}
	 */
	public static MultiBufferSource beginArmor(LivingEntity entity, EquipmentSlot slot, MultiBufferSource source) {
		if (source instanceof TeeBufferSource tee) {
			tee.enterNested();
			return source;
		}
		PhaseState phase = current;
		if (phase == null || OutlineFailsafe.tripped()) {
			return source;
		}
		try {
			if (entity == null || slot == null || source == null) {
				return source;
			}
			ItemStack stack = entity.getItemBySlot(slot);
			if (stack.isEmpty()) {
				return source;
			}
			return capture(phase, stack, OutlineTarget.ARMOR, source, entity);
		} catch (Throwable t) {
			OutlineFailsafe.trip("outline armor capture", t);
			return source;
		}
	}

	/**
	 * Ends an armor render started with {@link #beginArmor}.
	 */
	public static void endArmor(MultiBufferSource returnedByBegin) {
		endItem(returnedByBegin);
	}

	// ---- O5: GuiGraphics.renderItem

	/**
	 * Opens the GUI phase around one {@code GuiGraphics.renderItem} call (nested calls join the outer one).
	 */
	public static void beginGuiItem() {
		guiDepth++;
		PhaseState phase = gui;
		if (guiDepth != 1 || phase == null || !ready()) {
			return;
		}
		try {
			if (!phase.open && settings.guiPhaseAllowed(shaderPack)) {
				phase.discard();
				open(phase);
			}
		} catch (Throwable t) {
			OutlineFailsafe.trip("outline gui begin", t);
		}
	}

	/**
	 * Closes the GUI phase and composites the item's outline (the item was already flushed by vanilla).
	 */
	public static void endGuiItem() {
		if (guiDepth > 0) {
			guiDepth--;
		}
		PhaseState phase = gui;
		if (guiDepth != 0 || phase == null || !phase.open) {
			return;
		}
		try {
			close(phase);
			if (OutlineFailsafe.tripped()) {
				return;
			}
			OutlineCompositor.composite(phase, OutlineCompositor.MaskDepth.NONE, false, 0.0f, 0.0f);
		} catch (Throwable t) {
			OutlineFailsafe.trip("outline gui composite", t);
		}
	}

	// ---- O6: BufferSource flushes

	/**
	 * HEAD ({@code begin = true}) and TAIL of {@code BufferSource.endBatch()}.
	 */
	public static void onFullFlush(MultiBufferSource.BufferSource source, boolean begin) {
		if (source == mainSource && source != null) {
			inFullFlush = begin;
		}
	}

	/**
	 * TAIL of {@code BufferSource.endBatch(RenderType)}: the water-mask flush of the main source is the world
	 * composite point (all item geometry is in the main depth by then, translucent terrain and weather are not).
	 */
	public static void onEndBatch(MultiBufferSource.BufferSource source, RenderType type) {
		if (source != mainSource || source == null || inFullFlush) {
			return;
		}
		PhaseState phase = world;
		if (phase == null || !phase.open) {
			return;
		}
		try {
			if (type == RenderType.waterMask()) {
				compositeWorld(phase);
			}
		} catch (Throwable t) {
			OutlineFailsafe.trip("outline world composite", t);
		}
	}

	// ---- O7: GameRenderer

	/**
	 * HEAD of {@code GameRenderer.render}.
	 */
	public static void onFrameBegin() {
		frame++;
		frameHook = true;
		timeSeconds = Util.getMillis() / 1000.0;
		if (world == null) {
			return;
		}
		if (OutlineFailsafe.tripped()) {
			releaseAfterTrip();
			return;
		}
		try {
			IrisCompat.refreshPerFrame();
			shaderPack = IrisCompat.shaderPackInUse();
			inFullFlush = false;
			OutlineTargets.tickFrame();
			logShaderPackNotice();
		} catch (Throwable t) {
			OutlineFailsafe.trip("outline frame begin", t);
		}
	}

	/**
	 * RETURN of {@code GameRenderer.render}: discards whatever was not composited and closes stray phases.
	 */
	public static void onFrameEnd() {
		holderDepth = 0;
		guiDepth = 0;
		for (int i = 0; i < MAX_HOLDERS; i++) {
			HOLDERS[i] = null;
		}
		if (world == null || OutlineFailsafe.tripped()) {
			return;
		}
		try {
			TEES.releaseAll();
			while (openCount > 0) {
				close(OPEN[openCount - 1]);
			}
			world.discard();
			gui.discard();
			handPending = false;
			hand.discard();
			inFullFlush = false;
		} catch (Throwable t) {
			OutlineFailsafe.trip("outline frame end", t);
		}
	}

	/**
	 * RETURN of {@code GameRenderer.renderLevel}: the deferred hand composite under a shader pack, onto the final
	 * image.
	 */
	public static void onLevelRendered() {
		if (!handPending) {
			return;
		}
		handPending = false;
		PhaseState phase = hand;
		if (phase == null || OutlineFailsafe.tripped()) {
			return;
		}
		try {
			OutlineCompositor.resolve(phase);
		} catch (Throwable t) {
			OutlineFailsafe.trip("outline hand composite", t);
		}
	}

	// ---- E1, E2: world events

	private static void onWorldStart(WorldRenderContext context) {
		PhaseState phase = world;
		if (phase == null || !ready()) {
			return;
		}
		try {
			inFullFlush = false;
			if (phase.open) {
				close(phase);
			}
			phase.discard();
			if (!settings.worldPhaseAllowed(shaderPack) || OutlineCompositor.depthCopyBroken()) {
				return;
			}
			if (context.consumers() instanceof MultiBufferSource.BufferSource source) {
				mainSource = source;
				open(phase);
			}
		} catch (Throwable t) {
			OutlineFailsafe.trip("outline world begin", t);
		}
	}

	private static void onWorldLast(WorldRenderContext context) {
		PhaseState phase = world;
		if (phase == null || !phase.open || OutlineFailsafe.tripped()) {
			return;
		}
		try {
			compositeWorld(phase);
		} catch (Throwable t) {
			OutlineFailsafe.trip("outline world composite", t);
		}
	}

	private static void compositeWorld(PhaseState phase) {
		close(phase);
		OutlineRuntimeSettings s = settings;
		boolean fade = s.fadeActive();
		OutlineCompositor.composite(phase, OutlineCompositor.MaskDepth.COPY, true,
			fade ? s.fadeStart() : 0.0f, fade ? s.fadeEnd() : 0.0f);
	}

	// ---- capture

	private static MultiBufferSource capture(PhaseState phase, ItemStack stack, OutlineTarget target, MultiBufferSource source,
		@Nullable LivingEntity holder) {
		OutlineRuntimeSettings s = settings;
		boolean local = holder != null && holder == Minecraft.getInstance().player;
		if (!s.accepts(phase.phase, target, local)) {
			return source;
		}
		if (IrisCompat.loaded() && IrisCompat.shadowPass()) {
			return source;
		}
		ResolvedStyle style = RuleEngine.resolve(stack, target, OutlineContexts.categoryOf(target));
		OutlineLook look = style.outline();
		if (look == null || !(look.thickness() > 0.0f) || !(look.opacity() > 0.0f)) {
			return source;
		}
		int argb = RuleEngine.outlineColor(stack, look, time());
		if ((argb >>> 24) == 0) {
			return source;
		}
		int index = phase.styles.indexFor(argb, look.thickness() * phase.thicknessScale, look.softness());
		if (index == 0) {
			return source;
		}
		phase.prepareCapture();
		return TEES.acquire(source, phase, index);
	}

	private static double time() {
		return frameHook ? timeSeconds : Util.getMillis() / 1000.0;
	}

	// ---- phase stack

	private static void open(PhaseState phase) {
		if (phase.open) {
			return;
		}
		Minecraft minecraft = Minecraft.getInstance();
		phase.thicknessScale = settings.thicknessScale(phase.phase, minecraft.getMainRenderTarget().height,
			minecraft.getWindow().getGuiScale());
		phase.open = true;
		if (openCount < OPEN.length) {
			OPEN[openCount++] = phase;
		}
		current = phase;
	}

	private static void close(PhaseState phase) {
		if (!phase.open) {
			return;
		}
		phase.open = false;
		for (int i = openCount - 1; i >= 0; i--) {
			if (OPEN[i] == phase) {
				for (int j = i; j < openCount - 1; j++) {
					OPEN[j] = OPEN[j + 1];
				}
				openCount--;
				OPEN[openCount] = null;
				break;
			}
		}
		current = openCount > 0 ? OPEN[openCount - 1] : null;
	}

	/**
	 * After a failsafe trip, frees the off-screen targets once, at a frame start where no pass can be using them.
	 * The mask buffers stay until shutdown (they may hold a half-written vertex and are never used again).
	 */
	private static void releaseAfterTrip() {
		if (targetsReleasedAfterTrip) {
			return;
		}
		targetsReleasedAfterTrip = true;
		try {
			OutlineTargets.release();
		} catch (Throwable ignored) {
			// outlines are already off; freeing the targets is best effort
		}
	}

	/** Failsafe hook: forget every phase; the mask buffers are never used again. */
	private static void abandon() {
		current = null;
		for (int i = 0; i < OPEN.length; i++) {
			if (OPEN[i] != null) {
				OPEN[i].open = false;
			}
			OPEN[i] = null;
		}
		openCount = 0;
		handPending = false;
		mainSource = null;
		TEES.releaseAll();
	}

	private static void logShaderPackNotice() {
		OutlineRuntimeSettings s = settings;
		if (!shaderPack) {
			packNoticeLogged = false;
			return;
		}
		if (packNoticeLogged || !s.enabled()) {
			return;
		}
		packNoticeLogged = true;
		if (s.disableWithShaders()) {
			LOGGER.info("[Glintsmith] A shader pack is active: outlines are off (disableWithShaders).");
		} else if (s.anyWorld()) {
			LOGGER.info("[Glintsmith] A shader pack is active: world outlines are off, first-person and GUI outlines stay on.");
		}
	}

	// ---- E3, E4: lifecycle and resources

	private static void onClientStarted(Minecraft minecraft) {
		try {
			world = new PhaseState(OutlinePhase.WORLD, true);
			hand = new PhaseState(OutlinePhase.HAND, true);
			gui = new PhaseState(OutlinePhase.GUI, false);
			NativeImage white = new NativeImage(1, 1, false);
			white.setPixelRGBA(0, 0, 0xFFFFFFFF);
			minecraft.getTextureManager().register(MaskTypes.WHITE, new DynamicTexture(white));
			reportMissingHooks();
		} catch (Throwable t) {
			OutlineFailsafe.trip("outline startup", t);
		}
	}

	private static void onClientStopping(Minecraft minecraft) {
		try {
			abandon();
			closePhase(world);
			closePhase(hand);
			closePhase(gui);
			world = null;
			hand = null;
			gui = null;
			OutlineTargets.release();
			OutlineShaders.close();
			FullscreenQuad.close();
			OutlineCompositor.close();
		} catch (Throwable ignored) {
			// the game is shutting down; native memory is freed with the process anyway
		}
	}

	private static void closePhase(@Nullable PhaseState phase) {
		if (phase != null) {
			phase.maskSource.close();
		}
	}

	/**
	 * Logs one warning if an outline hook did not apply (every injector has {@code require = 0}, so a conflict with
	 * another mod is otherwise silent). Runs once at startup.
	 */
	private static void reportMissingHooks() {
		List<String> missing = new ArrayList<>();
		check(missing, net.minecraft.client.renderer.entity.ItemRenderer.class, "glintsmith$outlineItem", "item (O1)");
		check(missing, net.minecraft.client.renderer.ItemInHandRenderer.class, "glintsmith$outlineHolder", "held item holder (O2a)");
		check(missing, net.minecraft.client.renderer.ItemInHandRenderer.class, "glintsmith$outlineHands", "first person (O2b)");
		check(missing, net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer.class, "glintsmith$outlineArmor", "armor (O3)");
		check(missing, net.minecraft.client.renderer.entity.layers.ElytraLayer.class, "glintsmith$outlineElytra", "elytra (O4)");
		check(missing, net.minecraft.client.gui.GuiGraphics.class, "glintsmith$outlineGuiItem", "GUI (O5)");
		check(missing, MultiBufferSource.BufferSource.class, "glintsmith$outlineFlush", "world composite (O6)");
		check(missing, net.minecraft.client.renderer.GameRenderer.class, "glintsmith$outlineFrame", "frame (O7)");
		if (!missing.isEmpty()) {
			LOGGER.warn("[Glintsmith] Some outline hooks were not applied (probably a mod conflict), these outlines will be missing: {}",
				String.join(", ", missing));
		}
	}

	private static void check(List<String> missing, Class<?> target, String handler, String what) {
		for (Method method : target.getDeclaredMethods()) {
			if (method.getName().contains(handler)) {
				return;
			}
		}
		missing.add(what);
	}

	/** E4: marks the outline shaders for re-creation after every resource reload. */
	private static final class ShaderReloadListener implements SimpleSynchronousResourceReloadListener {
		private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("glintsmith", "outline_shaders");

		@Override
		public ResourceLocation getFabricId() {
			return ID;
		}

		@Override
		public void onResourceManagerReload(ResourceManager manager) {
			OutlineShaders.markDirty();
		}
	}
}
