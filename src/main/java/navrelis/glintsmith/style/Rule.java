package navrelis.glintsmith.style;

import java.util.ArrayList;
import java.util.List;

/**
 * One user rule. A rule matches when it is enabled, valid, and
 * <ul>
 * <li>{@code items} is empty or matches the item ({@link Pattern#matchesList}),</li>
 * <li>no {@code excludeItems} pattern matches,</li>
 * <li>{@code enchantments} is empty or matches the enchantments on the stack,</li>
 * <li>{@code enchantedCondition} holds, and</li>
 * <li>{@code targets} is empty or contains the current target.</li>
 * </ul>
 * A matching rule overlays its non-null {@code outline} and {@code glint} fields on the result;
 * see {@link RuleMatcher} for the order. A rule with an invalid pattern gets {@link #error} set and never matches,
 * but stays in the file so the user can fix it.
 */
public final class Rule {
	/** Constraint on whether the stack has enchantments. */
	public enum Condition {
		ANY,
		ENCHANTED,
		NOT_ENCHANTED
	}

	public String name = "";
	public boolean enabled = true;
	public List<String> items = new ArrayList<>();
	public List<String> excludeItems = new ArrayList<>();
	/** Match if any of these is present on the stack; empty means no constraint. */
	public List<String> enchantments = new ArrayList<>();
	public Condition enchantedCondition = Condition.ANY;
	/** Empty means all targets. */
	public List<OutlineTarget> targets = new ArrayList<>();
	/** Null means "no change". */
	public OutlineSettings outline;
	/** Null means "no change". */
	public GlintSettings glint;

	/** Validation message of the last {@link #sanitize()}, or null if the rule is valid. Not saved. */
	public transient String error;

	private transient List<Pattern> itemPatterns;
	private transient List<Pattern> excludePatterns;
	private transient List<Pattern> enchantmentPatterns;

	public Rule() {
	}

	/**
	 * @return an independent deep copy (not yet compiled; call {@link #sanitize()})
	 */
	public Rule copy() {
		Rule c = new Rule();
		c.name = name;
		c.enabled = enabled;
		c.items = items == null ? null : new ArrayList<>(items);
		c.excludeItems = excludeItems == null ? null : new ArrayList<>(excludeItems);
		c.enchantments = enchantments == null ? null : new ArrayList<>(enchantments);
		c.enchantedCondition = enchantedCondition;
		c.targets = targets == null ? null : new ArrayList<>(targets);
		c.outline = outline == null ? null : outline.copy();
		c.glint = glint == null ? null : glint.copy();
		return c;
	}

	/**
	 * Replaces nulls by defaults, sanitises the overrides and compiles the patterns, setting {@link #error}
	 * to the first invalid pattern (or null).
	 *
	 * @return {@code this}
	 */
	public Rule sanitize() {
		if (name == null) {
			name = "";
		}
		if (enchantedCondition == null) {
			enchantedCondition = Condition.ANY;
		}
		if (targets == null) {
			targets = new ArrayList<>();
		} else {
			targets = new ArrayList<>(targets);
			targets.removeIf(t -> t == null);
		}
		if (outline != null) {
			outline.sanitize();
		}
		if (glint != null) {
			glint.sanitize();
		}
		compile();
		return this;
	}

	private void compile() {
		String problem = null;
		List<Pattern> itemList = new ArrayList<>();
		List<Pattern> excludeList = new ArrayList<>();
		List<Pattern> enchantmentList = new ArrayList<>();
		items = cleaned(items);
		excludeItems = cleaned(excludeItems);
		enchantments = cleaned(enchantments);
		problem = compileInto(items, itemList, "item", problem);
		problem = compileInto(excludeItems, excludeList, "excluded item", problem);
		problem = compileInto(enchantments, enchantmentList, "enchantment", problem);
		itemPatterns = itemList;
		excludePatterns = excludeList;
		enchantmentPatterns = enchantmentList;
		error = problem;
	}

	private static List<String> cleaned(List<String> in) {
		List<String> out = new ArrayList<>();
		if (in != null) {
			for (String s : in) {
				if (s != null) {
					out.add(s);
				}
			}
		}
		return out;
	}

	private static String compileInto(List<String> source, List<Pattern> target, String what, String problem) {
		for (String text : source) {
			Pattern p = Pattern.parse(text);
			if (p == null) {
				if (problem == null) {
					problem = "Invalid " + what + " pattern '" + text + "'";
				}
			} else {
				target.add(p);
			}
		}
		return problem;
	}

	/**
	 * @return {@code true} if the rule is enabled and all its patterns are valid
	 */
	public boolean isActive() {
		if (!enabled) {
			return false;
		}
		if (itemPatterns == null) {
			sanitize();
		}
		return error == null;
	}

	/**
	 * Tests all conditions of this rule. Never throws.
	 */
	public boolean matches(ItemFacts facts, OutlineTarget target) {
		if (!isActive()) {
			return false;
		}
		if (!targets.isEmpty() && !targets.contains(target)) {
			return false;
		}
		switch (enchantedCondition) {
			case ENCHANTED:
				if (!facts.enchanted()) {
					return false;
				}
				break;
			case NOT_ENCHANTED:
				if (facts.enchanted()) {
					return false;
				}
				break;
			case ANY:
			default:
				break;
		}
		if (!itemPatterns.isEmpty() && !Pattern.matchesList(itemPatterns, p -> p.matchesItem(facts))) {
			return false;
		}
		for (Pattern p : excludePatterns) {
			if (p.matchesItem(facts)) {
				return false;
			}
		}
		return enchantmentPatterns.isEmpty() || Pattern.matchesList(enchantmentPatterns, p -> p.matchesEnchantments(facts));
	}
}
