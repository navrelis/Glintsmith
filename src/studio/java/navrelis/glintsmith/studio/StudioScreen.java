package navrelis.glintsmith.studio;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.brigadier.StringReader;
import com.mojang.math.Axis;
import navrelis.glintsmith.config.GlintsmithConfig;
import navrelis.glintsmith.modelfix.GapFix;
import navrelis.glintsmith.render.glint.GlintFailsafe;
import navrelis.glintsmith.render.outline.OutlineFailsafe;
import navrelis.glintsmith.render.outline.OutlineSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

/**
 * Runs a scene: one studio frame per game frame. Each studio frame draws the subjects twice with identical state and
 * clock, over pure black and over pure white, reads both back and hands them to the {@link FrameWriter}.
 * <p>
 * Render target: the game's main render target is resized to the scene size times the supersample factor (the
 * window keeps its size and shows a scaled preview). Rendering into the main target, not a private one, is what
 * lets the real outline compositor work unchanged: it always composites onto the main target.
 * <p>
 * Pipeline: items with the GUI display context are drawn inside the outline system's GUI phase
 * ({@code OutlineSystem.beginGuiItem / endGuiItem}, exactly what vanilla {@code GuiGraphics.renderItem} is wrapped in);
 * armor stands and items with any other display context inside its WORLD phase ({@link PipelineBridge}). Both draw
 * through the main buffer source, where the glint system swaps the glint render types. All of it goes through the
 * normal {@code ItemRenderer.render} / armor layer hooks of Glintsmith.
 * <p>
 * Clock: the time of the next studio frame is set at the end of the previous game frame, so the frame-start hooks of
 * the glint and outline systems read it; see {@link StudioClock}.
 */
final class StudioScreen extends Screen {
	private static final long MAX_RELOAD_WAIT_NANOS = 120_000_000_000L;

	private record Prop(Scene.Subject spec, @Nullable ItemStack stack, @Nullable ArmorStand stand) {
	}

	private record Job(int index, Scene.Look look, float angle, Scene.Camera camera, List<Prop> props, double time,
		@Nullable String name) {
	}

	private final Scene scene;
	private final Path out;
	@Nullable
	private List<Job> jobs;
	private final Map<String, GlintsmithConfig> frameConfigs = new LinkedHashMap<>();
	private final Map<String, GlintsmithConfig> subjectConfigs = new LinkedHashMap<>();
	private final Map<List<Scene.Subject>, List<Prop>> propCache = new IdentityHashMap<>();
	@Nullable
	private FrameWriter writer;
	private int cursor;
	private int clockFor = -1;
	@Nullable
	private Scene.Look applied;
	private int settle;
	private int reloadTarget = -1;
	private long reloadWaitStart;
	private boolean finished;
	private long renderNanos;
	private long startNanos;

	StudioScreen(Scene scene, Path out) {
		super(Component.literal("Glintsmith Studio"));
		this.scene = scene;
		this.out = out;
	}

	@Override
	public boolean isPauseScreen() {
		return true;
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		if (finished) {
			return;
		}
		try {
			step();
		} catch (Throwable t) {
			StudioMod.fail("rendering failed at frame " + (cursor + 1) + ": " + t, t);
			finish(false);
		}
	}

	private void step() throws Exception {
		if (jobs == null) {
			prepare();
		}
		List<Job> list = jobs;
		// The clock of this job was set during an earlier game frame, so this frame started with the job's time.
		boolean armed = clockFor == cursor;
		try {
			ensureTargetSize();
			if (reloadTarget >= 0) {
				if (StudioMod.reloads() >= reloadTarget) {
					reloadTarget = -1;
					settle = scene.settleFrames;
				} else if (System.nanoTime() - reloadWaitStart > MAX_RELOAD_WAIT_NANOS) {
					throw new IllegalStateException("the resource reload for the gap fix of look '" + applied.id() + "' did not happen within "
						+ MAX_RELOAD_WAIT_NANOS / 1_000_000_000L + " s");
				}
				return;
			}
			if (settle > 0) {
				settle--;
				return;
			}
			Job job = list.get(cursor);
			if (applied != job.look()) {
				applyLook(job.look());
				return;
			}
			if (!armed) {
				return;
			}
			long t0 = System.nanoTime();
			renderJob(job);
			renderNanos += System.nanoTime() - t0;
			if (OutlineFailsafe.tripped() || GlintFailsafe.tripped()) {
				throw new IllegalStateException("a Glintsmith failsafe tripped (outline " + OutlineFailsafe.tripped() + ", glint "
					+ GlintFailsafe.tripped() + "); see the log above");
			}
			writer.addFrame(job.index(), job.look().id(), job.look().caption(), job.name());
			cursor++;
			if (cursor % 30 == 0 || cursor == list.size()) {
				StudioMod.info("Frame {}/{}", cursor, list.size());
			}
			if (cursor >= list.size()) {
				finish(true);
			}
		} finally {
			if (!finished && cursor < list.size()) {
				StudioClock.set(list.get(cursor).time());
				clockFor = cursor;
			}
		}
	}

	// ---------------------------------------------------------------- setup

	private void prepare() throws Exception {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) {
			throw new IllegalStateException("no world loaded");
		}
		RegistryAccess registries = mc.level.registryAccess();
		for (Scene.Look look : scene.looks.values()) {
			GlintsmithConfig config = Looks.build(look, scene.supersample, null);
			frameConfigs.put(look.id(), config);
		}
		List<Job> list = new ArrayList<>();
		int index = 1;
		if (scene.type == Scene.Type.TURNTABLE) {
			List<Prop> props = props(scene.subjects, registries);
			for (String lookId : scene.sequence) {
				Scene.Look look = scene.looks.get(lookId);
				for (int f = 0; f < scene.framesPerLook; f++) {
					float angle = (float) (360.0 * scene.turnsPerLook * f / scene.framesPerLook);
					list.add(new Job(index, look, angle, scene.camera, props, time(index), null));
					index++;
				}
			}
		} else {
			for (Scene.Shot shot : scene.shots) {
				List<Prop> props = props(shot.subjects() == null ? scene.subjects : shot.subjects(), registries);
				Scene.Camera camera = shot.camera() == null ? scene.camera : shot.camera();
				double t = shot.time() == null ? time(index) : shot.time() + scene.timeOffset;
				list.add(new Job(index, scene.looks.get(shot.look()), shot.yaw(), camera, props, t,
					shot.name() == null ? "shot_" + FrameWriter.file(index) : shot.name()));
				index++;
			}
		}
		jobs = list;
		writer = new FrameWriter(out);
		startNanos = System.nanoTime();
		StudioMod.info("Rendering {} frames at {}x{} (output {}x{}).", list.size(), scene.renderWidth(), scene.renderHeight(),
			scene.width, scene.height);
	}

	private double time(int index) {
		return scene.timeOffset + (index - 1) / (double) scene.fps;
	}

	private List<Prop> props(List<Scene.Subject> subjects, RegistryAccess registries) throws Exception {
		List<Prop> cached = propCache.get(subjects);
		if (cached != null) {
			return cached;
		}
		Minecraft mc = Minecraft.getInstance();
		List<Prop> props = new ArrayList<>();
		for (int i = 0; i < subjects.size(); i++) {
			Scene.Subject s = subjects.get(i);
			String path = "subjects[" + i + "]";
			if (s.kind() == Scene.Kind.ITEM) {
				props.add(new Prop(s, stack(s.item(), registries, path + ".item"), null));
			} else {
				ArmorStand stand = new ArmorStand(EntityType.ARMOR_STAND, mc.level);
				CompoundTag tag = s.nbt() == null ? new CompoundTag() : TagParser.parseTag(s.nbt());
				if (!tag.contains("ShowArms")) {
					tag.putBoolean("ShowArms", true);
				}
				stand.readAdditionalSaveData(tag);
				for (Map.Entry<String, JsonElement> e : s.equipment().entrySet()) {
					EquipmentSlot slot;
					try {
						slot = EquipmentSlot.byName(e.getKey());
					} catch (IllegalArgumentException ex) {
						throw new IllegalArgumentException(path + ".equipment: unknown slot '" + e.getKey()
							+ "' (head, chest, legs, feet, mainhand, offhand)");
					}
					stand.setItemSlot(slot, stack(e.getValue(), registries, path + ".equipment." + e.getKey()));
				}
				stand.setPos(0.0, 0.0, 0.0);
				stand.setOldPosAndRot();
				stand.setYRot(0.0f);
				stand.yRotO = 0.0f;
				stand.yBodyRot = 0.0f;
				stand.yBodyRotO = 0.0f;
				stand.yHeadRot = 0.0f;
				stand.yHeadRotO = 0.0f;
				stand.lastHit = -1_000_000L; // no hit wobble
				props.add(new Prop(s, null, stand));
			}
		}
		props = List.copyOf(props);
		propCache.put(subjects, props);
		return props;
	}

	/**
	 * An item spec: {@code "minecraft:netherite_sword"} or full {@code /give} syntax with components, or an object
	 * {@code {"item": ..., "enchantments": {"minecraft:sharpness": 5}, "glint": true, "count": 1}}.
	 */
	private static ItemStack stack(@Nullable JsonElement spec, RegistryAccess registries, String path) throws Exception {
		if (spec == null || spec.isJsonNull()) {
			throw new IllegalArgumentException(path + ": missing item");
		}
		String itemText;
		JsonObject options = null;
		if (spec.isJsonPrimitive()) {
			itemText = spec.getAsString();
		} else {
			options = Scene.asObject(spec, path);
			if (!options.has("item")) {
				throw new IllegalArgumentException(path + ".item: missing");
			}
			itemText = options.get("item").getAsString();
		}
		ItemStack stack;
		try {
			ItemParser.ItemResult result = new ItemParser(registries).parse(new StringReader(itemText));
			int count = options != null && options.has("count") ? options.get("count").getAsInt() : 1;
			stack = new ItemInput(result.item(), result.components()).createItemStack(count, false);
		} catch (Exception e) {
			throw new IllegalArgumentException(path + ": cannot parse item '" + itemText + "': " + e.getMessage(), e);
		}
		if (options != null && options.has("enchantments")) {
			var lookup = registries.lookupOrThrow(Registries.ENCHANTMENT);
			for (Map.Entry<String, JsonElement> e : Scene.asObject(options.get("enchantments"), path + ".enchantments").entrySet()) {
				ResourceKey<Enchantment> key = ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.parse(e.getKey()));
				Holder<Enchantment> holder = lookup.get(key).orElseThrow(
					() -> new IllegalArgumentException(path + ".enchantments: unknown enchantment '" + e.getKey() + "'"));
				stack.enchant(holder, e.getValue().getAsInt());
			}
		}
		if (options != null && options.has("glint")) {
			stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, options.get("glint").getAsBoolean());
		}
		return stack;
	}

	/**
	 * Makes the look's config current. A gap fix change makes Glintsmith reload the resources (the models are baked
	 * with it); the next frames wait for that reload.
	 */
	private void applyLook(Scene.Look look) throws Exception {
		GapFix.Settings before = GapFix.current();
		PipelineBridge.applyConfig(frameConfigs.get(look.id()));
		applied = look;
		subjectConfigs.clear(); // built lazily, with the gap fix of this look
		settle = scene.settleFrames;
		if (GapFix.requiresReload(before, GapFix.current())) {
			reloadTarget = StudioMod.reloads() + 1;
			reloadWaitStart = System.nanoTime();
			StudioMod.info("Look '{}': gap fix changed, waiting for the resource reload.", look.id());
		} else {
			StudioMod.info("Look '{}' applied.", look.id());
		}
	}

	private GlintsmithConfig subjectConfig(String lookId) {
		GlintsmithConfig config = subjectConfigs.get(lookId);
		if (config == null) {
			config = Looks.build(scene.looks.get(lookId), scene.supersample, frameConfigs.get(applied.id()).gapFix);
			subjectConfigs.put(lookId, config);
		}
		return config;
	}

	private void ensureTargetSize() {
		RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
		int w = scene.renderWidth();
		int h = scene.renderHeight();
		if (main.width != w || main.height != h) {
			main.resize(w, h, Minecraft.ON_OSX);
		}
	}

	// ---------------------------------------------------------------- rendering

	private void renderJob(Job job) throws Exception {
		Minecraft mc = Minecraft.getInstance();
		RenderTarget main = mc.getMainRenderTarget();
		Matrix4fStack modelView = RenderSystem.getModelViewStack();
		RenderSystem.backupProjectionMatrix();
		modelView.pushMatrix();
		try {
			modelView.identity();
			RenderSystem.applyModelViewMatrix();
			main.bindWrite(true);
			setProjection(job.camera(), mc);
			FogRenderer.setupNoFog();
			RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
			RenderSystem.setShaderLights(new Vector3f(scene.keyLight).normalize(), new Vector3f(scene.fillLight).normalize());
			RenderSystem.disableScissor();
			for (int pass = 0; pass < 2; pass++) {
				boolean white = pass == 1;
				float c = white ? 1.0f : 0.0f;
				main.bindWrite(true);
				RenderSystem.colorMask(true, true, true, true);
				RenderSystem.depthMask(true);
				RenderSystem.clearColor(c, c, c, 1.0f);
				RenderSystem.clearDepth(1.0);
				RenderSystem.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
				drawSubjects(job, mc);
				NativeImage image = Screenshot.takeScreenshot(main);
				writer.write(image, job.index(), white);
			}
		} finally {
			modelView.popMatrix();
			RenderSystem.applyModelViewMatrix();
			RenderSystem.restoreProjectionMatrix();
			main.bindWrite(true);
		}
	}

	/**
	 * Orthographic by default. Its depth range is chosen so that window depth and depth slope at the camera target
	 * match the vanilla perspective projection (near 0.05, far {@code getDepthFar()}) that the outline compositor uses
	 * to turn depth into blocks; the world-phase depth tolerances then mean what they mean in game around the subject.
	 */
	private void setProjection(Scene.Camera camera, Minecraft mc) {
		float aspect = scene.width / (float) scene.height;
		float near = 0.05f;
		float far = mc.gameRenderer.getDepthFar();
		Matrix4f projection = new Matrix4f();
		if (camera.orthographic()) {
			float halfH = camera.viewHeight() / camera.zoom() * 0.5f;
			float halfW = halfH * aspect;
			double d = camera.distance();
			double windowZ = far / (far - near) * (1.0 - near / d);
			double range = (far - near) * d * d / (far * near);
			double orthoNear = d - windowZ * range;
			projection.setOrtho(-halfW, halfW, -halfH, halfH, (float) orthoNear, (float) (orthoNear + range));
			RenderSystem.setProjectionMatrix(projection, VertexSorting.ORTHOGRAPHIC_Z);
		} else {
			double half = Math.toRadians(camera.fov()) * 0.5;
			float fov = (float) (2.0 * Math.atan(Math.tan(half) / camera.zoom()));
			projection.setPerspective(fov, aspect, near, Math.max(far, camera.distance() * 4.0f));
			RenderSystem.setProjectionMatrix(projection, VertexSorting.DISTANCE_TO_ORIGIN);
		}
	}

	private void drawSubjects(Job job, Minecraft mc) throws Exception {
		MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
		buffers.endBatch();
		Scene.Camera camera = job.camera();
		PoseStack pose = new PoseStack();
		pose.translate(0.0f, 0.0f, -camera.distance());
		pose.mulPose(Axis.ZP.rotationDegrees(camera.roll()));
		pose.mulPose(Axis.XP.rotationDegrees(camera.pitch()));
		pose.mulPose(Axis.YP.rotationDegrees(-camera.yaw()));
		pose.translate(-camera.target().x, -camera.target().y, -camera.target().z);
		EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
		for (Prop prop : job.props()) {
			Scene.Subject s = prop.spec();
			boolean subjectLook = s.look() != null;
			if (subjectLook) {
				PipelineBridge.applyConfig(subjectConfig(s.look()));
			}
			pose.pushPose();
			Vector3f p = s.pos();
			pose.translate(p.x, p.y, p.z);
			pose.mulPose(Axis.YP.rotationDegrees((s.spin() ? job.angle() : 0.0f) + s.phase()));
			pose.mulPose(Axis.YP.rotationDegrees(s.rotation().y));
			pose.mulPose(Axis.XP.rotationDegrees(s.rotation().x));
			pose.mulPose(Axis.ZP.rotationDegrees(s.rotation().z));
			pose.scale(s.scale(), s.scale(), s.scale());
			pose.translate(-s.pivot().x, -s.pivot().y, -s.pivot().z);
			if (prop.stack() != null && s.context() == ItemDisplayContext.GUI) {
				// The path of GuiGraphics.renderItem: one GUI outline phase per item, composited after its flush.
				OutlineSystem.beginGuiItem();
				try {
					mc.getItemRenderer().renderStatic(prop.stack(), ItemDisplayContext.GUI, LightTexture.FULL_BRIGHT,
						OverlayTexture.NO_OVERLAY, pose, buffers, mc.level, 0);
					buffers.endBatch();
				} finally {
					OutlineSystem.endGuiItem();
				}
			} else {
				boolean world = PipelineBridge.openWorldPhase();
				try {
					if (prop.stack() != null) {
						mc.getItemRenderer().renderStatic(prop.stack(), s.context(), LightTexture.FULL_BRIGHT,
							OverlayTexture.NO_OVERLAY, pose, buffers, mc.level, 0);
					} else {
						// Centre the stand (2 blocks tall) on the subject position.
						pose.translate(0.0f, -1.0f, 0.0f);
						dispatcher.setRenderShadow(false);
						dispatcher.render(prop.stand(), 0.0, 0.0, 0.0, 0.0f, 1.0f, pose, buffers, LightTexture.FULL_BRIGHT);
					}
					buffers.endBatch();
				} finally {
					if (world) {
						PipelineBridge.compositeWorldPhase();
					}
				}
			}
			pose.popPose();
			if (subjectLook) {
				PipelineBridge.applyConfig(frameConfigs.get(applied.id()));
			}
		}
		buffers.endBatch();
	}

	// ---------------------------------------------------------------- end

	private void finish(boolean success) {
		finished = true;
		StudioClock.release();
		Minecraft mc = Minecraft.getInstance();
		try {
			if (success && writer != null) {
				List<Scene.Look> used = new ArrayList<>();
				for (Job job : jobs) {
					if (!used.contains(job.look())) {
						used.add(job.look());
					}
				}
				writer.finish(scene, used, FrameWriter.commit(StudioMod.repoDir()));
				double total = (System.nanoTime() - startNanos) / 1.0e9;
				StudioMod.info("Done: {} frames in {} ({} s, {} ms render per frame pair, {} ms per frame overall).",
					jobs.size(), out, String.format("%.1f", total), String.format("%.1f", renderNanos / 1.0e6 / jobs.size()),
					String.format("%.1f", total * 1000.0 / jobs.size()));
			} else if (writer != null) {
				writer.abort();
			}
		} catch (Throwable t) {
			StudioMod.fail("writing the output failed: " + t, t);
		}
		mc.getMainRenderTarget().resize(mc.getWindow().getWidth(), mc.getWindow().getHeight(), Minecraft.ON_OSX);
		mc.stop();
	}
}
