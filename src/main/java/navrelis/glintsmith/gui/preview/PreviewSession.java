package navrelis.glintsmith.gui.preview;

import java.util.List;

import navrelis.glintsmith.config.GlintsmithConfig;
import navrelis.glintsmith.render.outline.OutlineFailsafe;
import navrelis.glintsmith.render.outline.OutlineRuntimeSettings;
import navrelis.glintsmith.render.outline.OutlineSystem;
import navrelis.glintsmith.render.preview.PreviewRenderer;
import navrelis.glintsmith.style.GlintCategory;
import navrelis.glintsmith.style.ItemFacts;
import navrelis.glintsmith.style.OutlineLook;
import navrelis.glintsmith.style.OutlineTarget;
import navrelis.glintsmith.style.RuleEngine;
import navrelis.glintsmith.style.RuleMatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

/**
 * The state of the preview that outlives single sub screens: view angles, what is shown and the notes about it.
 * One per root settings screen, shared with every sub screen opened from it, so the item keeps turning smoothly
 * while the player moves through the screens.
 */
public final class PreviewSession {
	/** Where the previewed item comes from. */
	public enum Source {
		HELD,
		SAMPLE
	}

	public static final float DEFAULT_PITCH = 15.0f;
	private static final float TURN_DEGREES_PER_SECOND = 30.0f;
	private static final float DRAG_DEGREES_PER_PIXEL = 1.5f;
	private static final float MAX_PITCH = 60.0f;
	private static final long MAX_STEP_MS = 100L;
	private static final long RESUME_AFTER_MS = 1500L;

	/** Kept for the whole game session: the choice survives closing and reopening the screen. */
	public static Source source = Source.HELD;

	public final PreviewSnapshot snap = new PreviewSnapshot();
	public float yaw;
	public float pitch = DEFAULT_PITCH;
	public boolean dragging;
	private long releasedAt = Long.MIN_VALUE / 2;
	private long lastMillis = -1L;

	@Nullable
	private GlintsmithConfig notesConfig;
	@Nullable
	private ItemStack notesStack;
	private boolean notesShaderPack;
	private boolean notesFailsafe;
	private boolean notesNoWorld;
	private boolean notesEmptyHand;
	private List<String> notes = List.of();
	@Nullable
	private OutlineLook outlineLook;
	@Nullable
	private OutlineRuntimeSettings outlineSettings;

	private boolean emptyHandFallback;
	private boolean noWorld;

	/** Advances the idle turn; call once per frame. */
	public void tick(long now) {
		long dt = lastMillis < 0L ? 0L : Math.min(Math.max(now - lastMillis, 0L), MAX_STEP_MS);
		lastMillis = now;
		if (!dragging && now - releasedAt > RESUME_AFTER_MS) {
			yaw = (yaw + TURN_DEGREES_PER_SECOND * dt / 1000.0f) % 360.0f;
		}
	}

	public void press() {
		dragging = true;
	}

	/** Turns the item by a mouse movement in GUI pixels. */
	public void drag(double dx, double dy) {
		yaw = (yaw + (float) (DRAG_DEGREES_PER_PIXEL * dx)) % 360.0f;
		pitch = Mth.clamp(pitch + (float) dy, -MAX_PITCH, MAX_PITCH);
	}

	public void release(long now) {
		dragging = false;
		releasedAt = now;
	}

	public void resetView() {
		yaw = 0.0f;
		pitch = DEFAULT_PITCH;
	}

	public void toggleSource() {
		source = source == Source.HELD ? Source.SAMPLE : Source.HELD;
	}

	/** The stack to draw: the held item if chosen and present, otherwise the sample sword. */
	public ItemStack shownStack(Minecraft mc) {
		emptyHandFallback = false;
		noWorld = false;
		if (source == Source.HELD) {
			ItemStack held = PreviewItems.held(mc);
			if (!held.isEmpty()) {
				return held;
			}
			emptyHandFallback = true;
		}
		PreviewItems.Sample sample = PreviewItems.sample(mc);
		noWorld = sample.noWorld();
		return sample.stack();
	}

	/**
	 * The notes about {@code shown} under {@code snapshot} (translation keys, most important first). Recomputed
	 * only when the snapshot, the stack or the surrounding state changed.
	 */
	public List<String> notes(ItemStack shown, GlintsmithConfig snapshot) {
		boolean shaderPack = OutlineSystem.shaderPackActive();
		boolean failsafe = OutlineFailsafe.tripped() || !PreviewRenderer.available();
		if (notesConfig != snapshot || notesStack == null || notesShaderPack != shaderPack || notesFailsafe != failsafe
			|| notesNoWorld != noWorld || notesEmptyHand != emptyHandFallback
			|| !ItemStack.matches(notesStack, shown)) {
			ItemFacts facts = RuleEngine.factsOf(shown);
			notes = PreviewNotes.keys(snapshot, facts, shown.is(Items.FILLED_MAP), shaderPack, noWorld,
				emptyHandFallback, failsafe);
			outlineLook = RuleMatcher.resolve(facts, OutlineTarget.FIRST_PERSON, GlintCategory.ITEM, snapshot).outline();
			outlineSettings = OutlineRuntimeSettings.from(snapshot);
			notesConfig = snapshot;
			notesStack = shown.copy();
			notesShaderPack = shaderPack;
			notesFailsafe = failsafe;
			notesNoWorld = noWorld;
			notesEmptyHand = emptyHandFallback;
		}
		return notes;
	}

	/**
	 * Free space the renderer keeps around the item for the outline, in GUI pixels:
	 * {@code ceil(reach / guiScale) + 2}, or 2 without an outline. {@code reach} is
	 * {@link OutlineRuntimeSettings#outlineReachPixels}: the outline's extent in framebuffer pixels with the thickness
	 * factor every phase uses, quantised and capped like the composite. Call after {@link #notes}.
	 */
	public float marginGui(int framebufferHeight, double guiScale) {
		if (outlineLook == null || outlineSettings == null || !(guiScale > 0.0) || !Double.isFinite(guiScale)) {
			return 2.0f;
		}
		int reach = outlineSettings.outlineReachPixels(outlineLook.thickness(), outlineLook.softness(), framebufferHeight);
		return (float) Math.ceil(reach / guiScale) + 2.0f;
	}
}
