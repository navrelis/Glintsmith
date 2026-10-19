package navrelis.glintsmith.render.outline;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import navrelis.glintsmith.config.ConfigManager;
import navrelis.glintsmith.config.GlintsmithConfig;
import navrelis.glintsmith.render.outline.compat.IrisCompat;
import navrelis.glintsmith.style.OutlineLook;
import navrelis.glintsmith.style.OutlineTarget;
import navrelis.glintsmith.style.ResolvedStyle;
import navrelis.glintsmith.style.RuleEngine;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.lwjgl.opengl.GL11;
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
	/** The flat models vanilla draws a trident or spyglass with in the GUI, on the ground and in frames. */
	private static final ModelResourceLocation TRIDENT_INVENTORY = ModelResourceLocation.inventory(
		ResourceLocation.withDefaultNamespace("trident"));
	private static final ModelResourceLocation SPYGLASS_INVENTORY = ModelResourceLocation.inventory(
		ResourceLocation.withDefaultNamespace("spyglass"));
	/** Scratch matrices of the proportional measure (render thread only). */
	private static final Matrix4f ITEM_POSE = new Matrix4f();
	private static final Matrix4f MODEL_TO_VIEW = new Matrix4f();

	private static volatile OutlineRuntimeSettings settings = OutlineRuntimeSettings.DISABLED;
	private static boolean initialized;
	@Nullable
	private static PhaseState world;
	@Nullable
	private static PhaseState hand;
	@Nullable
	private static PhaseState gui;
	/** Created on first use: most sessions never open the settings screen's preview. */
	@Nullable
	private static PhaseState preview;
	/** The snapshot {@link #previewSettings} was built from. */
	@Nullable
	private static GlintsmithConfig previewOwner;
	private static OutlineRuntimeSettings previewSettings = OutlineRuntimeSettings.DISABLED;

	private static final PhaseState[] OPEN = new PhaseState[4];
	private static int openCount;
	@Nullable
	private static PhaseState current;
	private static final TeePool TEES = new TeePool();
	private static final OccluderBufferSource OCCLUDERS = new OccluderBufferSource();

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
	private static int handRenders;
	private static int handCaptures;
	private static boolean handPhaseLogged;
	private static boolean targetsReleasedAfterTrip;

	private OutlineSystem() {
	}

	/**
	 * Registers the config listener. Call once from the client initialiser, after the config manager exists. Safe to
	 * call twice. The loader layer wires the events: {@link #onWorldStart}, {@link #onWorldLast},
	 * {@link #onClientStarted}, {@link #onClientStopping} and {@link #shaderReloadListener()}.
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

	/**
	 * @return whether an outline phase is open, so that an item rendered now may be captured
	 */
	public static boolean phaseOpen() {
		return current != null;
	}

	private static boolean ready() {
		return world != null && !OutlineFailsafe.tripped() && settings.enabled();
	}

	// ---- O1: ItemRenderer.render

	/**
	 * Starts one {@code ItemRenderer.render} call. {@code leftHand}, {@code pose} and {@code model} are the arguments of
	 * that call: the pose and the model's display transform give the item's size on screen for proportional outlines
	 * (null: unknown; with proportional outlines the item then counts as the first-person reference item).
	 *
	 * @return the buffer source to render the item with: a tee if the item gets an outline, otherwise {@code source}
	 */
	public static MultiBufferSource beginItem(ItemStack stack, ItemDisplayContext context, boolean leftHand,
		@Nullable PoseStack pose, @Nullable BakedModel model, MultiBufferSource source) {
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
			MultiBufferSource result = target == null ? source
				: capture(phase, stack, target, source, topHolder(), pose, context, leftHand, model);
			if (phase.phase == OutlinePhase.HAND) {
				handRenders++;
				if (result != source) {
					handCaptures++;
				}
			}
			return result;
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
				finishItem(tee, TEES, OutlineSystem::drawPendingGui);
			} catch (Throwable t) {
				OutlineFailsafe.trip("outline item release", t);
			}
		}
	}

	/**
	 * Releases the tee of a finished item render. A GUI item (or the preview item) that left geometry in the mask is
	 * drawn right away, at the point where vanilla {@code GuiGraphics.renderItem} flushes it (still with the item's
	 * lighting).
	 * <p>
	 * Why: HUD batching (ImmediatelyFast) suppresses that flush and keeps the item, together with everything drawn
	 * before it (hotbar sprite, earlier slots, stack counts), in a batch that is drawn at the end of the HUD layer or
	 * as soon as anything issues a draw call. The GUI composite issues draw calls with the mask target bound and a
	 * scissor box set, so that batch would be drawn into the mask and lost. Drawing it here puts it on the target
	 * the GUI draws to, in order, before the outline; non-outlined items keep batching. Without a batching mod
	 * vanilla flushes the same buffers a moment later anyway, so this only moves the same draw a little earlier.
	 *
	 * @param drawPendingGui draws everything the given buffer source holds
	 */
	static void finishItem(TeeBufferSource tee, TeePool pool, Consumer<MultiBufferSource> drawPendingGui) {
		if (tee.exitNested()) {
			return;
		}
		PhaseState phase = tee.phase();
		MultiBufferSource delegate = tee.delegate();
		pool.release(tee);
		if (phase != null && (phase.phase == OutlinePhase.GUI || phase.phase == OutlinePhase.PREVIEW) && phase.hasGeometry
			&& !OutlineFailsafe.tripped()) {
			drawPendingGui.accept(delegate);
		}
	}

	/**
	 * What {@code GuiGraphics.flush()} does (depth test off while drawing), except that the depth test is left as
	 * it was: a batching mod relies on it, and vanilla's own flush follows right after.
	 */
	private static void drawPendingGui(MultiBufferSource source) {
		if (!(source instanceof MultiBufferSource.BufferSource buffers)) {
			return;
		}
		boolean depthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
		RenderSystem.disableDepthTest();
		try {
			buffers.endBatch();
		} finally {
			if (depthTest) {
				RenderSystem.enableDepthTest();
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
	 *
	 * @param source the buffer source the hands are drawn with
	 * @return the buffer source to draw the hands with: {@code source}, or under a shader pack an
	 *         {@link OccluderBufferSource} over it that also records the hand geometry as occluders
	 */
	public static MultiBufferSource.BufferSource beginHands(MultiBufferSource.BufferSource source) {
		PhaseState phase = hand;
		if (phase == null || !ready()) {
			return source;
		}
		try {
			if (phase.open || !settings.handPhaseAllowed(shaderPack) || (!shaderPack && OutlineCompositor.depthCopyBroken())) {
				return source;
			}
			if (!handPending) {
				phase.discard();
			}
			handRenders = 0;
			handCaptures = 0;
			open(phase);
			if (shaderPack && source != null) {
				// The pack draws the hand into its own targets, so the main depth cannot hide the outline where the
				// arm covers the item: record the hand geometry as occluders for the mask depth instead.
				return OCCLUDERS.bind(source, phase);
			}
		} catch (Throwable t) {
			OutlineFailsafe.trip("outline hand begin", t);
		}
		return source;
	}

	/**
	 * Closes the HAND phase: composites at once, or under a shader pack draws the mask and defers the composite to
	 * {@link #onLevelRendered()}.
	 */
	public static void endHands() {
		PhaseState phase = hand;
		if (phase == null || !phase.open) {
			releaseOccluders();
			return;
		}
		try {
			OCCLUDERS.release(); // completes the last occluder vertex before the mask is drawn
			close(phase);
			if (OutlineFailsafe.tripped()) {
				return;
			}
			logHandPhaseOnce(phase);
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

	private static void releaseOccluders() {
		try {
			OCCLUDERS.release();
		} catch (Throwable t) {
			OutlineFailsafe.trip("outline occluder release", t);
		}
	}

	/**
	 * Once per session, at DEBUG: the first HAND phase that captured an item, so the first-person path can be
	 * confirmed in game.
	 */
	private static void logHandPhaseOnce(PhaseState phase) {
		if (handPhaseLogged || handCaptures == 0) {
			return;
		}
		handPhaseLogged = true;
		LOGGER.debug("[Glintsmith] First-person outline phase: captured {} of {} item renders, mask geometry {}, {}.",
			handCaptures, handRenders, phase.hasGeometry ? "present" : "missing",
			shaderPack ? "composite deferred to the end of the level (shader pack)" : "composited now");
	}

	// ---- O3, O4: armor and elytra

	/**
	 * @param pose the pose the layer draws the armor with (entity model space, one unit per block), for the size on
	 *        screen of proportional outlines; null if unknown
	 * @return the buffer source for an armor piece (or the elytra) worn by {@code entity} in {@code slot}
	 */
	public static MultiBufferSource beginArmor(LivingEntity entity, EquipmentSlot slot, @Nullable PoseStack pose,
		MultiBufferSource source) {
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
			return capture(phase, stack, OutlineTarget.ARMOR, source, entity, pose, null, false, null);
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
	 * Closes the GUI phase and composites the item's outline (the item was already drawn: by vanilla's flush, or
	 * under HUD batching by {@link #finishItem}).
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

	// ---- settings screen preview

	/**
	 * Opens the PREVIEW phase for one item render; outlines it like the first-person hand under {@code snapshot}.
	 * The live settings do not gate it: the rule engine, scoped to the same snapshot by the caller, decides.
	 *
	 * @return whether the phase was opened; if so, {@link #endPreview()} must follow
	 */
	public static boolean beginPreview(GlintsmithConfig snapshot) {
		if (world == null || OutlineFailsafe.tripped() || snapshot == null) {
			return false;
		}
		try {
			if (preview == null) {
				preview = new PhaseState(OutlinePhase.PREVIEW, false);
			}
			if (preview.open || current != null) {
				return false; // never nest into another phase
			}
			if (snapshot != previewOwner) {
				previewSettings = OutlineRuntimeSettings.from(snapshot);
				previewOwner = snapshot;
			}
			preview.discard();
			open(preview);
			// The same thickness rules as every other phase, but from the snapshot (proportional, scaleWithResolution).
			preview.settings = previewSettings;
			return true;
		} catch (Throwable t) {
			OutlineFailsafe.trip("outline preview begin", t);
			return false;
		}
	}

	/**
	 * Closes the PREVIEW phase and composites it (flat mask, no depth), clipped to the current scissor box. Call after
	 * the preview item was flushed.
	 */
	public static void endPreview() {
		PhaseState phase = preview;
		if (phase == null || !phase.open) {
			return;
		}
		try {
			close(phase);
			if (OutlineFailsafe.tripped()) {
				return;
			}
			OutlineCompositor.composite(phase, OutlineCompositor.MaskDepth.NONE, false, 0.0f, 0.0f);
		} catch (Throwable t) {
			OutlineFailsafe.trip("outline preview composite", t);
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
			OCCLUDERS.release();
			while (openCount > 0) {
				close(OPEN[openCount - 1]);
			}
			world.discard();
			gui.discard();
			if (preview != null) {
				preview.discard();
			}
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

	/**
	 * E1: start of the world render pass.
	 *
	 * @param consumers the vertex consumer provider of the world pass; the phase only opens if it is the main
	 *                  buffer source
	 */
	public static void onWorldStart(@Nullable MultiBufferSource consumers) {
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
			if (consumers instanceof MultiBufferSource.BufferSource source) {
				mainSource = source;
				open(phase);
			}
		} catch (Throwable t) {
			OutlineFailsafe.trip("outline world begin", t);
		}
	}

	/** E2: end of the world render pass: composites the world outlines. */
	public static void onWorldLast() {
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

	/**
	 * {@code pose}, {@code context}, {@code leftHand} and {@code model} describe how the item is drawn, for its size on
	 * screen ({@link #pixelsPerUnit}); {@code context} and {@code model} are null for armor (no display transform).
	 */
	private static MultiBufferSource capture(PhaseState phase, ItemStack stack, OutlineTarget target, MultiBufferSource source,
		@Nullable LivingEntity holder, @Nullable PoseStack pose, @Nullable ItemDisplayContext context, boolean leftHand,
		@Nullable BakedModel model) {
		OutlineRuntimeSettings s = settings;
		boolean local = holder != null && holder == Minecraft.getInstance().player;
		// The preview follows its snapshot, not the live settings; the scoped rule engine applies the snapshot's switches.
		boolean previewPhase = phase.phase == OutlinePhase.PREVIEW;
		if (!previewPhase && !s.accepts(phase.phase, target, local)) {
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
		OutlineRuntimeSettings thicknessSettings = phase.settings;
		float pixelsPerUnit = thicknessSettings.proportional() ? pixelsPerUnit(phase, stack, pose, context, leftHand, model)
			: Float.NaN;
		float thickness = thicknessSettings.itemThickness(look.thickness(), pixelsPerUnit, phase.framebufferHeight);
		int index = phase.styles.indexFor(argb, thickness, look.softness());
		if (index == 0) {
			return source;
		}
		phase.prepareCapture();
		return TEES.acquire(source, phase, index);
	}

	/**
	 * The item's size on screen per model unit ({@link OutlineScale#pixelsPerUnit}) from the pose it is drawn with,
	 * its display transform and the matrices in effect now: the ones the mask is drawn with, as for the screen bounds
	 * ({@link PhaseState#prepareCapture}).
	 *
	 * @return pixels per model unit, or NaN if unknown
	 */
	private static float pixelsPerUnit(PhaseState phase, ItemStack stack, @Nullable PoseStack pose,
		@Nullable ItemDisplayContext context, boolean leftHand, @Nullable BakedModel model) {
		if (pose == null) {
			return Float.NaN;
		}
		OutlineScale.itemPose(ITEM_POSE, pose.last().pose(), displayTransform(stack, context, model), leftHand);
		MODEL_TO_VIEW.set(RenderSystem.getModelViewMatrix()).mul(ITEM_POSE);
		return OutlineScale.pixelsPerUnit(RenderSystem.getProjectionMatrix(), MODEL_TO_VIEW, phase.framebufferHeight);
	}

	/**
	 * The display transform {@code ItemRenderer.render} will apply, including its swap of the trident and the spyglass
	 * to their flat models in the GUI, on the ground and in frames.
	 */
	@Nullable
	private static ItemTransform displayTransform(ItemStack stack, @Nullable ItemDisplayContext context,
		@Nullable BakedModel model) {
		if (context == null || model == null) {
			return null;
		}
		BakedModel drawn = model;
		if (context == ItemDisplayContext.GUI || context == ItemDisplayContext.GROUND || context == ItemDisplayContext.FIXED) {
			if (stack.is(Items.TRIDENT)) {
				drawn = Minecraft.getInstance().getModelManager().getModel(TRIDENT_INVENTORY);
			} else if (stack.is(Items.SPYGLASS)) {
				drawn = Minecraft.getInstance().getModelManager().getModel(SPYGLASS_INVENTORY);
			}
		}
		return drawn == null || drawn.getTransforms() == null ? null : drawn.getTransforms().getTransform(context);
	}

	private static double time() {
		return frameHook ? timeSeconds : Util.getMillis() / 1000.0;
	}

	// ---- phase stack

	private static void open(PhaseState phase) {
		if (phase.open) {
			return;
		}
		phase.settings = settings;
		phase.framebufferHeight = Minecraft.getInstance().getMainRenderTarget().height;
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
		OCCLUDERS.release();
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

	/** The client has started: creates the phases and the white mask texture. */
	public static void onClientStarted(Minecraft minecraft) {
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

	/** The client is stopping: frees the native resources. */
	public static void onClientStopping(Minecraft minecraft) {
		try {
			abandon();
			closePhase(world);
			closePhase(hand);
			closePhase(gui);
			closePhase(preview);
			world = null;
			hand = null;
			gui = null;
			preview = null;
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
			phase.close();
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

	/** Id under which the loader registers the shader reload listener. */
	public static final ResourceLocation SHADER_RELOAD_ID = ResourceLocation.fromNamespaceAndPath("glintsmith", "outline_shaders");

	/**
	 * E4: the listener that marks the outline shaders for re-creation after every resource reload. The loader layer
	 * registers it under {@link #SHADER_RELOAD_ID}.
	 */
	public static ResourceManagerReloadListener shaderReloadListener() {
		return new ShaderReloadListener();
	}

	/** E4: marks the outline shaders for re-creation after every resource reload. */
	private static final class ShaderReloadListener implements ResourceManagerReloadListener {
		@Override
		public void onResourceManagerReload(ResourceManager manager) {
			OutlineShaders.markDirty();
		}
	}
}
