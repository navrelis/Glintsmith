package navrelis.glintsmith.render.outline;

import navrelis.glintsmith.config.GlintsmithConfig;
import navrelis.glintsmith.style.OutlineTarget;

/**
 * Immutable snapshot of the outline config the renderer needs per frame. This is the only class that reads
 * {@code config.outline.*}; it is rebuilt whenever the config changes.
 * <p>
 * Colour, thickness, softness, opacity and the outline condition are not here: the rule engine resolves those per
 * stack.
 */
public record OutlineRuntimeSettings(
	boolean enabled,
	boolean firstPerson,
	boolean thirdPerson,
	boolean armor,
	boolean dropped,
	boolean itemFrame,
	boolean gui,
	boolean other,
	boolean onlyLocalPlayer,
	boolean disableWithShaders,
	boolean handWithShaderPack,
	boolean scaleWithResolution,
	float fadeStart,
	float fadeEnd
) {
	/** Everything off. Used before the config is read and when it cannot be read. */
	public static final OutlineRuntimeSettings DISABLED = new OutlineRuntimeSettings(false, false, false, false, false,
		false, false, false, false, false, false, false, 0.0f, 0.0f);

	/** Reference framebuffer height for {@code scaleWithResolution} in the world and the hand. */
	public static final float REFERENCE_HEIGHT = 1080.0f;
	/** Reference GUI scale for {@code scaleWithResolution} in the GUI. */
	public static final double REFERENCE_GUI_SCALE = 2.0;

	/**
	 * Builds the snapshot. A missing config or outline section gives {@link #DISABLED}.
	 */
	public static OutlineRuntimeSettings from(GlintsmithConfig config) {
		if (config == null || config.outline == null) {
			return DISABLED;
		}
		GlintsmithConfig.Outline o = config.outline;
		GlintsmithConfig.Targets t = o.targets == null ? new GlintsmithConfig.Targets() : o.targets;
		float start = nonNegative(o.fadeStart);
		float end = nonNegative(o.fadeEnd);
		return new OutlineRuntimeSettings(o.enabled, t.firstPerson, t.thirdPerson, t.armor, t.dropped, t.itemFrame,
			t.gui, t.other, o.onlyLocalPlayer, o.disableWithShaders, o.handWithShaderPack, o.scaleWithResolution,
			start, end);
	}

	private static float nonNegative(float value) {
		return Float.isFinite(value) && value > 0.0f ? value : 0.0f;
	}

	/**
	 * @return whether the user enabled outlines for this target (the master switch is not checked here)
	 */
	public boolean allows(OutlineTarget target) {
		if (target == null) {
			return false;
		}
		return switch (target) {
			case FIRST_PERSON -> firstPerson;
			case THIRD_PERSON -> thirdPerson;
			case ARMOR -> armor;
			case DROPPED -> dropped;
			case ITEM_FRAME -> itemFrame;
			case GUI -> gui;
			case OTHER -> other;
		};
	}

	/**
	 * @return whether any target captured in the WORLD phase is enabled
	 */
	public boolean anyWorld() {
		return thirdPerson || armor || dropped || itemFrame || other;
	}

	/**
	 * @return whether an item of this target may be outlined while this phase is open, ignoring who holds it
	 */
	public boolean accepts(OutlinePhase phase, OutlineTarget target) {
		if (!enabled || !OutlineContexts.phaseAccepts(phase, target)) {
			return false;
		}
		return allows(target);
	}

	/**
	 * Like {@link #accepts(OutlinePhase, OutlineTarget)}, plus "only local player": held third-person items and armor
	 * of anyone but the local player are rejected when that option is on.
	 *
	 * @param localHolder whether the entity holding or wearing the item is the local player
	 */
	public boolean accepts(OutlinePhase phase, OutlineTarget target, boolean localHolder) {
		if (!accepts(phase, target)) {
			return false;
		}
		return localHolder || !onlyLocalPlayer || !holderRestricted(target);
	}

	/**
	 * @return whether "only local player" applies to this target
	 */
	public static boolean holderRestricted(OutlineTarget target) {
		return target == OutlineTarget.THIRD_PERSON || target == OutlineTarget.ARMOR;
	}

	/**
	 * @return whether the WORLD phase may open; world outlines are always off while a shader pack is active
	 */
	public boolean worldPhaseAllowed(boolean shaderPack) {
		return enabled && anyWorld() && !shaderPack;
	}

	/**
	 * @return whether the HAND phase may open; under a shader pack only with {@code handWithShaderPack} and without
	 *         {@code disableWithShaders}
	 */
	public boolean handPhaseAllowed(boolean shaderPack) {
		return enabled && firstPerson && (!shaderPack || (!disableWithShaders && handWithShaderPack));
	}

	/**
	 * @return whether the GUI phase may open; GUI outlines keep working under a shader pack unless
	 *         {@code disableWithShaders} is on
	 */
	public boolean guiPhaseAllowed(boolean shaderPack) {
		return enabled && gui && !(shaderPack && disableWithShaders);
	}

	/**
	 * @return whether the distance fade of world outlines is on
	 */
	public boolean fadeActive() {
		return fadeEnd > fadeStart && fadeEnd > 0.0f;
	}

	/**
	 * The factor applied to the thickness of a look before it goes into the style table. The table then caps the
	 * outline at {@link StyleTable#MAX_RADIUS} pixels.
	 *
	 * @param framebufferHeight height of the main render target in pixels
	 * @param guiScale the window's GUI scale
	 */
	public float thicknessScale(OutlinePhase phase, int framebufferHeight, double guiScale) {
		if (!scaleWithResolution) {
			return 1.0f;
		}
		if (phase == OutlinePhase.GUI) {
			return guiScale > 0.0 && Double.isFinite(guiScale) ? (float) (guiScale / REFERENCE_GUI_SCALE) : 1.0f;
		}
		return framebufferHeight > 0 ? framebufferHeight / REFERENCE_HEIGHT : 1.0f;
	}
}
