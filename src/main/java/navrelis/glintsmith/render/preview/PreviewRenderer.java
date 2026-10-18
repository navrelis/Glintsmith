package navrelis.glintsmith.render.preview;

import java.util.HashMap;
import java.util.Map;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import navrelis.glintsmith.config.GlintsmithConfig;
import navrelis.glintsmith.render.outline.OutlineSystem;
import navrelis.glintsmith.style.RuleEngine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Draws the settings screen's live preview item. Render thread only.
 * <p>
 * The item is drawn with {@link ItemDisplayContext#FIRST_PERSON_RIGHT_HAND}, so outline and glint classify it exactly
 * like the item in the hand, under a pose that cancels that context's display transform and turns the item upright
 * on a turntable fitted to the box. For exactly that one render the rule engine and the outline system are scoped to
 * the given config snapshot, so the preview shows unsaved settings while the rest of the game keeps the live ones.
 * <p>
 * A throw disables the preview for the rest of the session ({@link #available()}); the game itself is unaffected.
 */
public final class PreviewRenderer {
	private static final Logger LOGGER = LoggerFactory.getLogger("Glintsmith/Preview");
	private static final int MIN_BOX = 8;
	/** GUI depth of the box centre, like GUI items. */
	private static final float CENTER_Z = 150.0f;
	/** Depth budget either side of {@link #CENTER_Z}: keeps the item between z 40 and 260. */
	private static final float MAX_DEPTH = 110.0f;
	private static final int MAX_BOUNDS = 16;

	private static final Map<BoundsKey, float[]> BOUNDS = new HashMap<>();
	private static boolean disabled;

	private PreviewRenderer() {
	}

	/**
	 * @return false after an error disabled the preview for this session
	 */
	public static boolean available() {
		return !disabled;
	}

	/**
	 * Draws {@code stack} into the box {@code x0,y0 .. x1,y1} (GUI coordinates) as it looks in the hand under
	 * {@code snapshot}, turned by {@code yawDeg} and tilted by {@code pitchDeg}.
	 *
	 * @param snapshot a sanitised config that stays the same instance while nothing changes (scoped caches are kept
	 *        per instance)
	 * @param marginGui space kept free on each side of the box, for the outline
	 * @param outline whether to outline the item at all
	 */
	public static void draw(GuiGraphics g, int x0, int y0, int x1, int y1, ItemStack stack, GlintsmithConfig snapshot,
		float yawDeg, float pitchDeg, float marginGui, boolean outline) {
		if (disabled || stack == null || stack.isEmpty() || x1 - x0 < MIN_BOX || y1 - y0 < MIN_BOX) {
			return;
		}
		try {
			drawItem(g, x0, y0, x1, y1, stack, snapshot, yawDeg, pitchDeg, marginGui, outline);
		} catch (Throwable t) {
			disabled = true;
			LOGGER.error("[Glintsmith] Preview disabled for this session after an error", t);
		}
	}

	/**
	 * Drops the cached item bounds (for example when the settings screen closes).
	 */
	public static void clearCache() {
		BOUNDS.clear();
	}

	private static void drawItem(GuiGraphics g, int x0, int y0, int x1, int y1, ItemStack stack, GlintsmithConfig snapshot,
		float yawDeg, float pitchDeg, float marginGui, boolean outline) {
		Minecraft minecraft = Minecraft.getInstance();
		ItemRenderer itemRenderer = minecraft.getItemRenderer();
		BakedModel model = itemRenderer.getModel(stack, minecraft.level, minecraft.player, 0);

		ItemDisplayContext context = ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
		ItemTransform cancel = model.getTransforms().getTransform(context);
		if (!PreviewPose.invertible(cancel)) {
			// A degenerate first-person transform cannot be undone: show the untransformed model instead.
			context = ItemDisplayContext.NONE;
			cancel = null;
		}
		float[] b = bounds(stack, model, context, cancel);
		PreviewPose.Fit fit = PreviewPose.fit(b[0], b[1], b[2], b[3], b[4], b[5], x1 - x0, y1 - y0, marginGui, pitchDeg,
			MAX_DEPTH);

		g.flush();
		// Our own pose stack, seeded with the GUI pose: an item renderer that throws halfway cannot unbalance g.pose().
		PoseStack pose = new PoseStack();
		pose.mulPose(g.pose().last().pose());
		pose.translate((x0 + x1) * 0.5f, (y0 + y1) * 0.5f, CENTER_Z);
		PreviewPose.turntable(pose, fit, yawDeg, pitchDeg);
		if (cancel != null) {
			PreviewPose.applyInverse(cancel, pose);
		}

		g.enableScissor(x0, y0, x1, y1);
		try {
			Lighting.setupForEntityInInventory();
			MultiBufferSource.BufferSource source = sourceFor(g);
			beginScopes(snapshot);
			boolean outlined = false;
			try {
				outlined = outline && OutlineSystem.beginPreview(snapshot);
				itemRenderer.render(stack, context, false, pose, source, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
					model);
				RenderSystem.disableDepthTest();
				source.endBatch();
				RenderSystem.enableDepthTest();
			} finally {
				if (outlined) {
					OutlineSystem.endPreview();
				}
				endScopes();
			}
		} finally {
			Lighting.setupFor3DItems();
			g.disableScissor();
		}
	}

	/**
	 * The buffer source the preview item is drawn into and flushed from: the GUI's own, which is the game's main
	 * buffer source.
	 */
	private static MultiBufferSource.BufferSource sourceFor(GuiGraphics g) {
		return g.bufferSource();
	}

	/**
	 * Scopes everything that resolves looks to {@code snapshot} for the preview render. Undone by {@link #endScopes()}.
	 */
	private static void beginScopes(GlintsmithConfig snapshot) {
		RuleEngine.beginScope(snapshot);
	}

	private static void endScopes() {
		RuleEngine.endScope();
	}

	/**
	 * Cached bounds of a model in a context. While an outline phase is open (it should not be, outside of item
	 * renders), the unit cube is used for this frame instead of a measuring render that would be captured.
	 */
	private static float[] bounds(ItemStack stack, BakedModel model, ItemDisplayContext context, @Nullable ItemTransform cancel) {
		BoundsKey key = new BoundsKey(stack.getItem(), model, context);
		float[] cached = BOUNDS.get(key);
		if (cached != null) {
			return cached;
		}
		if (OutlineSystem.phaseOpen()) {
			return PreviewBounds.unitCube();
		}
		float[] measured = PreviewBounds.measure(stack, model, context, cancel);
		if (BOUNDS.size() >= MAX_BOUNDS) {
			BOUNDS.clear();
		}
		BOUNDS.put(key, measured);
		return measured;
	}

	/** Bounds cache key; the model compares by identity (an override can pick another model for the same item). */
	private record BoundsKey(Item item, BakedModel model, ItemDisplayContext context) {
		@Override
		public boolean equals(Object o) {
			return o instanceof BoundsKey k && item == k.item && model == k.model && context == k.context;
		}

		@Override
		public int hashCode() {
			int h = System.identityHashCode(item);
			h = 31 * h + System.identityHashCode(model);
			return 31 * h + context.hashCode();
		}
	}
}
