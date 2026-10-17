package navrelis.glintsmith.render.outline;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL20;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The three outline shader programs, created lazily on the render thread at first use after each resource reload.
 * <p>
 * Resource paths: vanilla's {@code ShaderInstance} constructor always looks up
 * {@code minecraft:shaders/core/<name>.json}, and resolves the vertex and fragment program names inside the JSON as
 * {@code minecraft:shaders/core/<program>.vsh/.fsh}. Fabric API only adds namespace support for its own impl class.
 * So the instances are named {@code glintsmith_<x>} and created with a {@link ResourceProvider} that maps
 * {@code minecraft:shaders/core/glintsmith_<file>} to {@code glintsmith:shaders/core/<file>}; the program names in
 * the JSON files use the same {@code glintsmith_} prefix.
 * <p>
 * A compile or link failure logs one line and disables outlines until the next reload; it never throws.
 */
final class OutlineShaders {
	private static final Logger LOGGER = LoggerFactory.getLogger("Glintsmith/Outline");
	static final String PREFIX = "glintsmith_";
	private static final String CORE = "shaders/core/";

	@Nullable
	private static ShaderInstance mask;
	@Nullable
	private static ShaderInstance edt;
	@Nullable
	private static ShaderInstance composite;
	private static boolean dirty = true;
	private static boolean failed;

	private OutlineShaders() {
	}

	/** The mask shader, or null if unavailable. */
	@Nullable
	static ShaderInstance mask() {
		ensure();
		return mask;
	}

	/** The horizontal distance pass, or null if unavailable. */
	@Nullable
	static ShaderInstance edt() {
		ensure();
		return edt;
	}

	/** The vertical pass that blends the outline, or null if unavailable. */
	@Nullable
	static ShaderInstance composite() {
		ensure();
		return composite;
	}

	/**
	 * @return whether all three programs are ready (creating them if needed)
	 */
	static boolean available() {
		ensure();
		return mask != null && edt != null && composite != null;
	}

	/**
	 * Called by the resource reload listener; the programs are rebuilt at their next use.
	 */
	static void markDirty() {
		dirty = true;
	}

	/**
	 * Frees the programs (client shutdown).
	 */
	static void close() {
		closeAll();
		dirty = true;
	}

	/**
	 * Maps {@code minecraft:shaders/core/glintsmith_<file>} to {@code glintsmith:shaders/core/<file>}; everything
	 * else (for example GLSL imports) passes through unchanged.
	 */
	static ResourceLocation remap(ResourceLocation location) {
		if (ResourceLocation.DEFAULT_NAMESPACE.equals(location.getNamespace())) {
			String path = location.getPath();
			if (path.startsWith(CORE + PREFIX)) {
				return ResourceLocation.fromNamespaceAndPath("glintsmith", CORE + path.substring(CORE.length() + PREFIX.length()));
			}
		}
		return location;
	}

	private static void ensure() {
		if (!dirty) {
			return;
		}
		dirty = false;
		closeAll();
		failed = false;
		try {
			ResourceManager manager = Minecraft.getInstance().getResourceManager();
			ResourceProvider provider = location -> manager.getResource(remap(location));
			mask = create(provider, "mask", DefaultVertexFormat.POSITION_TEX_COLOR);
			edt = create(provider, "outline_edt", DefaultVertexFormat.POSITION);
			composite = create(provider, "outline_composite", DefaultVertexFormat.POSITION);
		} catch (Throwable t) {
			closeAll();
			if (!failed) {
				failed = true;
				LOGGER.error("[Glintsmith] Could not load the outline shaders, outlines are off until the next resource reload: {}",
					String.valueOf(t.getMessage()));
			}
		}
	}

	private static ShaderInstance create(ResourceProvider provider, String name, VertexFormat format) throws Exception {
		ShaderInstance shader = new ShaderInstance(provider, PREFIX + name, format);
		// Vanilla only logs a link failure; a program that did not link must not be used.
		if (GlStateManager.glGetProgrami(shader.getId(), GL20.GL_LINK_STATUS) == 0) {
			shader.close();
			throw new IllegalStateException("shader " + PREFIX + name + " did not link");
		}
		return shader;
	}

	private static void closeAll() {
		mask = closeQuietly(mask);
		edt = closeQuietly(edt);
		composite = closeQuietly(composite);
	}

	@Nullable
	private static ShaderInstance closeQuietly(@Nullable ShaderInstance shader) {
		if (shader != null) {
			try {
				shader.close();
			} catch (Throwable ignored) {
				// closing twice after a vanilla shader reload is harmless; anything else is not worth a crash
			}
		}
		return null;
	}
}
