package navrelis.glintsmith.render.glint;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.client.renderer.MultiBufferSource;

/**
 * The parts of the facade that need no OpenGL: the entry points are fast exits while the settings are vanilla or the
 * main source has not been captured, and they never throw (a failure would trip the failsafe instead).
 */
class GlintSystemTest {
	@BeforeEach
	@AfterEach
	void reset() {
		GlintFailsafe.resetForTests();
		GlintContext.reset();
	}

	@Test
	void substituteLeavesTheRequestedTypeAloneWhileEverythingIsVanilla() {
		// With the default settings (anyCustom is false) the source and the type are not even looked at.
		assertNull(GlintSystem.substitute(null, null));
		assertFalse(GlintFailsafe.tripped());
	}

	@Test
	void foreignSourcesAreIgnoredBeforeTheMainSourceExists() {
		try (ByteBufferBuilder bytes = new ByteBufferBuilder(256)) {
			MultiBufferSource.BufferSource other = MultiBufferSource.immediate(bytes);
			assertDoesNotThrow(() -> GlintSystem.afterEndBatch(other, null));
			assertNull(GlintSystem.substitute(other, null));
		}
		assertFalse(GlintFailsafe.tripped());
	}

	@Test
	void bufferedSubstituteLeavesTheRequestedTypeAloneWhileEverythingIsVanilla() {
		assertNull(GlintSystem.substituteBuffered(null));
		assertFalse(GlintFailsafe.tripped());
	}

	@Test
	void bufferedSwapNeedsCustomLooksAStartedClientNoShaderPackAndNoTrip() {
		assertTrue(GlintSystem.bufferedSwapAllowed(true, true, false, false));
		assertFalse(GlintSystem.bufferedSwapAllowed(false, true, false, false), "all vanilla");
		assertFalse(GlintSystem.bufferedSwapAllowed(true, false, false, false), "main source not captured yet");
		assertFalse(GlintSystem.bufferedSwapAllowed(true, true, true, false), "shader pack: world glint stays vanilla");
		assertFalse(GlintSystem.bufferedSwapAllowed(true, true, false, true), "failsafe tripped");
	}

	@Test
	void unknownBatchesAreIgnored() {
		assertDoesNotThrow(() -> GlintSystem.onBatchDrawn(null, null));
		assertFalse(GlintFailsafe.tripped());
	}

	/**
	 * ImmediatelyFast's main source is a subclass whose public {@code endBatch(RenderType)} never runs the vanilla body,
	 * so G5b cannot flush the slots there; drawing the vanilla entityGlintDirect batch must do it instead. A plain
	 * vanilla source keeps the G5b flush alone (no change of the vanilla path), and foreign sources never flush.
	 */
	@Test
	void drawingEntityGlintDirectFlushesOnlyForASubclassedMainSource() {
		try (ByteBufferBuilder a = new ByteBufferBuilder(256); ByteBufferBuilder b = new ByteBufferBuilder(256)) {
			MultiBufferSource.BufferSource vanilla = MultiBufferSource.immediate(a);
			MultiBufferSource.BufferSource subclass = new MultiBufferSource.BufferSource(b, new Object2ObjectLinkedOpenHashMap<>()) {
			};

			assertTrue(GlintSystem.flushesFromDraw(subclass, subclass, true), "subclassed main source (ImmediatelyFast)");
			assertFalse(GlintSystem.flushesFromDraw(subclass, subclass, false), "only after the vanilla entityGlintDirect");
			assertFalse(GlintSystem.flushesFromDraw(vanilla, vanilla, true), "vanilla class: G5b flushes");
			assertFalse(GlintSystem.flushesFromDraw(subclass, vanilla, true), "not the main source");
			assertFalse(GlintSystem.flushesFromDraw(subclass, null, true), "main source not captured yet");
			assertFalse(GlintSystem.flushesFromDraw(null, null, true));
		}
	}

	@Test
	void drawnBatchesOfForeignSourcesAreIgnoredBeforeTheMainSourceExists() {
		try (ByteBufferBuilder bytes = new ByteBufferBuilder(256)) {
			MultiBufferSource.BufferSource other = new MultiBufferSource.BufferSource(bytes, new Object2ObjectLinkedOpenHashMap<>()) {
			};
			assertDoesNotThrow(() -> GlintSystem.onBatchDrawn(other, null));
		}
		assertFalse(GlintFailsafe.tripped());
	}

	@Test
	void contextBalanceSurvivesTheEntryPoints() {
		GlintContext.pushItem(null, null);
		GlintSystem.substitute(null, null);
		GlintContext.pop();
		assertEquals(0, GlintContext.depth());
	}
}
