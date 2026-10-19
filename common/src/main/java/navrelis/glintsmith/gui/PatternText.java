package navrelis.glintsmith.gui;

import java.util.ArrayList;
import java.util.List;

import navrelis.glintsmith.style.Pattern;

/**
 * Helpers for the comma-separated pattern boxes of the rule editor. Pure Java.
 */
public final class PatternText {
	private PatternText() {
	}

	/**
	 * @return the non-empty, trimmed entries of a comma-separated list
	 */
	public static List<String> split(String text) {
		List<String> out = new ArrayList<>();
		if (text != null) {
			for (String part : text.split(",", -1)) {
				String trimmed = part.trim();
				if (!trimmed.isEmpty()) {
					out.add(trimmed);
				}
			}
		}
		return out;
	}

	/**
	 * @return the entries joined with {@code ", "}
	 */
	public static String join(List<String> entries) {
		return entries == null ? "" : String.join(", ", entries);
	}

	/**
	 * @return {@code [start, end)} character ranges of the entries that are not valid patterns
	 */
	public static List<int[]> invalidRanges(String text) {
		List<int[]> ranges = new ArrayList<>();
		if (text == null) {
			return ranges;
		}
		int start = 0;
		while (start <= text.length()) {
			int comma = text.indexOf(',', start);
			int end = comma < 0 ? text.length() : comma;
			int from = start;
			int to = end;
			while (from < to && Character.isWhitespace(text.charAt(from))) {
				from++;
			}
			while (to > from && Character.isWhitespace(text.charAt(to - 1))) {
				to--;
			}
			if (from < to && Pattern.parse(text.substring(from, to)) == null) {
				ranges.add(new int[] {from, to});
			}
			if (comma < 0) {
				break;
			}
			start = comma + 1;
		}
		return ranges;
	}

	/**
	 * @return {@code true} if every entry is a valid pattern (an empty text is valid)
	 */
	public static boolean allValid(String text) {
		return invalidRanges(text).isEmpty();
	}
}
