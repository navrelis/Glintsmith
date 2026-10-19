package navrelis.glintsmith.platform;

import java.util.ServiceLoader;

/**
 * Loader-neutral access to the loader's {@link Platform} service. The implementation is looked up once, when this
 * class is first used, so a build that lacks one fails right there with a clear message.
 */
public final class Services {
	public static final Platform PLATFORM = load(Platform.class);

	private Services() {
	}

	public static <T> T load(Class<T> type) {
		return ServiceLoader.load(type, Services.class.getClassLoader()).findFirst()
			.orElseThrow(() -> new IllegalStateException("Glintsmith: no implementation of " + type.getName()
				+ " found; the loader build must register one in META-INF/services/" + type.getName()));
	}
}
