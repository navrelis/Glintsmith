package navrelis.glintsmith.gui.preview;

import navrelis.glintsmith.config.ConfigManager;
import navrelis.glintsmith.config.GlintsmithConfig;
import org.jetbrains.annotations.Nullable;

/**
 * The config the preview renders with: a detached, sanitised copy of "working config plus pending edits", rebuilt
 * only when its JSON changes. The renderer keys its caches by the identity of the returned instance, so equal
 * content must give the same instance. Pure Java.
 */
public final class PreviewSnapshot {
	@Nullable
	private String json;
	@Nullable
	private GlintsmithConfig cached;

	/**
	 * @param live the working config as it is right now (may be unsanitised)
	 * @return the snapshot for it; the previous one if the config cannot be serialised
	 */
	public GlintsmithConfig update(GlintsmithConfig live) {
		try {
			String now = ConfigManager.toJson(live);
			if (cached != null && now.equals(json)) {
				return cached;
			}
			GlintsmithConfig next = ConfigManager.fromJson(now).sanitize();
			json = now;
			cached = next;
			return next;
		} catch (RuntimeException e) {
			if (cached != null) {
				return cached;
			}
			try {
				cached = live.copy();
			} catch (RuntimeException second) {
				cached = new GlintsmithConfig().sanitize();
			}
			json = null;
			return cached;
		}
	}
}
