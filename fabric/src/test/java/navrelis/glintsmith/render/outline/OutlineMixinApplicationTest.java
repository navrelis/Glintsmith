package navrelis.glintsmith.render.outline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import navrelis.glintsmith.mixin.outline.CompositeRenderTypeAccessor;
import navrelis.glintsmith.mixin.outline.CompositeStateAccessor;
import navrelis.glintsmith.mixin.outline.EmptyTextureStateShardAccessor;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
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

/**
 * Proves that every outline injector and accessor applies to its 1.21.1 target. The outline mixin config sets
 * {@code defaultRequire} to 0 and every injector says {@code require = 0}, so a broken target is silent at run time;
 * the only way to notice it is to look at the transformed classes.
 * <p>
 * Approach: load the target through the loader (this applies the mixins), run the raw class bytes through the active
 * Mixin transformer once more to get the transformed bytecode, and check with ASM that
 * <ul>
 * <li>the target method (or the {@code $mixinextras$wrapped} copy another wrapper left behind) calls our renamed
 * handler, and that handler exists in the class,</li>
 * <li>{@code @WrapMethod} moved the original body into a {@code $mixinextras$wrapped} method,</li>
 * <li>HEAD and TAIL injections sit before and after the vanilla body,</li>
 * <li>the accessor interfaces are implemented and read the intended field or call the intended method.</li>
 * </ul>
 */
class OutlineMixinApplicationTest {
	private static final String CONFIG = "glintsmith.outline.mixins.json";

	private static final String ITEM_RENDERER = "net.minecraft.client.renderer.entity.ItemRenderer";
	private static final String ITEM_IN_HAND_RENDERER = "net.minecraft.client.renderer.ItemInHandRenderer";
	private static final String ARMOR_LAYER = "net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer";
	private static final String ELYTRA_LAYER = "net.minecraft.client.renderer.entity.layers.ElytraLayer";
	private static final String GUI_GRAPHICS = "net.minecraft.client.gui.GuiGraphics";
	private static final String BUFFER_SOURCE = "net.minecraft.client.renderer.MultiBufferSource$BufferSource";
	private static final String GAME_RENDERER = "net.minecraft.client.renderer.GameRenderer";
	private static final String COMPOSITE_RENDER_TYPE = "net.minecraft.client.renderer.RenderType$CompositeRenderType";
	private static final String COMPOSITE_STATE = "net.minecraft.client.renderer.RenderType$CompositeState";
	private static final String EMPTY_TEXTURE_SHARD = "net.minecraft.client.renderer.RenderStateShard$EmptyTextureStateShard";

	private static final Set<String> MIXIN_CLASSES = new TreeSet<>(Set.of(
		"outline.ItemRendererOutlineMixin", "outline.ItemInHandRendererOutlineMixin", "outline.HumanoidArmorLayerOutlineMixin",
		"outline.ElytraLayerOutlineMixin", "outline.GuiGraphicsOutlineMixin", "outline.BufferSourceOutlineMixin",
		"outline.GameRendererOutlineMixin", "outline.CompositeRenderTypeAccessor", "outline.CompositeStateAccessor",
		"outline.EmptyTextureStateShardAccessor"));

	/** O1, O2a, O2b, O3, O4, O5 (WrapMethod); O6 x3, O7a, O7b, O7c (Inject). */
	private static final int INJECTORS = 12;

	// ---- O1 to O5: @WrapMethod

	@Test
	void o1ItemRendererRenderIsWrapped() throws Exception {
		ClassNode node = transformed(ITEM_RENDERER);
		String desc = "(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;"
			+ "Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/resources/model/BakedModel;)V";
		assertWrapped(node, "render", desc, "glintsmith$outlineItem");
	}

	@Test
	void o2aItemInHandRendererRenderItemIsWrapped() throws Exception {
		ClassNode node = transformed(ITEM_IN_HAND_RENDERER);
		String desc = "(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Z"
			+ "Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V";
		assertWrapped(node, "renderItem", desc, "glintsmith$outlineHolder");
	}

	@Test
	void o2bRenderHandsWithItemsIsWrapped() throws Exception {
		ClassNode node = transformed(ITEM_IN_HAND_RENDERER);
		String desc = "(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;"
			+ "Lnet/minecraft/client/player/LocalPlayer;I)V";
		assertWrapped(node, "renderHandsWithItems", desc, "glintsmith$outlineHands");
	}

	@Test
	void o3HumanoidArmorLayerRenderArmorPieceIsWrapped() throws Exception {
		ClassNode node = transformed(ARMOR_LAYER);
		String desc = "(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/entity/LivingEntity;"
			+ "Lnet/minecraft/world/entity/EquipmentSlot;ILnet/minecraft/client/model/HumanoidModel;)V";
		assertWrapped(node, "renderArmorPiece", desc, "glintsmith$outlineArmor");
	}

	@Test
	void o4ElytraLayerRenderIsWrapped() throws Exception {
		ClassNode node = transformed(ELYTRA_LAYER);
		String desc = "(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V";
		assertWrapped(node, "render", desc, "glintsmith$outlineElytra");
	}

	@Test
	void o5GuiGraphicsRenderItemIsWrapped() throws Exception {
		ClassNode node = transformed(GUI_GRAPHICS);
		String desc = "(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;IIII)V";
		assertWrapped(node, "renderItem", desc, "glintsmith$outlineGuiItem");
	}

	// ---- O6, O7: @Inject

	@Test
	void o6BufferSourceFlushHooksAreAtHeadAndTail() throws Exception {
		ClassNode node = transformed(BUFFER_SOURCE);
		MethodNode endBatch = method(node, "endBatch", "()V");
		List<String> calls = calls(endBatch);
		int head = indexOfHandler(calls, "glintsmith$outlineFlushHead");
		int tail = indexOfHandler(calls, "glintsmith$outlineFlushTail");
		int firstVanilla = calls.indexOf("endLastBatch");
		int lastVanilla = calls.lastIndexOf("endBatch");
		assertTrue(head >= 0 && tail >= 0, "endBatch() does not call both flush handlers: " + calls);
		assertTrue(firstVanilla >= 0 && lastVanilla >= 0, "unexpected endBatch() body: " + calls);
		assertTrue(head < firstVanilla, "the HEAD handler must run before vanilla flushes anything: " + calls);
		assertTrue(tail > lastVanilla, "the TAIL handler must run after the last per-type flush: " + calls);

		MethodNode endBatchType = method(node, "endBatch", "(Lnet/minecraft/client/renderer/RenderType;)V");
		assertCallsExistingHandler(node, endBatchType, "glintsmith$outlineFlushType");
	}

	@Test
	void o7GameRendererFrameHooksAreInjected() throws Exception {
		ClassNode node = transformed(GAME_RENDERER);
		MethodNode render = method(node, "render", "(Lnet/minecraft/client/DeltaTracker;Z)V");
		assertCallsExistingHandler(node, render, "glintsmith$outlineFrameBegin");
		assertCallsExistingHandler(node, render, "glintsmith$outlineFrameEnd");
		List<String> calls = calls(render);
		assertTrue(indexOfHandler(calls, "glintsmith$outlineFrameBegin") < calls.indexOf("renderLevel"),
			"frame begin must run before the level is rendered: " + calls);
		assertTrue(lastIndexOfHandler(calls, "glintsmith$outlineFrameEnd") > calls.lastIndexOf("renderLevel"),
			"frame end must run after the level is rendered: " + calls);

		MethodNode renderLevel = method(node, "renderLevel", "(Lnet/minecraft/client/DeltaTracker;)V");
		assertCallsExistingHandler(node, renderLevel, "glintsmith$outlineFrameLevel");
		List<String> levelCalls = calls(renderLevel);
		int hands = levelCalls.lastIndexOf("renderItemInHand");
		assertTrue(hands >= 0, "unexpected renderLevel body: " + levelCalls);
		assertTrue(indexOfHandler(levelCalls, "glintsmith$outlineFrameLevel") > hands,
			"the level hook must run at RETURN, after the hand: " + levelCalls);
	}

	// ---- A1 to A3: accessors

	@Test
	void a1CompositeRenderTypeAccessorInvokesState() throws Exception {
		assertImplements(COMPOSITE_RENDER_TYPE, CompositeRenderTypeAccessor.class);
		assertAccessorBody(transformed(COMPOSITE_RENDER_TYPE), "glintsmith$outlineState", null, "state");
	}

	@Test
	void a2CompositeStateAccessorReadsTheShardFields() throws Exception {
		assertImplements(COMPOSITE_STATE, CompositeStateAccessor.class);
		ClassNode node = transformed(COMPOSITE_STATE);
		assertAccessorBody(node, "glintsmith$outlineTextureState", "textureState", null);
		assertAccessorBody(node, "glintsmith$outlineTransparencyState", "transparencyState", null);
		assertAccessorBody(node, "glintsmith$outlineTexturingState", "texturingState", null);
		assertAccessorBody(node, "glintsmith$outlineWriteMaskState", "writeMaskState", null);
		assertAccessorBody(node, "glintsmith$outlineLayeringState", "layeringState", null);
		assertAccessorBody(node, "glintsmith$outlineDepthTestState", "depthTestState", null);
		assertAccessorBody(node, "glintsmith$outlineCullState", "cullState", null);
	}

	@Test
	void a3EmptyTextureStateShardAccessorInvokesCutoutTexture() throws Exception {
		assertImplements(EMPTY_TEXTURE_SHARD, EmptyTextureStateShardAccessor.class);
		assertAccessorBody(transformed(EMPTY_TEXTURE_SHARD), "glintsmith$outlineCutoutTexture", null, "cutoutTexture");
	}

	// ---- the configuration itself

	@Test
	void mixinConfigListsExactlyTheOutlineClassesAndRequiresNothing() throws IOException {
		JsonObject config = readJson("/" + CONFIG);
		assertEquals("navrelis.glintsmith.mixin", config.get("package").getAsString());
		assertEquals("JAVA_21", config.get("compatibilityLevel").getAsString());
		assertEquals(0, config.getAsJsonObject("injectors").get("defaultRequire").getAsInt());
		assertFalse(config.has("mixins"), "outline mixins are client only");
		assertFalse(config.has("server"), "outline mixins are client only");

		Set<String> listed = new TreeSet<>();
		for (JsonElement element : config.getAsJsonArray("client")) {
			listed.add(element.getAsString());
		}
		assertEquals(MIXIN_CLASSES, listed);
		for (String name : listed) {
			assertNotNull(OutlineMixinApplicationTest.class.getResource(classResource(name)), "missing class for " + name);
		}
	}

	@Test
	void fabricModJsonRegistersTheOutlineConfig() throws IOException {
		// Every mod on the test classpath has a fabric.mod.json: pick ours by id, not the first one found.
		JsonObject ours = null;
		Enumeration<URL> urls = OutlineMixinApplicationTest.class.getClassLoader().getResources("fabric.mod.json");
		while (urls.hasMoreElements() && ours == null) {
			try (InputStream in = urls.nextElement().openStream()) {
				JsonElement parsed = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8));
				if (parsed.isJsonObject()) {
					JsonObject candidate = parsed.getAsJsonObject();
					if (candidate.has("id") && "glintsmith".equals(candidate.get("id").getAsString())) {
						ours = candidate;
					}
				}
			}
		}
		assertNotNull(ours, "no fabric.mod.json with id glintsmith on the classpath");
		// An entry is either a plain string or {"config": "...", "environment": "..."}.
		List<String> names = new ArrayList<>();
		for (JsonElement entry : ours.getAsJsonArray("mixins")) {
			if (entry.isJsonPrimitive()) {
				names.add(entry.getAsString());
			} else if (entry.isJsonObject() && entry.getAsJsonObject().has("config")) {
				JsonObject object = entry.getAsJsonObject();
				if (object.has("environment")) {
					String environment = object.get("environment").getAsString();
					assertTrue("client".equals(environment) || "*".equals(environment),
						object.get("config").getAsString() + " must load on the client, not " + environment);
				}
				names.add(object.get("config").getAsString());
			}
		}
		assertTrue(names.contains(CONFIG), "fabric.mod.json mixins: " + names);
	}

	@Test
	void everyInjectorStatesRequireZeroAndNoOrdinal() throws IOException {
		int injectors = 0;
		for (String name : MIXIN_CLASSES) {
			ClassNode node = new ClassNode();
			try (InputStream in = OutlineMixinApplicationTest.class.getResourceAsStream(classResource(name))) {
				assertNotNull(in, "missing class for " + name);
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
					if (a.desc.endsWith("/WrapMethod;") || a.desc.endsWith("/Inject;") || a.desc.endsWith("/ModifyVariable;")
						|| a.desc.endsWith("/Redirect;") || a.desc.endsWith("/WrapOperation;")) {
						injectors++;
						assertEquals(Integer.valueOf(0), annotationValue(a, "require"), name + "." + m.name + " must declare require = 0");
						assertFalse(mentionsKey(a, "ordinal"), name + "." + m.name + " must not pin an ordinal");
					}
				}
			}
		}
		assertEquals(INJECTORS, injectors, "expected O1 to O5 (WrapMethod), O6 x3 and O7 x3 (Inject)");
	}

	// ---- helpers

	/** The transformed bytecode of a class, as the loader defines it. */
	private static ClassNode transformed(String name) throws Exception {
		Class.forName(name, false, OutlineMixinApplicationTest.class.getClassLoader()); // applies the mixins once
		IMixinTransformer transformer = (IMixinTransformer) MixinEnvironment.getCurrentEnvironment().getActiveTransformer();
		byte[] raw = FabricLauncherBase.getLauncher().getClassByteArray(name, false);
		assertNotNull(raw, "no bytes for " + name);
		byte[] out = transformer.transformClassBytes(name, name, raw);
		ClassNode node = new ClassNode();
		new ClassReader(out).accept(node, 0);
		return node;
	}

	private static String classResource(String mixin) {
		return "/navrelis/glintsmith/mixin/" + mixin.replace('.', '/') + ".class";
	}

	private static MethodNode method(ClassNode node, String name, String desc) {
		for (MethodNode m : node.methods) {
			if (m.name.equals(name) && m.desc.equals(desc)) {
				return m;
			}
		}
		throw new AssertionError(node.name + " has no method " + name + desc);
	}

	/**
	 * {@code @WrapMethod}: the target, or the wrapped copy another wrapper on the same method left behind, calls our
	 * handler, and the original body now lives in a {@code $mixinextras$wrapped} method with the same descriptor.
	 */
	private static void assertWrapped(ClassNode node, String name, String desc, String handlerPart) {
		boolean called = false;
		boolean wrappedBody = false;
		for (MethodNode m : node.methods) {
			if (!m.desc.equals(desc)) {
				continue;
			}
			boolean wrappedCopy = m.name.startsWith(name + "$mixinextras$wrapped");
			wrappedBody |= wrappedCopy;
			if (m.name.equals(name) || wrappedCopy) {
				called |= callsExistingHandler(node, m, handlerPart);
			}
		}
		assertTrue(called, node.name + "." + name + desc + " does not call a handler containing '" + handlerPart + "': not applied");
		assertTrue(wrappedBody, node.name + "." + name + desc + " was not wrapped (no $mixinextras$wrapped method)");
	}

	private static void assertCallsExistingHandler(ClassNode node, MethodNode m, String handlerPart) {
		assertTrue(callsExistingHandler(node, m, handlerPart),
			node.name + "." + m.name + m.desc + " does not call a handler containing '" + handlerPart + "': not applied");
	}

	/** Whether {@code m} calls a method of its own class whose name contains {@code handlerPart}, and that method exists. */
	private static boolean callsExistingHandler(ClassNode node, MethodNode m, String handlerPart) {
		for (AbstractInsnNode insn : m.instructions) {
			if (insn instanceof MethodInsnNode call && call.owner.equals(node.name) && call.name.contains(handlerPart)) {
				for (MethodNode h : node.methods) {
					if (h.name.equals(call.name) && h.desc.equals(call.desc)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/** Names of the methods {@code m} calls, in bytecode order. */
	private static List<String> calls(MethodNode m) {
		List<String> names = new ArrayList<>();
		for (AbstractInsnNode insn : m.instructions) {
			if (insn instanceof MethodInsnNode call) {
				names.add(call.name);
			}
		}
		return names;
	}

	private static int indexOfHandler(List<String> calls, String handlerPart) {
		for (int i = 0; i < calls.size(); i++) {
			if (calls.get(i).contains(handlerPart)) {
				return i;
			}
		}
		return -1;
	}

	private static int lastIndexOfHandler(List<String> calls, String handlerPart) {
		for (int i = calls.size() - 1; i >= 0; i--) {
			if (calls.get(i).contains(handlerPart)) {
				return i;
			}
		}
		return -1;
	}

	private static void assertImplements(String target, Class<?> accessor) throws ClassNotFoundException {
		Class<?> type = Class.forName(target, false, OutlineMixinApplicationTest.class.getClassLoader());
		assertTrue(accessor.isAssignableFrom(type), accessor.getSimpleName() + " was not applied to " + target);
	}

	/** The accessor method exists in the target and reads {@code field} or calls {@code invoked}. */
	private static void assertAccessorBody(ClassNode node, String accessor, String field, String invoked) {
		MethodNode m = null;
		for (MethodNode candidate : node.methods) {
			if (candidate.name.equals(accessor)) {
				m = candidate;
			}
		}
		assertNotNull(m, node.name + " has no " + accessor);
		boolean ok = false;
		for (AbstractInsnNode insn : m.instructions) {
			if (field != null && insn instanceof FieldInsnNode f && f.name.equals(field) && f.owner.equals(node.name)) {
				ok = true;
			}
			if (invoked != null && insn instanceof MethodInsnNode call && call.name.equals(invoked)) {
				ok = true;
			}
		}
		assertTrue(ok, accessor + " does not " + (field != null ? "read " + field : "call " + invoked));
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
			for (Object v : list) {
				if (mentionsKey(v, key)) {
					return true;
				}
			}
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
		try (InputStream in = OutlineMixinApplicationTest.class.getResourceAsStream(resource)) {
			assertNotNull(in, "missing resource " + resource);
			return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
		}
	}
}
