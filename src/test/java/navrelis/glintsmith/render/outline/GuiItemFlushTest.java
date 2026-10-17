package navrelis.glintsmith.render.outline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.renderer.MultiBufferSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The end of an item render ({@link OutlineSystem#finishItem}). With HUD batching (ImmediatelyFast) the hotbar
 * sprite, the slots drawn so far and the outlined item are still in the batch when the item render ends; the GUI
 * composite's first draw call, made with the mask target bound, would make the batch draw into the mask and the
 * hotbar would vanish. So an outlined GUI item draws what its buffer source holds before the composite, and nothing
 * else changes.
 */
class GuiItemFlushTest {
	private final MultiBufferSource delegate = type -> {
		throw new AssertionError("the delegate is only flushed here, never drawn into");
	};
	private final List<MultiBufferSource> flushed = new ArrayList<>();
	private final TeePool pool = new TeePool();
	private PhaseState gui;
	private PhaseState world;

	@BeforeEach
	void setUp() {
		OutlineFailsafe.setOnTrip(null);
		OutlineFailsafe.resetForTests();
		gui = new PhaseState(OutlinePhase.GUI, false);
		world = new PhaseState(OutlinePhase.WORLD, true);
	}

	@AfterEach
	void tearDown() {
		gui.maskSource.close();
		world.maskSource.close();
		OutlineFailsafe.resetForTests();
	}

	@Test
	void outlinedGuiItemIsDrawnBeforeItsComposite() {
		TeeBufferSource tee = pool.acquire(delegate, gui, 1);
		gui.hasGeometry = true;
		OutlineSystem.finishItem(tee, pool, source -> {
			assertFalse(tee.active(), "the tee is released before the buffers are drawn");
			flushed.add(source);
		});
		assertEquals(1, flushed.size(), "exactly one flush per outlined GUI item");
		assertSame(delegate, flushed.get(0), "the flush targets the buffer source the item was handed (the GUI's)");
		assertEquals(0, pool.inUse());
		assertTrue(gui.hasGeometry, "the mask geometry stays for the composite");
	}

	@Test
	void guiItemWithoutMaskGeometryKeepsTheBatch() {
		TeeBufferSource tee = pool.acquire(delegate, gui, 1);
		OutlineSystem.finishItem(tee, pool, flushed::add);
		assertTrue(flushed.isEmpty(), "nothing to composite, so nothing to flush");
		assertEquals(0, pool.inUse());
	}

	@Test
	void worldAndHandItemsAreNeverFlushedEarly() {
		TeeBufferSource tee = pool.acquire(delegate, world, 1);
		world.hasGeometry = true;
		OutlineSystem.finishItem(tee, pool, flushed::add);
		assertTrue(flushed.isEmpty(), "world items batch until the world composite point");
		assertEquals(0, pool.inUse());
	}

	@Test
	void nestedRenderFlushesOnlyWhenTheOwnerEnds() {
		TeeBufferSource tee = pool.acquire(delegate, gui, 1);
		gui.hasGeometry = true;
		tee.enterNested();
		OutlineSystem.finishItem(tee, pool, flushed::add);
		assertTrue(flushed.isEmpty(), "a nested item render inside the outlined item must not flush");
		assertTrue(tee.active(), "and must not release the owner's tee");
		OutlineSystem.finishItem(tee, pool, flushed::add);
		assertEquals(1, flushed.size());
		assertFalse(tee.active());
	}

	@Test
	void trippedFailsafeAddsNoFlush() {
		TeeBufferSource tee = pool.acquire(delegate, gui, 1);
		gui.hasGeometry = true;
		OutlineFailsafe.trip("test", new IllegalStateException("forced"));
		OutlineSystem.finishItem(tee, pool, flushed::add);
		assertTrue(flushed.isEmpty(), "no composite follows, so vanilla behaviour is kept");
		assertEquals(0, pool.inUse());
	}
}
