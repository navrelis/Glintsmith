package navrelis.glintsmith.style;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;

/**
 * A compiled id pattern for items and enchantments.
 * <ul>
 * <li>{@code minecraft:diamond_sword}: exact id. Ids without a namespace default to {@code minecraft:}.</li>
 * <li>{@code #minecraft:swords}: tag (item tags for items, enchantment tags for enchantments).</li>
 * <li>{@code @modid}: any id in that namespace.</li>
 * <li>{@code *:*_sword}, {@code mymod:*}: glob, {@code *} is any characters, {@code ?} one character.
 * A glob without namespace also defaults to {@code minecraft:}; write {@code *:name} for every namespace.</li>
 * <li>{@code *}: everything.</li>
 * <li>A leading {@code !} negates the pattern.</li>
 * </ul>
 * For enchantments a pattern is tested against the whole set: it matches when <em>any</em> enchantment on the stack
 * matches (tags: when any enchantment tag is present), so {@code !minecraft:mending} means "has no mending".
 * <p>
 * In a list of patterns, positive and negative ones combine as {@link #matchesList}: the list matches when no
 * negated pattern is violated and, if there are positive patterns, at least one of them matches.
 */
public final class Pattern {
	private enum Kind {
		ALL, EXACT, TAG, NAMESPACE, GLOB
	}

	private final String source;
	private final boolean negated;
	private final Kind kind;
	/** Exact id, tag id, or namespace, depending on the kind. */
	private final String value;
	private final java.util.regex.Pattern glob;

	private Pattern(String source, boolean negated, Kind kind, String value, java.util.regex.Pattern glob) {
		this.source = source;
		this.negated = negated;
		this.kind = kind;
		this.value = value;
		this.glob = glob;
	}

	/**
	 * @return the compiled pattern, or {@code null} if the text is not valid. Never throws.
	 */
	public static Pattern parse(String text) {
		if (text == null) {
			return null;
		}
		String s = text.trim().toLowerCase(Locale.ROOT);
		boolean negated = false;
		if (s.startsWith("!")) {
			negated = true;
			s = s.substring(1).trim();
		}
		if (s.isEmpty()) {
			return null;
		}
		if (s.equals("*")) {
			return new Pattern(text, negated, Kind.ALL, "*", null);
		}
		if (s.charAt(0) == '#') {
			String tag = withNamespace(s.substring(1));
			return isPlainId(tag) ? new Pattern(text, negated, Kind.TAG, tag, null) : null;
		}
		if (s.charAt(0) == '@') {
			String ns = s.substring(1);
			return !ns.isEmpty() && isPlainNamespace(ns) ? new Pattern(text, negated, Kind.NAMESPACE, ns, null) : null;
		}
		String id = withNamespace(s);
		if (id.indexOf('*') >= 0 || id.indexOf('?') >= 0) {
			if (!isGlob(id)) {
				return null;
			}
			return new Pattern(text, negated, Kind.GLOB, id, compileGlob(id));
		}
		return isPlainId(id) ? new Pattern(text, negated, Kind.EXACT, id, null) : null;
	}

	/**
	 * @return the text this pattern was parsed from
	 */
	public String source() {
		return source;
	}

	public boolean negated() {
		return negated;
	}

	/**
	 * Tests an item, negation included.
	 */
	public boolean matchesItem(ItemFacts facts) {
		return raw(facts.itemId(), facts.itemTags()) != negated;
	}

	/**
	 * Tests the enchantments of a stack (see the class description), negation included.
	 */
	public boolean matchesEnchantments(ItemFacts facts) {
		boolean hit = false;
		if (kind == Kind.TAG) {
			hit = facts.enchantmentTags().contains(value);
		} else {
			for (String id : facts.enchantments()) {
				if (raw(id, Set.of())) {
					hit = true;
					break;
				}
			}
		}
		return hit != negated;
	}

	/**
	 * Combines a list: every negated pattern must hold, and if there is any positive pattern, one of them must hold.
	 * An empty list matches.
	 *
	 * @param test applies one pattern, negation included (e.g. {@code p -> p.matchesItem(facts)})
	 */
	public static boolean matchesList(List<Pattern> patterns, Predicate<Pattern> test) {
		boolean hasPositive = false;
		boolean positiveHit = false;
		for (Pattern p : patterns) {
			if (p.negated) {
				if (!test.test(p)) {
					return false;
				}
			} else {
				hasPositive = true;
				if (!positiveHit && test.test(p)) {
					positiveHit = true;
				}
			}
		}
		return !hasPositive || positiveHit;
	}

	@Override
	public String toString() {
		return source;
	}

	private boolean raw(String id, Set<String> tags) {
		return switch (kind) {
			case ALL -> true;
			case EXACT -> value.equals(id);
			case TAG -> tags.contains(value);
			case NAMESPACE -> id.length() > value.length() && id.charAt(value.length()) == ':' && id.startsWith(value);
			case GLOB -> glob.matcher(id).matches();
		};
	}

	private static String withNamespace(String id) {
		return id.indexOf(':') < 0 ? "minecraft:" + id : id;
	}

	private static boolean isPlainNamespace(String ns) {
		if (ns.isEmpty()) {
			return false;
		}
		for (int i = 0; i < ns.length(); i++) {
			char c = ns.charAt(i);
			if (!(c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '_' || c == '-' || c == '.')) {
				return false;
			}
		}
		return true;
	}

	private static boolean isPlainId(String id) {
		int colon = id.indexOf(':');
		if (colon <= 0 || colon != id.lastIndexOf(':') || colon == id.length() - 1) {
			return false;
		}
		if (!isPlainNamespace(id.substring(0, colon))) {
			return false;
		}
		for (int i = colon + 1; i < id.length(); i++) {
			char c = id.charAt(i);
			if (!(c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '_' || c == '-' || c == '.' || c == '/')) {
				return false;
			}
		}
		return true;
	}

	private static boolean isGlob(String id) {
		int colon = id.indexOf(':');
		if (colon != id.lastIndexOf(':')) {
			return false;
		}
		for (int i = 0; i < id.length(); i++) {
			char c = id.charAt(i);
			if (!(c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '_' || c == '-' || c == '.' || c == '/'
				|| c == ':' || c == '*' || c == '?')) {
				return false;
			}
		}
		return true;
	}

	private static java.util.regex.Pattern compileGlob(String glob) {
		StringBuilder regex = new StringBuilder();
		for (int i = 0; i < glob.length(); i++) {
			char c = glob.charAt(i);
			switch (c) {
				case '*' -> regex.append(".*");
				case '?' -> regex.append('.');
				default -> regex.append(java.util.regex.Pattern.quote(String.valueOf(c)));
			}
		}
		return java.util.regex.Pattern.compile(regex.toString());
	}
}
