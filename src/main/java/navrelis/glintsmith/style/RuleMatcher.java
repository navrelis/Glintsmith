package navrelis.glintsmith.style;

import java.util.List;

import navrelis.glintsmith.config.GlintsmithConfig;
import navrelis.glintsmith.config.OutlineCondition;

/**
 * Turns {@link ItemFacts} plus a config into a {@link ResolvedStyle}. Pure Java.
 * <h2>Resolution</h2>
 * Start from the global defaults, then walk {@link GlintsmithConfig#rules} <b>in order</b>. Every matching rule overlays
 * its non-null fields on the result, so later rules override earlier ones. A "deny" is therefore just a rule with
 * {@code outline.enabled = false} placed after the broader rule that allows.
 * <h3>Outline</h3>
 * <ul>
 * <li>The master switch {@code outline.enabled} and the per-target flags {@code outline.targets} are hard limits:
 * when they say no, there is no outline and no rule can bring it back.</li>
 * <li>The base {@code enabled} comes from {@code outline.condition}: ALL, NONE, or ENCHANTED (the stack has
 * enchantments or shows the vanilla glint). Rules overlay on that.</li>
 * <li>Colour, thickness, softness and opacity start from the global values and are clamped at the end. The thickness
 * starts from the own thickness of the target ({@code outline.targetThickness}) if it has one, else from the global
 * {@code outline.thickness}; a rule that sets a thickness overrides either.</li>
 * </ul>
 * <h3>Glint</h3>
 * <ul>
 * <li>{@code glint.enabled} is a master switch: off means {@code null} (leave the glint alone).</li>
 * <li>Otherwise the settings of the {@code category} are the base, rule overrides overlay on them.
 * {@code enabled = false} hides the glint, a texture of {@link GlintSettings#VANILLA_TEXTURE} resets to vanilla.</li>
 * <li>If the end result equals the vanilla look, the result is {@code null} so the renderer can skip all work.</li>
 * </ul>
 * Rules with an invalid pattern or that are disabled never match. This class never throws on bad data.
 */
public final class RuleMatcher {
	private RuleMatcher() {
	}

	/**
	 * @param facts what is being drawn
	 * @param target where it is drawn
	 * @param category the glint layer being drawn
	 * @param config a sanitised config
	 * @return never null
	 */
	public static ResolvedStyle resolve(ItemFacts facts, OutlineTarget target, GlintCategory category, GlintsmithConfig config) {
		if (facts == null || config == null || target == null || category == null) {
			return ResolvedStyle.NONE;
		}
		List<Rule> rules = config.rules != null ? config.rules : List.of();

		// Outline base.
		GlintsmithConfig.Outline global = config.outline;
		boolean outlineAllowed = global != null && global.enabled && (global.targets == null || global.targets.allows(target));
		boolean outlineEnabled = false;
		ColorStyle color = null;
		float thickness = 2.0f;
		float softness = 0.0f;
		float opacity = 1.0f;
		if (outlineAllowed) {
			OutlineCondition condition = global.condition == null ? OutlineCondition.ENCHANTED : global.condition;
			outlineEnabled = switch (condition) {
				case ALL -> true;
				case NONE -> false;
				case ENCHANTED -> facts.enchanted() || facts.hasFoil();
			};
			color = global.color;
			Float own = global.targetThickness == null ? null : global.targetThickness.get(target);
			thickness = own != null ? own : global.thickness;
			softness = global.softness;
			opacity = global.opacity;
		}

		// Glint base.
		GlintsmithConfig.Glint glintGlobal = config.glint;
		boolean glintActive = glintGlobal != null && glintGlobal.enabled && glintGlobal.forCategory(category) != null;
		boolean glintEnabled = true;
		String texture = null;
		ColorStyle tint = null;
		float speed = 1.0f;
		float scale = 1.0f;
		float strength = 1.0f;
		float rotation = 10.0f;
		if (glintActive) {
			GlintsmithConfig.GlintCategorySettings base = glintGlobal.forCategory(category);
			glintEnabled = base.enabled;
			texture = GlintSettings.isVanillaTexture(base.texture) ? null : base.texture;
			tint = base.tint;
			speed = base.speed;
			scale = base.scale;
			strength = base.strength;
			rotation = base.rotation;
		}

		for (Rule rule : rules) {
			if (rule == null || !rule.matches(facts, target)) {
				continue;
			}
			OutlineSettings o = rule.outline;
			if (outlineAllowed && o != null) {
				if (o.enabled != null) {
					outlineEnabled = o.enabled;
				}
				if (o.color != null) {
					color = o.color;
				}
				if (o.thickness != null) {
					thickness = o.thickness;
				}
				if (o.softness != null) {
					softness = o.softness;
				}
				if (o.opacity != null) {
					opacity = o.opacity;
				}
			}
			GlintSettings g = rule.glint;
			if (glintActive && g != null) {
				if (g.enabled != null) {
					glintEnabled = g.enabled;
				}
				if (g.texture != null) {
					texture = GlintSettings.isVanillaTexture(g.texture) ? null : g.texture;
				}
				if (g.tint != null) {
					tint = g.tint;
				}
				if (g.speed != null) {
					speed = g.speed;
				}
				if (g.scale != null) {
					scale = g.scale;
				}
				if (g.strength != null) {
					strength = g.strength;
				}
				if (g.rotation != null) {
					rotation = g.rotation;
				}
			}
		}

		OutlineLook outlineLook = null;
		if (outlineAllowed && outlineEnabled) {
			ColorStyle style = color != null ? color : ColorStyle.of(ColorStyle.DEFAULT_COLOR);
			outlineLook = new OutlineLook(
				ColorEvaluator.compile(style),
				style,
				Num.clamp(thickness, OutlineSettings.MIN_THICKNESS, OutlineSettings.MAX_THICKNESS, 2.0f),
				Num.clamp(softness, 0.0f, 1.0f, 0.0f),
				Num.clamp(opacity, 0.0f, 1.0f, 1.0f)
			);
		}

		GlintLook glintLook = null;
		if (glintActive) {
			speed = Num.clamp(speed, 0.0f, 10.0f, 1.0f);
			scale = Num.clamp(scale, 0.1f, 10.0f, 1.0f);
			strength = Num.clamp(strength, 0.0f, 1.0f, 1.0f);
			rotation = Num.clamp(rotation, -180.0f, 180.0f, 10.0f);
			boolean vanilla = glintEnabled && texture == null && tint == null
				&& speed == 1.0f && scale == 1.0f && strength == 1.0f && rotation == 10.0f;
			if (!vanilla) {
				glintLook = new GlintLook(
					glintEnabled,
					texture,
					tint == null ? null : ColorEvaluator.compile(tint),
					tint,
					speed, scale, strength, rotation
				);
			}
		}

		if (outlineLook == null && glintLook == null) {
			return ResolvedStyle.NONE;
		}
		return new ResolvedStyle(outlineLook, glintLook);
	}
}
