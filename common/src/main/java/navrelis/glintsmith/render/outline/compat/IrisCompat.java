package navrelis.glintsmith.render.outline.compat;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import navrelis.glintsmith.platform.Services;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Optional Iris integration of the outline renderer (the glint system also reads {@link #queryShaderPack()} once per
 * frame to keep G7 off under a shader pack). Iris is a compile-only dependency: its API is only touched from
 * {@link Api}, which is loaded only when the {@code iris} mod is present, so nothing breaks without it.
 * <p>
 * Any error from the Iris API counts as "a shader pack is in use" (the safe side: world outlines off) and is logged
 * once. No Sodium API is needed.
 * <p>
 * World outlines under a shader pack also need Iris internals that have no API: the depth texture {@code depthtex1}
 * ({@code RenderTargets.getDepthTextureNoTranslucents()}) of the active {@code IrisRenderingPipeline}, and the
 * pipeline's "main target bound" flag. They are reached by reflection only ({@link Internals}), resolved once on first
 * use and cached as method handles, so every call afterwards is a plain handle invocation. Anything missing (another
 * Iris version, another pipeline class, an exception) turns world outlines under shader packs off with one INFO line:
 * for the rest of the session if the internals cannot be resolved or a call throws, for that pipeline (that is, until
 * the pack or the dimension changes) if the pipeline is not one Glintsmith knows.
 */
public final class IrisCompat {
	private static final Logger LOGGER = LoggerFactory.getLogger("Glintsmith/Outline");
	private static final boolean LOADED = detect();

	private static boolean packInUse;
	private static boolean warned;

	/** World outlines under shader packs are off for the session (internals missing, or a call threw). */
	private static boolean packDepthOff;
	/** A pipeline whose depth textures cannot be used; world outlines stay off while it is the active one. */
	@Nullable
	private static WeakReference<Object> rejectedPipeline;
	/** The pipeline {@link #beginOwnDraw()} saved the flag of, and the flag. */
	@Nullable
	private static Object drawPipeline;
	private static boolean drawMainBound;

	private IrisCompat() {
	}

	private static boolean detect() {
		try {
			return Services.PLATFORM.isModLoaded("iris");
		} catch (Throwable t) {
			return false;
		}
	}

	/**
	 * @return whether Iris is installed
	 */
	public static boolean loaded() {
		return LOADED;
	}

	/**
	 * @return whether a shader pack was in use at the start of this frame (see {@link #refreshPerFrame()})
	 */
	public static boolean shaderPackInUse() {
		return packInUse;
	}

	/**
	 * Asks Iris right now whether a shader pack is in use. False without Iris; true on any Iris error.
	 */
	public static boolean queryShaderPack() {
		if (!LOADED) {
			return false;
		}
		try {
			return Api.shaderPackInUse();
		} catch (Throwable t) {
			warnOnce(t);
			return true;
		}
	}

	/**
	 * @return whether Iris is rendering its shadow pass right now; false without Iris or on errors
	 */
	public static boolean shadowPass() {
		if (!LOADED) {
			return false;
		}
		try {
			return Api.shadowPass();
		} catch (Throwable t) {
			warnOnce(t);
			return false;
		}
	}

	/**
	 * Caches {@link #queryShaderPack()} for the frame. Call once at frame begin.
	 */
	public static void refreshPerFrame() {
		packInUse = queryShaderPack();
	}

	private static void warnOnce(Throwable t) {
		if (!warned) {
			warned = true;
			LOGGER.warn("[Glintsmith] The Iris API failed; treating shaders as active for outlines: {}", String.valueOf(t));
		}
	}

	// ---- world outlines under a shader pack (Iris internals)

	/**
	 * Whether world outlines can use Iris's depth under the active pipeline. Call once per frame while a shader pack
	 * is in use. The first call resolves the internals (one INFO line if that fails).
	 *
	 * @return false without Iris, if the internals are missing, after an error, or while the active pipeline was
	 *         rejected by {@link #packSceneDepthTexture}
	 */
	public static boolean packDepthUsable() {
		if (!LOADED || packDepthOff) {
			return false;
		}
		Internals internals = Internals.get();
		if (internals == null) {
			disablePackDepth("Iris internals not found (" + Internals.error + ")", null);
			return false;
		}
		WeakReference<Object> rejected = rejectedPipeline;
		if (rejected == null) {
			return true;
		}
		try {
			Object pipeline = internals.pipeline();
			if (pipeline != null && pipeline == rejected.get()) {
				return false;
			}
			rejectedPipeline = null; // another pack or dimension: try again
			return true;
		} catch (Throwable t) {
			disablePackDepth("the Iris pipeline cannot be read", t);
			return false;
		}
	}

	/**
	 * The GL id of Iris's {@code depthtex1} of the active pipeline: the depth of the frame without translucent terrain
	 * and translucent entity draws, with the solid first-person hand. Iris copies it every frame right before the
	 * translucent terrain, so after the level is rendered it holds this frame's depth.
	 *
	 * @param width expected width (the main target's)
	 * @param height expected height
	 * @return the texture id, or -1 if it cannot be used this frame (the size differs, for example during a resize;
	 *         or the pipeline is not an {@code IrisRenderingPipeline}, which is logged once and rejects the pipeline)
	 */
	public static int packSceneDepthTexture(int width, int height) {
		if (!LOADED || packDepthOff) {
			return -1;
		}
		Internals internals = Internals.get();
		if (internals == null) {
			return -1;
		}
		try {
			Object pipeline = internals.pipeline();
			if (pipeline == null || !internals.pipelineClass.isInstance(pipeline)) {
				rejectPipeline(pipeline);
				return -1;
			}
			Object targets = internals.renderTargets(pipeline);
			if (targets == null || internals.width(targets) != width || internals.height(targets) != height) {
				return -1;
			}
			Object texture = internals.noTranslucents(targets);
			if (texture == null) {
				rejectPipeline(pipeline);
				return -1;
			}
			int id = internals.textureId(texture);
			return id > 0 ? id : -1;
		} catch (Throwable t) {
			disablePackDepth("reading Iris's depth texture failed", t);
			return -1;
		}
	}

	/**
	 * Call before Glintsmith binds its own framebuffer inside Iris's level rendering. Iris tracks whether the main
	 * target is bound ({@code RenderTarget.bindWrite} of any target updates it) and only redirects or masks unknown
	 * programs while it is; the outline code binds its targets with {@code bindWrite} but restores the previous
	 * framebuffer with a raw bind. {@link #endOwnDraw()} puts the flag back. No-op without an Iris pipeline.
	 */
	public static void beginOwnDraw() {
		drawPipeline = null;
		if (!LOADED || packDepthOff) {
			return;
		}
		Internals internals = Internals.get();
		if (internals == null) {
			return;
		}
		try {
			Object pipeline = internals.pipeline();
			if (internals.pipelineClass.isInstance(pipeline)) {
				drawMainBound = internals.mainBound(pipeline);
				drawPipeline = pipeline;
			}
		} catch (Throwable t) {
			disablePackDepth("the Iris pipeline cannot be read", t);
		}
	}

	/**
	 * Restores what {@link #beginOwnDraw()} saved. Call after the previous framebuffer is bound again.
	 */
	public static void endOwnDraw() {
		Object pipeline = drawPipeline;
		drawPipeline = null;
		Internals internals = Internals.get();
		if (pipeline == null || internals == null) {
			return;
		}
		try {
			internals.setMainBound(pipeline, drawMainBound);
		} catch (Throwable t) {
			disablePackDepth("the Iris pipeline cannot be updated", t);
		}
	}

	/**
	 * Turns world outlines under shader packs off for the session, with one INFO line.
	 */
	public static void disablePackDepth(String reason, @Nullable Throwable cause) {
		if (packDepthOff) {
			return;
		}
		packDepthOff = true;
		LOGGER.info("[Glintsmith] World outlines are off under shader packs for this session: {}{}. First-person and GUI outlines are not affected.",
			reason, cause == null ? "" : " (" + cause + ")");
	}

	private static void rejectPipeline(@Nullable Object pipeline) {
		WeakReference<Object> rejected = rejectedPipeline;
		if (rejected != null && rejected.get() == pipeline) {
			return;
		}
		rejectedPipeline = new WeakReference<>(pipeline);
		LOGGER.info("[Glintsmith] World outlines are off under this shader pack: the Iris pipeline {} has no usable depth texture. First-person and GUI outlines are not affected.",
			pipeline == null ? "null" : pipeline.getClass().getName());
	}

	/** Only loaded when Iris is present. */
	private static final class Api {
		private static final net.irisshaders.iris.api.v0.IrisApi INSTANCE = net.irisshaders.iris.api.v0.IrisApi.getInstance();

		static boolean shaderPackInUse() {
			return INSTANCE.isShaderPackInUse();
		}

		static boolean shadowPass() {
			return INSTANCE.isRenderingShadowPass();
		}
	}

	/**
	 * The Iris internals used by world outlines under a shader pack, as method handles typed over {@code Object}, so
	 * that no Iris type appears in Glintsmith's bytecode. Verified in the Iris 1.8.14-beta.1 (1.21.1) jar:
	 * <ul>
	 * <li>{@code net.irisshaders.iris.Iris.getPipelineManager()}: public static, returns {@code PipelineManager};</li>
	 * <li>{@code PipelineManager.getPipelineNullable()}: public, returns {@code WorldRenderingPipeline};</li>
	 * <li>{@code net.irisshaders.iris.pipeline.IrisRenderingPipeline}: field {@code private final RenderTargets
	 * renderTargets}, {@code public boolean shouldOverrideShaders()} ({@code isRenderingWorld && isMainBound}),
	 * {@code public void setIsMainBound(boolean)};</li>
	 * <li>{@code net.irisshaders.iris.targets.RenderTargets}: {@code public DepthTexture
	 * getDepthTextureNoTranslucents()}, {@code public int getCurrentWidth()}, {@code public int getCurrentHeight()};</li>
	 * <li>{@code net.irisshaders.iris.targets.DepthTexture.getTextureId()}: public, returns int.</li>
	 * </ul>
	 * The Iris classes are not remapped by any loader, so the names hold on Fabric and NeoForge alike.
	 */
	private static final class Internals {
		private static boolean resolved;
		@Nullable
		private static Internals instance;
		@Nullable
		static String error;

		final Class<?> pipelineClass;
		private final MethodHandle pipelineManager;
		private final MethodHandle pipeline;
		private final MethodHandle renderTargets;
		private final MethodHandle overridesShaders;
		private final MethodHandle setMainBound;
		private final MethodHandle noTranslucents;
		private final MethodHandle width;
		private final MethodHandle height;
		private final MethodHandle textureId;

		private Internals(Class<?> pipelineClass, MethodHandle pipelineManager, MethodHandle pipeline,
			MethodHandle renderTargets, MethodHandle overridesShaders, MethodHandle setMainBound, MethodHandle noTranslucents,
			MethodHandle width, MethodHandle height, MethodHandle textureId) {
			this.pipelineClass = pipelineClass;
			this.pipelineManager = pipelineManager;
			this.pipeline = pipeline;
			this.renderTargets = renderTargets;
			this.overridesShaders = overridesShaders;
			this.setMainBound = setMainBound;
			this.noTranslucents = noTranslucents;
			this.width = width;
			this.height = height;
			this.textureId = textureId;
		}

		/**
		 * @return the resolved internals, or null if they are missing (then {@link #error} says why); resolved once
		 */
		@Nullable
		static Internals get() {
			if (!resolved) {
				resolved = true;
				try {
					instance = resolve();
				} catch (Throwable t) {
					instance = null;
					error = String.valueOf(t);
				}
			}
			return instance;
		}

		private static Internals resolve() throws ReflectiveOperationException {
			ClassLoader loader = IrisCompat.class.getClassLoader();
			Class<?> iris = Class.forName("net.irisshaders.iris.Iris", false, loader);
			Class<?> manager = Class.forName("net.irisshaders.iris.pipeline.PipelineManager", false, loader);
			Class<?> pipeline = Class.forName("net.irisshaders.iris.pipeline.IrisRenderingPipeline", false, loader);
			Class<?> targets = Class.forName("net.irisshaders.iris.targets.RenderTargets", false, loader);
			Class<?> depthTexture = Class.forName("net.irisshaders.iris.targets.DepthTexture", false, loader);
			MethodHandles.Lookup lookup = MethodHandles.lookup();
			MethodType toObject = MethodType.methodType(Object.class, Object.class);
			MethodType toInt = MethodType.methodType(int.class, Object.class);
			Field renderTargets = pipeline.getDeclaredField("renderTargets");
			renderTargets.setAccessible(true);
			return new Internals(pipeline,
				lookup.unreflect(method(iris, "getPipelineManager")).asType(MethodType.methodType(Object.class)),
				lookup.unreflect(method(manager, "getPipelineNullable")).asType(toObject),
				lookup.unreflectGetter(renderTargets).asType(toObject),
				lookup.unreflect(method(pipeline, "shouldOverrideShaders")).asType(MethodType.methodType(boolean.class, Object.class)),
				lookup.unreflect(method(pipeline, "setIsMainBound", boolean.class))
					.asType(MethodType.methodType(void.class, Object.class, boolean.class)),
				lookup.unreflect(method(targets, "getDepthTextureNoTranslucents")).asType(toObject),
				lookup.unreflect(method(targets, "getCurrentWidth")).asType(toInt),
				lookup.unreflect(method(targets, "getCurrentHeight")).asType(toInt),
				lookup.unreflect(method(depthTexture, "getTextureId")).asType(toInt));
		}

		private static Method method(Class<?> owner, String name, Class<?>... parameters) throws NoSuchMethodException {
			Method method = owner.getMethod(name, parameters);
			method.setAccessible(true);
			return method;
		}

		@Nullable
		Object pipeline() throws Throwable {
			Object manager = (Object) pipelineManager.invokeExact();
			return manager == null ? null : (Object) pipeline.invokeExact(manager);
		}

		Object renderTargets(Object pipeline) throws Throwable {
			return (Object) renderTargets.invokeExact(pipeline);
		}

		boolean mainBound(Object pipeline) throws Throwable {
			// shouldOverrideShaders() is isRenderingWorld && isMainBound, and the outline code only draws inside the
			// level render, where isRenderingWorld is true.
			return (boolean) overridesShaders.invokeExact(pipeline);
		}

		void setMainBound(Object pipeline, boolean bound) throws Throwable {
			setMainBound.invokeExact(pipeline, bound);
		}

		Object noTranslucents(Object targets) throws Throwable {
			return (Object) noTranslucents.invokeExact(targets);
		}

		int width(Object targets) throws Throwable {
			return (int) width.invokeExact(targets);
		}

		int height(Object targets) throws Throwable {
			return (int) height.invokeExact(targets);
		}

		int textureId(Object texture) throws Throwable {
			return (int) textureId.invokeExact(texture);
		}
	}
}
