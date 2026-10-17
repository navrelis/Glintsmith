package navrelis.glintsmith.render.outline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The capture path without GL: a {@link TeeBufferSource} over a recording buffer source, writing into a real
 * {@link MaskBufferSource} (native memory only). Checks what reaches the real buffers, what lands in the mask
 * (vertex count, style index, UV) and the screen bounds, plus the guards around the mask side.
 */
class TeeCaptureTest {
	private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/diamond_sword.png");
	/** POSITION_TEX_COLOR: position 12 bytes, UV0 8 bytes, colour 4 bytes. */
	private static final int MASK_VERTEX_SIZE = 24;
	private static final int UV_OFFSET = 12;
	private static final int COLOR_OFFSET = 20;

	private PhaseState phase;
	private final List<String> log = new ArrayList<>();
	private final RecordingSource source = new RecordingSource(log);

	@BeforeAll
	static void bootstrap() {
		GameBootstrap.ensure();
	}

	@BeforeEach
	void setUp() {
		OutlineFailsafe.setOnTrip(null);
		OutlineFailsafe.resetForTests();
		MaskTypes.clearCaches();
		phase = new PhaseState(OutlinePhase.WORLD, true);
		phase.bounds.snapshot(new Matrix4f(), new Matrix4f()); // identity: positions are NDC
	}

	@AfterEach
	void tearDown() {
		phase.maskSource.close();
		OutlineFailsafe.resetForTests();
	}

	@Test
	void texturedQuadReachesRealBufferAndMask() {
		TeePool pool = new TeePool();
		TeeBufferSource tee = pool.acquire(source, phase, 7);
		RenderType type = RenderType.entityCutoutNoCull(TEXTURE);

		VertexConsumer consumer = tee.getBuffer(type);
		assertInstanceOf(TeeVertexConsumer.class, consumer);
		assertSame(consumer, tee.getBuffer(type), "same type, same buffers: same tee consumer");

		quad(consumer, -0.5f, -0.5f, 0.5f, 0.5f);
		assertEquals(4, source.recorder(type).bulkVertices, "every vertex reaches the real buffer through its fast path");
		assertTrue(phase.hasGeometry);

		int[] region = new int[4];
		assertTrue(phase.bounds.toPixels(100, 100, 0, region));
		assertEquals(25, region[0]);
		assertEquals(25, region[1]);
		assertEquals(75, region[2]);
		assertEquals(75, region[3]);

		ByteBuffer vertices = drainMask(type);
		assertEquals(4 * MASK_VERTEX_SIZE, vertices.remaining());
		for (int v = 0; v < 4; v++) {
			int base = v * MASK_VERTEX_SIZE;
			assertEquals(7, vertices.get(base + COLOR_OFFSET) & 0xFF, "red channel holds the style index");
			assertEquals(0, vertices.get(base + COLOR_OFFSET + 1));
			assertEquals((byte) 255, vertices.get(base + COLOR_OFFSET + 3));
		}
		assertEquals(0.25f, vertices.getFloat(UV_OFFSET), "the mask samples the item's own UV");
		pool.release(tee);
	}

	@Test
	void glintAndUnmaskableTypesPassStraightThrough() {
		TeeBufferSource tee = new TeePool().acquire(source, phase, 1);
		assertSame(source.recorder(RenderType.glint()), tee.getBuffer(RenderType.glint()));
		assertSame(source.recorder(RenderType.entityGlint()), tee.getBuffer(RenderType.entityGlint()));
		assertSame(source.recorder(RenderType.lines()), tee.getBuffer(RenderType.lines()));
		assertFalse(phase.hasGeometry, "nothing maskable was requested");
	}

	@Test
	void untexturedSourceGetsTheDefaultUv() {
		TeeBufferSource tee = new TeePool().acquire(source, phase, 2);
		RenderType gui = RenderType.gui(); // POSITION_COLOR: no UV0 at all
		VertexConsumer consumer = tee.getBuffer(gui);
		for (int i = 0; i < 4; i++) {
			consumer.addVertex(i * 0.1f, 0.0f, 0.0f).setColor(255, 255, 255, 255);
		}
		ByteBuffer vertices = drainMask(gui);
		assertEquals(4 * MASK_VERTEX_SIZE, vertices.remaining(), "every mask vertex is complete");
		assertEquals(0.5f, vertices.getFloat(UV_OFFSET));
		assertEquals(0.5f, vertices.getFloat(UV_OFFSET + 4));
	}

	@Test
	void aVertexWithoutUvIsCompletedSoTheMaskNeverHoldsAHalfVertex() {
		TeePool pool = new TeePool();
		TeeBufferSource tee = pool.acquire(source, phase, 3);
		RenderType type = RenderType.entityCutout(TEXTURE);
		VertexConsumer consumer = tee.getBuffer(type);
		consumer.addVertex(0.0f, 0.0f, 0.0f).setUv(0.1f, 0.2f);
		consumer.addVertex(0.1f, 0.0f, 0.0f); // a sloppy renderer that never sets the UV
		consumer.addVertex(0.1f, 0.1f, 0.0f).setUv(0.3f, 0.4f).setUv(0.9f, 0.9f); // a second UV is ignored
		consumer.addVertex(0.0f, 0.1f, 0.0f);
		pool.release(tee); // completes the last vertex
		ByteBuffer vertices = drainMask(type);
		assertEquals(4 * MASK_VERTEX_SIZE, vertices.remaining());
		assertEquals(0.1f, vertices.getFloat(UV_OFFSET));
		assertEquals(0.5f, vertices.getFloat(MASK_VERTEX_SIZE + UV_OFFSET));
		assertEquals(0.3f, vertices.getFloat(2 * MASK_VERTEX_SIZE + UV_OFFSET));
		assertEquals(0.5f, vertices.getFloat(3 * MASK_VERTEX_SIZE + UV_OFFSET));
	}

	@Test
	void realIsWrittenBeforeTheMask() {
		List<String> order = new ArrayList<>();
		TeeVertexConsumer tee = new TeeVertexConsumer();
		tee.bind(new Recorder("real", order), new Recorder("mask", order), 1, false, phase.bounds);
		tee.addVertex(0.0f, 0.0f, 0.0f);
		tee.setUv(0.5f, 0.5f);
		tee.setColor(1, 2, 3, 4);
		tee.setNormal(0.0f, 1.0f, 0.0f);
		assertEquals(List.of("real:addVertex", "mask:addVertex", "mask:setColor", "real:setUv", "mask:setUv", "real:setColor",
			"real:setNormal"), order, "colour and normal never reach the mask; the mask colour is the style index");
	}

	@Test
	void aFailingMaskTripsTheFailsafeOnceAndTheRealBufferKeepsEverything() {
		Recorder real = new Recorder("real", log);
		TeeVertexConsumer tee = new TeeVertexConsumer();
		tee.bind(real, new Throwing(), 1, false, phase.bounds);
		tee.addVertex(0.0f, 0.0f, 0.0f, 0xFFFFFFFF, 0.0f, 0.0f, 0, 0, 0.0f, 1.0f, 0.0f);
		assertTrue(OutlineFailsafe.tripped());
		assertEquals(1, OutlineFailsafe.tripCount());
		for (int i = 0; i < 3; i++) {
			tee.addVertex(0.0f, 0.0f, 0.0f, 0xFFFFFFFF, 0.0f, 0.0f, 0, 0, 0.0f, 1.0f, 0.0f);
		}
		assertEquals(4, real.bulkVertices);
		assertEquals(1, OutlineFailsafe.tripCount());
	}

	@Test
	void releasedTeesKeepForwardingToTheRealBufferOnly() {
		TeePool pool = new TeePool();
		TeeBufferSource tee = pool.acquire(source, phase, 1);
		RenderType type = RenderType.entityCutout(TEXTURE);
		VertexConsumer consumer = tee.getBuffer(type);
		quad(consumer, 0.0f, 0.0f, 0.1f, 0.1f);
		pool.release(tee);
		assertFalse(tee.active());
		quad(consumer, 0.0f, 0.0f, 0.1f, 0.1f); // a renderer that kept the consumer
		assertSame(source.recorder(type), tee.getBuffer(type), "a released tee hands out the real buffer");
		assertEquals(8, source.recorder(type).bulkVertices);
		assertEquals(4 * MASK_VERTEX_SIZE, drainMask(type).remaining(), "the late quad did not reach the mask");
	}

	@Test
	void poolReusesTeesAndNestedRendersDoNotReleaseTheOuterOne() {
		TeePool pool = new TeePool();
		TeeBufferSource first = pool.acquire(source, phase, 1);
		pool.release(first);
		assertEquals(0, pool.inUse());
		TeeBufferSource again = pool.acquire(source, phase, 2);
		assertSame(first, again, "steady state allocates no tees");

		again.enterNested();
		assertTrue(again.exitNested(), "the nested end does not own the tee");
		assertFalse(again.exitNested(), "the owner's end releases it");

		TeeBufferSource inner = pool.acquire(source, phase, 3);
		assertEquals(2, pool.inUse());
		pool.release(again); // out of order
		assertEquals(1, pool.inUse());
		assertTrue(inner.active());
		pool.releaseAll();
		assertEquals(0, pool.inUse());
		assertFalse(inner.active());
	}

	@Test
	void flushesForwardToABufferSourceDelegateAndAreIgnoredOtherwise() {
		List<String> flushes = new ArrayList<>();
		try (ByteBufferBuilder shared = new ByteBufferBuilder(64)) {
			MultiBufferSource.BufferSource delegate = new MultiBufferSource.BufferSource(shared, new Object2ObjectLinkedOpenHashMap<>()) {
				@Override
				public void endBatch() {
					flushes.add("all");
				}

				@Override
				public void endLastBatch() {
					flushes.add("last");
				}

				@Override
				public void endBatch(RenderType type) {
					flushes.add(type == RenderType.solid() ? "solid" : "other");
				}
			};
			TeePool pool = new TeePool();
			TeeBufferSource tee = pool.acquire(delegate, phase, 1);
			tee.endBatch();
			tee.endLastBatch();
			tee.endBatch(RenderType.solid());
			assertEquals(List.of("all", "last", "solid"), flushes);

			TeeBufferSource plain = pool.acquire(source, phase, 2); // not a BufferSource: nothing to flush
			plain.endBatch();
			plain.endLastBatch();
			plain.endBatch(RenderType.solid());
			assertEquals(3, flushes.size());
		}
	}

	@Test
	void maskTypesPerPhaseAreCappedAndTrimmedAfterUse() {
		MaskBufferSource mask = phase.maskSource;
		for (int i = 0; i < MaskBufferSource.MAX_TYPES; i++) {
			RenderType maskType = MaskTypes.forSource(RenderType.entityCutout(texture(i)), true);
			assertNotNull(mask.bufferFor(maskType));
		}
		assertEquals(MaskBufferSource.MAX_TYPES, mask.typeCount());
		RenderType oneTooMany = MaskTypes.forSource(RenderType.entityCutout(texture(MaskBufferSource.MAX_TYPES)), true);
		assertNull(mask.bufferFor(oneTooMany), "beyond the cap a type is not masked");

		mask.trimIfFull();
		assertEquals(MaskBufferSource.MAX_TYPES, mask.typeCount(), "never trimmed while geometry is pending");
		phase.discard(); // discards the pending builders, then trims
		assertEquals(0, mask.typeCount());
		assertNotNull(mask.bufferFor(oneTooMany), "a new texture can be outlined again");
		phase.discard();
	}

	// ---- helpers

	private static ResourceLocation texture(int i) {
		return ResourceLocation.fromNamespaceAndPath("glintsmith", "test/cap_" + i + ".png");
	}

	/** Four vertices of an axis-aligned quad through the 11-argument bulk path, UVs 0.25..0.75. */
	private static void quad(VertexConsumer consumer, float x0, float y0, float x1, float y1) {
		consumer.addVertex(x0, y0, 0.0f, 0xFFFFFFFF, 0.25f, 0.25f, 0, 0xF000F0, 0.0f, 0.0f, 1.0f);
		consumer.addVertex(x1, y0, 0.0f, 0xFFFFFFFF, 0.75f, 0.25f, 0, 0xF000F0, 0.0f, 0.0f, 1.0f);
		consumer.addVertex(x1, y1, 0.0f, 0xFFFFFFFF, 0.75f, 0.75f, 0, 0xF000F0, 0.0f, 0.0f, 1.0f);
		consumer.addVertex(x0, y1, 0.0f, 0xFFFFFFFF, 0.25f, 0.75f, 0, 0xF000F0, 0.0f, 0.0f, 1.0f);
	}

	/**
	 * Builds the pending mask geometry of one source type and returns a copy of its vertex bytes. The builder is
	 * finished afterwards, so the test must not discard the phase again ({@link #tearDown} closes the source).
	 */
	private ByteBuffer drainMask(RenderType sourceType) {
		RenderType maskType = MaskTypes.forSource(sourceType, phase.depthTested);
		assertNotNull(maskType);
		BufferBuilder builder = (BufferBuilder) phase.maskSource.bufferFor(maskType);
		try (MeshData mesh = builder.build()) {
			assertNotNull(mesh, "no mask geometry");
			ByteBuffer src = mesh.vertexBuffer();
			ByteBuffer copy = ByteBuffer.allocate(src.remaining()).order(ByteOrder.nativeOrder());
			copy.put(src.duplicate());
			copy.flip();
			return copy;
		}
	}

	/** A buffer source that hands out one recorder per render type. */
	private static final class RecordingSource implements MultiBufferSource {
		private final Map<RenderType, Recorder> recorders = new IdentityHashMap<>();
		private final List<String> log;

		RecordingSource(List<String> log) {
			this.log = log;
		}

		Recorder recorder(RenderType type) {
			return recorders.computeIfAbsent(type, t -> new Recorder("real", log));
		}

		@Override
		public VertexConsumer getBuffer(RenderType type) {
			return recorder(type);
		}
	}

	private static final class Recorder implements VertexConsumer {
		private final String name;
		private final List<String> log;
		int bulkVertices;

		Recorder(String name, List<String> log) {
			this.name = name;
			this.log = log;
		}

		@Override
		public VertexConsumer addVertex(float x, float y, float z) {
			log.add(name + ":addVertex");
			return this;
		}

		@Override
		public void addVertex(float x, float y, float z, int color, float u, float v, int overlay, int light, float nx, float ny,
			float nz) {
			bulkVertices++;
		}

		@Override
		public VertexConsumer setColor(int r, int g, int b, int a) {
			log.add(name + ":setColor");
			return this;
		}

		@Override
		public VertexConsumer setUv(float u, float v) {
			log.add(name + ":setUv");
			return this;
		}

		@Override
		public VertexConsumer setUv1(int u, int v) {
			return this;
		}

		@Override
		public VertexConsumer setUv2(int u, int v) {
			return this;
		}

		@Override
		public VertexConsumer setNormal(float x, float y, float z) {
			log.add(name + ":setNormal");
			return this;
		}
	}

	private static final class Throwing implements VertexConsumer {
		@Override
		public VertexConsumer addVertex(float x, float y, float z) {
			throw new IllegalStateException("mask buffer broken");
		}

		@Override
		public VertexConsumer setColor(int r, int g, int b, int a) {
			throw new IllegalStateException("mask buffer broken");
		}

		@Override
		public VertexConsumer setUv(float u, float v) {
			throw new IllegalStateException("mask buffer broken");
		}

		@Override
		public VertexConsumer setUv1(int u, int v) {
			return this;
		}

		@Override
		public VertexConsumer setUv2(int u, int v) {
			return this;
		}

		@Override
		public VertexConsumer setNormal(float x, float y, float z) {
			return this;
		}
	}
}
