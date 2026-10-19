package navrelis.glintsmith.gui.preview;

import java.util.ArrayList;
import java.util.List;

import navrelis.glintsmith.config.GlintsmithConfig;
import navrelis.glintsmith.render.outline.OutlineRuntimeSettings;
import navrelis.glintsmith.style.GlintCategory;
import navrelis.glintsmith.style.ItemFacts;
import navrelis.glintsmith.style.OutlineTarget;
import navrelis.glintsmith.style.ResolvedStyle;
import navrelis.glintsmith.style.RuleMatcher;

/**
 * Why the preview may look different from what the player expects. Pure Java.
 */
public final class PreviewNotes {
	public static final String UNAVAILABLE = "glintsmith.preview.note.unavailable";
	public static final String OUTLINES_OFF = "glintsmith.preview.note.outlines_off";
	public static final String FIRST_PERSON_OFF = "glintsmith.preview.note.first_person_off";
	public static final String NO_OUTLINE = "glintsmith.preview.note.no_outline";
	public static final String MAP = "glintsmith.preview.note.map";
	public static final String SHADER_HAND_OFF = "glintsmith.preview.note.shader_hand_off";
	public static final String SHADER_GLINT = "glintsmith.preview.note.shader_glint";
	public static final String NO_WORLD = "glintsmith.preview.note.no_world";
	public static final String EMPTY_HAND = "glintsmith.preview.note.empty_hand";

	private PreviewNotes() {
	}

	/**
	 * The translation keys of all notes that apply, most important first; the panel shows the first one and lists
	 * the rest in its tooltip. The reasons for a missing outline exclude each other (the first that applies wins).
	 *
	 * @param snap the sanitised snapshot the preview is drawn with
	 * @param facts the previewed stack
	 * @param shaderPack whether a shader pack is active
	 * @param noWorld the sample sword had to be built without enchantments
	 * @param emptyHandFallback the sample is shown because the main hand is empty
	 * @param failsafe the outline renderer has switched itself off
	 */
	public static List<String> keys(GlintsmithConfig snap, ItemFacts facts, boolean filledMap, boolean shaderPack,
		boolean noWorld, boolean emptyHandFallback, boolean failsafe) {
		List<String> keys = new ArrayList<>();
		if (failsafe) {
			keys.add(UNAVAILABLE);
		}
		ResolvedStyle style = RuleMatcher.resolve(facts, OutlineTarget.FIRST_PERSON, GlintCategory.ITEM, snap);
		boolean outlineShown = true;
		if (!snap.outline.enabled) {
			keys.add(OUTLINES_OFF);
			outlineShown = false;
		} else if (!snap.outline.targets.firstPerson) {
			keys.add(FIRST_PERSON_OFF);
			outlineShown = false;
		} else if (style.outline() == null) {
			keys.add(NO_OUTLINE);
			outlineShown = false;
		} else if (filledMap) {
			keys.add(MAP);
			outlineShown = false;
		}
		if (shaderPack && outlineShown && !OutlineRuntimeSettings.from(snap).handPhaseAllowed(true)) {
			keys.add(SHADER_HAND_OFF);
		}
		if (shaderPack && style.glint() != null) {
			keys.add(SHADER_GLINT);
		}
		if (noWorld) {
			keys.add(NO_WORLD);
		}
		if (emptyHandFallback) {
			keys.add(EMPTY_HAND);
		}
		return keys;
	}
}
