package navrelis.glintsmith.gui.preview;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import navrelis.glintsmith.config.GlintsmithConfig;
import navrelis.glintsmith.style.Rule;
import org.jetbrains.annotations.Nullable;

/**
 * Writes the unsaved edit of a sub screen into a config for the duration of one call. The preview uses it to see
 * "root working copy plus every pending edit"; afterwards the config is exactly as it was. Pure Java.
 */
public final class PendingEdits {
	private PendingEdits() {
	}

	/**
	 * Runs {@code action} with {@code edited} in place of {@code original} in {@code cfg.rules} (appended when the
	 * original is not in the list). The list is swapped for a new one, so the old list object is never touched and
	 * is put back afterwards, also when the action throws.
	 */
	public static void withRule(GlintsmithConfig cfg, @Nullable Rule original, Rule edited, Runnable action) {
		List<Rule> before = cfg.rules;
		List<Rule> swapped = new ArrayList<>(before == null ? List.of() : before);
		int index = -1;
		for (int i = 0; i < swapped.size() && original != null; i++) {
			if (swapped.get(i) == original) {
				index = i;
				break;
			}
		}
		if (index >= 0) {
			swapped.set(index, edited);
		} else {
			swapped.add(edited);
		}
		cfg.rules = swapped;
		try {
			action.run();
		} finally {
			cfg.rules = before;
		}
	}

	/** Runs {@code action} with {@code pending} stored through {@code set}, and puts the old value back afterwards. */
	public static <T> void withValue(Supplier<T> get, Consumer<T> set, T pending, Runnable action) {
		T before = get.get();
		set.accept(pending);
		try {
			action.run();
		} finally {
			set.accept(before);
		}
	}
}
