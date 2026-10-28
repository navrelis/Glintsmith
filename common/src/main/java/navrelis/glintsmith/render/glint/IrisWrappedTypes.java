package navrelis.glintsmith.render.glint;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.IdentityHashMap;

import navrelis.glintsmith.Glintsmith;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.Nullable;

/**
 * Reflective access to Iris's {@code OuterWrappedRenderType} for G7 (render thread only). Iris is an optional mod, so
 * nothing here links against it: the class, its members and the {@code wrapExactlyOnce} factory are looked up from the
 * first instance that reaches {@link #isWrapper}.
 * <p>
 * Why: while a shader pack is active, the entity renderers draw through Iris's {@code BufferSourceWrapper}, which hands
 * every render type to the fully buffered source already wrapped in an {@code OuterWrappedRenderType} (the vanilla type
 * plus an extra state shard such as {@code iris:is_entity} that sets the pack's entity uniforms). G7 therefore sees
 * {@code iris_wrapped:RenderType[armor_entity_glint]} instead of the vanilla type, which {@link GlintKind#of} does not
 * know. G7 unwraps it, swaps the vanilla type for a slot type, and puts the same extra shard around the slot type again
 * ({@link #rewrap}); the wrapper delegates {@code setupRenderState}, {@code clearRenderState}, drawing and the
 * transparency class to the wrapped type, so the slot's own shards still run.
 * <p>
 * Any failure (a renamed member, a module that denies {@code setAccessible}) switches this helper off for the rest of
 * the session with one warning; G7 then leaves wrapped types vanilla.
 */
final class IrisWrappedTypes {
	private static final String WRAPPER = "net.irisshaders.iris.layer.OuterWrappedRenderType";
	/** The name given to the wrappers made here; it only shows in logs. */
	private static final String NAME = "glintsmith:iris_wrapped";

	@Nullable
	private static Class<?> wrapperClass;
	@Nullable
	private static Method unwrapMethod;
	@Nullable
	private static Method wrapMethod;
	@Nullable
	private static Field extraField;
	private static boolean broken;

	/**
	 * One wrapper per (extra shard, slot type): slot types are pooled, so this stays small and G7 allocates nothing per
	 * request after the first use of a slot.
	 */
	private static final IdentityHashMap<RenderStateShard, IdentityHashMap<RenderType, RenderType>> CACHE = new IdentityHashMap<>();

	private IrisWrappedTypes() {
	}

	/**
	 * @return whether {@code type} is an Iris {@code OuterWrappedRenderType} whose members could be resolved
	 */
	static boolean isWrapper(RenderType type) {
		if (broken) {
			return false;
		}
		Class<?> c = type.getClass();
		if (c == wrapperClass) {
			return true;
		}
		if (wrapperClass != null || !WRAPPER.equals(c.getName())) {
			return false;
		}
		try {
			Method unwrap = c.getMethod("unwrap");
			Method wrap = c.getMethod("wrapExactlyOnce", String.class, RenderType.class, RenderStateShard.class);
			Field extra = c.getDeclaredField("extra");
			if (!Modifier.isStatic(wrap.getModifiers()) || !RenderStateShard.class.isAssignableFrom(extra.getType())) {
				throw new NoSuchMethodException("unexpected shape of " + WRAPPER);
			}
			extra.setAccessible(true);
			unwrapMethod = unwrap;
			wrapMethod = wrap;
			extraField = extra;
			wrapperClass = c;
			return true;
		} catch (Throwable t) {
			disable(t);
			return false;
		}
	}

	/**
	 * @param wrapper a type for which {@link #isWrapper} was true
	 * @return the wrapped type, or null on failure
	 */
	@Nullable
	static RenderType unwrap(RenderType wrapper) {
		try {
			return unwrapMethod == null ? null : (RenderType) unwrapMethod.invoke(wrapper);
		} catch (Throwable t) {
			disable(t);
			return null;
		}
	}

	/**
	 * @param wrapper a type for which {@link #isWrapper} was true
	 * @return its extra state shard, or null on failure
	 */
	@Nullable
	static RenderStateShard extra(RenderType wrapper) {
		try {
			return extraField == null ? null : (RenderStateShard) extraField.get(wrapper);
		} catch (Throwable t) {
			disable(t);
			return null;
		}
	}

	/**
	 * @return {@code inner} wrapped with {@code extra} the way Iris wraps it, or null on failure
	 */
	@Nullable
	static RenderType rewrap(RenderType inner, RenderStateShard extra) {
		IdentityHashMap<RenderType, RenderType> perExtra = CACHE.get(extra);
		if (perExtra != null) {
			RenderType cached = perExtra.get(inner);
			if (cached != null) {
				return cached;
			}
		}
		try {
			if (wrapMethod == null) {
				return null;
			}
			RenderType wrapped = (RenderType) wrapMethod.invoke(null, NAME, inner, extra);
			CACHE.computeIfAbsent(extra, k -> new IdentityHashMap<>()).put(inner, wrapped);
			return wrapped;
		} catch (Throwable t) {
			disable(t);
			return null;
		}
	}

	private static void disable(Throwable t) {
		if (!broken) {
			broken = true;
			Glintsmith.LOGGER.warn("Could not use Iris's wrapped render types; glints drawn by entity renderers under a shader pack stay vanilla.", t);
		}
	}
}
