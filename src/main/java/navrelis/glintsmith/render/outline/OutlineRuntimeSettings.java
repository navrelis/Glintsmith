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

	/** Reference framebuffer height for {@code scaleWithResolution}: a thickness is in pixels at this height. */
	public static final float REFERENCE_HEIGHT = 1080.0f;

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
	 * The factor applied to the thickness of a look before it goes into the style table. It is the same for every
	 * phase (world, first-person hand, GUI and the settings preview), because every phase composites onto the main
	 * target in its pixels: one thickness then has one on-screen width everywhere.
	 * <p>
	 * With {@code scaleWithResolution} a thickness is in pixels at a framebuffer height of {@link #REFERENCE_HEIGHT}
	 * ({@code framebufferHeight / 1080}); without it, in framebuffer pixels (1.0). The GUI scale plays no part on
	 * purpose: world and hand outlines cannot follow it, so a GUI-only factor would make GUI outlines wider or
	 * narrower than the same look in the world. The table then caps the outline at {@link StyleTable#MAX_RADIUS}
	 * pixels.
	 *
	 * @param framebufferHeight height of the main render target in pixels; 0 or less (minimised window) gives 1.0
	 */
	public float thicknessScale(int framebufferHeight) {
		if (!scaleWithResolution || framebufferHeight <= 0) {
			return 1.0f;
		}
		return framebufferHeight / REFERENCE_HEIGHT;
	}

	/**
	 * How far the outline of a look reaches beyond the item's silhouette, in framebuffer pixels: the search radius the
	 * style table gives it after scaling, quantising and capping, exactly as the composite sees it. For layout code
	 * that must keep room for the outline (the settings preview).
	 *
	 * @param thickness the look's thickness, before {@link #thicknessScale}
	 * @param softness the look's softness, 0..1
	 * @param framebufferHeight height of the main render target in pixels
	 * @return 1..{@link StyleTable#MAX_RADIUS}
	 */
	public int outlineReachPixels(float thickness, float softness, int framebufferHeight) {
		return StyleTable.radiusOf(StyleTable.packParams(thickness * thicknessScale(framebufferHeight), softness));
	}
}
