package navrelis.glintsmith.render.glint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import navrelis.glintsmith.mixin.glint.BufferSourceAccessor;
import navrelis.glintsmith.mixin.glint.RenderBuffersAccessor;

/**
 * Proves that the glint mixins are applied. {@code defaultRequire} is 0, so a broken injection point is silent; the
 * only way to notice is to look at the result.
 * <p>
 * Approach: load the target class through the loader (this applies the mixins), then run the raw class bytes through
 * the active Mixin transformer once more to get the transformed bytecode, and check with ASM that
 * <ul>
 * <li>the target method contains a call to the (renamed) handler method, and that handler exists in the class,</li>
 * <li>for {@code @WrapMethod}, the original body was moved into a {@code $mixinextras$wrapped} method,</li>
 * <li>the accessor interfaces are implemented and their getters read the intended field.</li>
 * </ul>
 */
class GlintMixinApplicationTest {
	private static final String ITEM_RENDERER = "net.minecraft.client.renderer.entity.ItemRenderer";
	private static final String ARMOR_LAYER = "net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer";
	private static final String ELYTRA_LAYER = "net.minecraft.client.renderer.entity.layers.ElytraLayer";
	private static final String BEWLR = "net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer";
	private static final String BUFFER_SOURCE = "net.minecraft.client.renderer.MultiBufferSource$BufferSource";
	private static final String GAME_RENDERER = "net.minecraft.client.renderer.GameRenderer";
	private static final String RENDER_BUFFERS = "net.minecraft.client.renderer.RenderBuffers";

	private static final Set<String> MIXIN_CLASSES = new TreeSet<>(Set.of(
		"glint.ItemRendererGlintMixin", "glint.HumanoidArmorLayerGlintMixin", "glint.ElytraLayerGlintMixin",
		"glint.BlockEntityWithoutLevelRendererGlintMixin", "glint.BufferSourceGlintMixin", "glint.GameRendererGlintMixin",
		"glint.BufferSourceAccessor", "glint.RenderBuffersAccessor"));

	/** The transformed bytecode of a class. */
	private static ClassNode transformed(String name) throws Exception {
		Class.forName(name, false, GlintMixinApplicationTest.class.getClassLoader()); // applies the mixins once
		IMixinTransformer transformer = (IMixinTransformer) MixinEnvironment.getCurrentEnvironment().getActiveTransformer();
		byte[] raw = FabricLauncherBase.getLauncher().getClassByteArray(name, false);
		byte[] out = transformer.transformClassBytes(name, name, raw);
		ClassNode node = new ClassNode();
		new ClassReader(out).accept(node, 0);
		return node;
	}

	/**
	 * Asserts that a method called {@code method} whose descriptor contains {@code descPart} calls a method of the same
	 * class whose name contains {@code handlerPart}, and that this handler exists. When another {@code @WrapMethod} (the
	 * outline feature) wraps the same method, MixinExtras moves our wrapper's call into a {@code $mixinextras$wrapped}
	 * copy of the method, so the target and those copies are all searched.
	 */
	private static void assertCallsHandler(ClassNode node, String method, String descPart, String handlerPart) {
		boolean found = false;
		for (MethodNode m : node.methods) {
			boolean wrappedCopy = m.name.startsWith(method + "$mixinextras$wrapped");
			if (!(m.name.equals(method) || wrappedCopy) || !m.desc.contains(descPart)) {
				continue;
			}
			for (AbstractInsnNode insn : m.instructions) {
				if (insn instanceof MethodInsnNode call && call.owner.equals(node.name) && call.name.contains(handlerPart)) {
					found |= node.methods.stream().anyMatch(h -> h.name.equals(call.name) && h.desc.equals(call.desc));
				}
			}
		}
		assertTrue(found, node.name + "." + method + " does not call a handler containing '" + handlerPart + "': injection was not applied");
	}

	private static void assertHasWrapped(ClassNode node, String method) {
		boolean found = node.methods.stream().anyMatch(m -> m.name.startsWith(method + "$mixinextras$wrapped"));
		assertTrue(found, node.name + "." + method + " was not wrapped (no $mixinextras$wrapped method)");
	}

	@Test
	void itemRendererRenderIsWrapped() throws Exception {
		ClassNode node = transformed(ITEM_RENDERER);
		assertCallsHandler(node, "render", "Lnet/minecraft/client/resources/model/BakedModel;)V", "glintsmith$glintContext");
		assertHasWrapped(node, "render");
	}

	@Test
	void humanoidArmorLayerRenderArmorPieceIsWrapped() throws Exception {
		ClassNode node = transformed(ARMOR_LAYER);
		assertCallsHandler(node, "renderArmorPiece", "Lnet/minecraft/world/entity/EquipmentSlot;I", "glintsmith$glintContext");
		assertHasWrapped(node, "renderArmorPiece");
	}

	@Test
	void elytraLayerRenderIsWrapped() throws Exception {
		ClassNode node = transformed(ELYTRA_LAYER);
		assertCallsHandler(node, "render", "ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V", "glintsmith$glintContext");
		assertHasWrapped(node, "render");
	}

	@Test
	void blockEntityWithoutLevelRendererRenderByItemIsWrapped() throws Exception {
		ClassNode node = transformed(BEWLR);
		assertCallsHandler(node, "renderByItem", "Lnet/minecraft/world/item/ItemDisplayContext;", "glintsmith$glintContext");
		assertHasWrapped(node, "renderByItem");
	}

	@Test
	void bufferSourceHooksAreApplied() throws Exception {
		ClassNode node = transformed(BUFFER_SOURCE);
		assertCallsHandler(node, "getBuffer", "(Lnet/minecraft/client/renderer/RenderType;)", "glintsmith$substituteGlint");
		assertCallsHandler(node, "endBatch", "(Lnet/minecraft/client/renderer/RenderType;)V", "glintsmith$flushGlintSlots");
		assertCallsHandler(node, "endBatch", "(Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/vertex/BufferBuilder;)V",
			"glintsmith$releaseGlintSlot");
	}

	@Test
	void gameRendererFrameBeginIsInjected() throws Exception {
		ClassNode node = transformed(GAME_RENDERER);
		assertCallsHandler(node, "render", "(Lnet/minecraft/client/DeltaTracker;Z)V", "glintsmith$glintFrameBegin");
	}

	@Test
	void accessorsAreImplementedAndReadTheirFields() throws Exception {
		Class<?> source = Class.forName(BUFFER_SOURCE, false, getClass().getClassLoader());
		Class<?> buffers = Class.forName(RENDER_BUFFERS, false, getClass().getClassLoader());
		assertTrue(BufferSourceAccessor.class.isAssignableFrom(source), "BufferSourceAccessor was not applied");
		assertTrue(RenderBuffersAccessor.class.isAssignableFrom(buffers), "RenderBuffersAccessor was not applied");

		assertGetter(transformed(BUFFER_SOURCE), "glintsmith$getFixedBuffers", "fixedBuffers");
		assertGetter(transformed(RENDER_BUFFERS), "glintsmith$getBufferSource", "bufferSource");
	}

	private static void assertGetter(ClassNode node, String getter, String field) {
		MethodNode m = node.methods.stream().filter(x -> x.name.equals(getter)).findFirst().orElse(null);
		assertNotNull(m, node.name + " has no " + getter);
		boolean reads = false;
		for (AbstractInsnNode insn : m.instructions) {
			reads |= insn instanceof FieldInsnNode f && f.name.equals(field);
		}
		assertTrue(reads, getter + " does not read " + field);
	}

	// ---- the configuration itself

	@Test
	void mixinConfigListsExactlyOurClassesAndRequiresNothing() throws IOException {
		JsonObject config = readJson("/glintsmith.glint.mixins.json");
		assertEquals("navrelis.glintsmith.mixin", config.get("package").getAsString());
		assertEquals("JAVA_21", config.get("compatibilityLevel").getAsString());
		assertEquals(0, config.getAsJsonObject("injectors").get("defaultRequire").getAsInt());

		Set<String> listed = new TreeSet<>();
		for (var element : config.getAsJsonArray("client")) {
			listed.add(element.getAsString());
		}
		assertEquals(MIXIN_CLASSES, listed);
		for (String name : listed) {
			String resource = "/navrelis/glintsmith/mixin/" + name.replace('.', '/') + ".class";
			assertNotNull(GlintMixinApplicationTest.class.getResource(resource), "missing class for " + name);
		}
	}

	@Test
	void fabricModJsonRegistersTheConfig() throws IOException {
		// Every mod on the test classpath has a fabric.mod.json: pick ours by id, not the first one found.
		JsonObject ours = null;
		var urls = GlintMixinApplicationTest.class.getClassLoader().getResources("fabric.mod.json");
		while (urls.hasMoreElements()) {
			try (InputStream in = urls.nextElement().openStream()) {
				JsonObject candidate = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
				if (candidate.has("id") && "glintsmith".equals(candidate.get("id").getAsString())) {
					ours = candidate;
					break;
				}
			}
		}
		assertNotNull(ours, "no fabric.mod.json with id glintsmith on the classpath");
		JsonArray mixins = ours.getAsJsonArray("mixins");
		// An entry is either a plain string or {"config": "...", "environment": "..."}.
		List<String> names = new ArrayList<>();
		for (var entry : mixins) {
			if (entry.isJsonPrimitive()) {
				names.add(entry.getAsString());
			} else if (entry.isJsonObject() && entry.getAsJsonObject().has("config")) {
				names.add(entry.getAsJsonObject().get("config").getAsString());
			}
		}
		assertTrue(names.contains("glintsmith.glint.mixins.json"), "fabric.mod.json mixins: " + names);
	}

	@Test
	void everyInjectorStatesRequireZero() throws IOException {
		int injectors = 0;
		for (String name : MIXIN_CLASSES) {
			String resource = "/navrelis/glintsmith/mixin/" + name.replace('.', '/') + ".class";
			ClassNode node = new ClassNode();
			try (InputStream in = GlintMixinApplicationTest.class.getResourceAsStream(resource)) {
				new ClassReader(in).accept(node, 0);
			}
			for (MethodNode m : node.methods) {
				List<AnnotationNode> annotations = new ArrayList<>();
				if (m.visibleAnnotations != null) {
					annotations.addAll(m.visibleAnnotations);
				}
				if (m.invisibleAnnotations != null) {
					annotations.addAll(m.invisibleAnnotations);
				}
				for (AnnotationNode a : annotations) {
					if (a.desc.endsWith("/WrapMethod;") || a.desc.endsWith("/Inject;") || a.desc.endsWith("/ModifyVariable;")) {
						injectors++;
						assertEquals(Integer.valueOf(0), annotationValue(a, "require"),
							name + "." + m.name + " must declare require = 0");
						assertFalse(mentionsKey(a, "ordinal"), name + "." + m.name + " must not pin an ordinal");
					}
				}
			}
		}
		assertEquals(8, injectors, "expected G1 to G4 (WrapMethod), G5a (ModifyVariable), G5b, G5c and G6 (Inject)");
	}

	/** True if the annotation, or any annotation nested in it (such as the {@code @At}), sets {@code key}. */
	private static boolean mentionsKey(AnnotationNode a, String key) {
		if (a.values == null) {
			return false;
		}
		for (int i = 0; i + 1 < a.values.size(); i += 2) {
			if (key.equals(a.values.get(i)) || mentionsKey(a.values.get(i + 1), key)) {
				return true;
			}
		}
		return false;
	}

	private static boolean mentionsKey(Object value, String key) {
		if (value instanceof AnnotationNode nested) {
			return mentionsKey(nested, key);
		}
		if (value instanceof List<?> list) {
			return list.stream().anyMatch(v -> mentionsKey(v, key));
		}
		return false;
	}

	private static Object annotationValue(AnnotationNode a, String key) {
		if (a.values == null) {
			return null;
		}
		for (int i = 0; i + 1 < a.values.size(); i += 2) {
			if (key.equals(a.values.get(i))) {
				return a.values.get(i + 1);
			}
		}
		return null;
	}

	private static JsonObject readJson(String resource) throws IOException {
		try (InputStream in = GlintMixinApplicationTest.class.getResourceAsStream(resource)) {
			assertNotNull(in, "missing resource " + resource);
			return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
		}
	}
}
