package navrelis.glintsmith.render.glint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.minecraft.resources.ResourceLocation;

/**
 * The pooling rules, with plain objects instead of render types (no GL context needed).
 */
class GlintSlotPoolTest {
	private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath("glintsmith", "glint/preset/rainbow");

	/** Marker standing in for a RenderType. */
	private static final class FakeType {
		final String name;

		FakeType(String name) {
			this.name = name;
		}
	}

	private int created;
	private GlintSlotPool<FakeType> pool;

	@BeforeEach
	void setUp() {
		created = 0;
		pool = new GlintSlotPool<>(new GlintSlotPool.Factory<>() {
			@Override
			public FakeType createSlotType(GlintSlot<FakeType> slot) {
				created++;
				return new FakeType(slot.kind.id + "_" + slot.index);
			}

			@Override
			public FakeType createOffType(GlintSlot<FakeType> slot) {
				created++;
				return new FakeType(slot.kind.id + "_off");
			}
		});
	}

	private static GlintParams look(float speed) {
		GlintParams p = new GlintParams();
		p.set(TEX, GlintParams.NO_TINT, speed, 1.0f, 1.0f, 10.0f);
		return p;
	}

	@Test
	void kindsAreCreatedLazily() {
		assertEquals(0, created);
		assertFalse(pool.isCreated(GlintKind.GLINT));
		pool.acquire(GlintKind.GLINT, look(2.0f), 1);
		assertTrue(pool.isCreated(GlintKind.GLINT));
		assertFalse(pool.isCreated(GlintKind.ENTITY_GLINT));
		assertEquals(GlintSlotPool.SLOTS_PER_KIND + 1, created, "32 look slots and one off type");
		assertEquals(GlintSlotPool.SLOTS_PER_KIND + 1, pool.count());
		pool.acquire(GlintKind.GLINT, look(3.0f), 1);
		assertEquals(GlintSlotPool.SLOTS_PER_KIND + 1, created, "no more types are created");
	}

	@Test
	void sameLookSharesASlot() {
		GlintSlot<FakeType> a = pool.acquire(GlintKind.GLINT, look(2.0f), 1);
		GlintSlot<FakeType> b = pool.acquire(GlintKind.GLINT, look(2.0f), 1);
		assertSame(a, b);
		assertTrue(a.inUse);
		assertEquals(1, a.params.speed == 2.0f ? 1 : 0);
	}

	@Test
	void differentLooksGetDifferentSlots() {
		GlintSlot<FakeType> a = pool.acquire(GlintKind.GLINT, look(2.0f), 1);
		GlintSlot<FakeType> b = pool.acquire(GlintKind.GLINT, look(3.0f), 1);
		assertNotSame(a, b);
		assertNotSame(a.type, b.type);
		assertEquals(2.0f, a.params.speed);
		assertEquals(3.0f, b.params.speed);
	}

	@Test
	void kindsDoNotShareSlots() {
		GlintSlot<FakeType> a = pool.acquire(GlintKind.GLINT, look(2.0f), 1);
		GlintSlot<FakeType> b = pool.acquire(GlintKind.GLINT_TRANSLUCENT, look(2.0f), 1);
		assertNotSame(a, b);
		assertEquals(GlintKind.GLINT, a.kind);
		assertEquals(GlintKind.GLINT_TRANSLUCENT, b.kind);
	}

	@Test
	void slotParamsAreCopiedNotAliased() {
		GlintParams scratch = look(2.0f);
		GlintSlot<FakeType> a = pool.acquire(GlintKind.GLINT, scratch, 1);
		scratch.speed = 9.0f;
		assertEquals(2.0f, a.params.speed);
	}

	@Test
	void overflowReturnsNullAfterAllSlotsAreBusy() {
		Set<FakeType> types = new HashSet<>();
		for (int i = 0; i < GlintSlotPool.SLOTS_PER_KIND; i++) {
			GlintSlot<FakeType> s = pool.acquire(GlintKind.GLINT, look(1.5f + i), 7);
			assertNotNull(s, "slot " + i);
			types.add(s.type);
		}
		assertEquals(GlintSlotPool.SLOTS_PER_KIND, types.size());
		assertEquals(0, pool.overflowCount());
		assertNull(pool.acquire(GlintKind.GLINT, look(99.0f), 7));
		assertEquals(1, pool.overflowCount());
		assertNull(pool.acquire(GlintKind.GLINT, look(98.0f), 7));
		assertEquals(2, pool.overflowCount());
		// an existing look is still found when the pool is full
		assertNotNull(pool.acquire(GlintKind.GLINT, look(1.5f + 5), 7));
		// other kinds are unaffected
		assertNotNull(pool.acquire(GlintKind.ENTITY_GLINT, look(99.0f), 7));
	}

	@Test
	void releaseFreesTheSlotForADifferentLook() {
		GlintSlot<FakeType> a = pool.acquire(GlintKind.GLINT, look(2.0f), 1);
		GlintSlot<FakeType> b = pool.acquire(GlintKind.GLINT, look(3.0f), 1);
		assertTrue(pool.release(a.type));
		assertFalse(a.inUse);
		assertTrue(b.inUse);
		GlintSlot<FakeType> c = pool.acquire(GlintKind.GLINT, look(4.0f), 1);
		assertSame(a, c, "the first free slot is reused");
		assertEquals(4.0f, a.params.speed);
	}

	@Test
	void releasedLookDoesNotMatchAnymore() {
		GlintSlot<FakeType> a = pool.acquire(GlintKind.GLINT, look(2.0f), 1);
		pool.release(a.type);
		GlintSlot<FakeType> again = pool.acquire(GlintKind.GLINT, look(2.0f), 1);
		assertSame(a, again, "the first free slot is the same one");
		assertTrue(again.inUse);
	}

	@Test
	void releaseOfAForeignTypeIsIgnored() {
		pool.acquire(GlintKind.GLINT, look(2.0f), 1);
		assertFalse(pool.release(new FakeType("other")));
		assertNull(pool.slotOf(new FakeType("other")));
	}

	@Test
	void guiStyleCyclingNeverRunsOut() {
		// one item at a time: acquire, draw (release), next item
		for (int i = 0; i < 1000; i++) {
			GlintSlot<FakeType> s = pool.acquire(GlintKind.GLINT, look(1.0f + (i % 200)), 5);
			assertNotNull(s);
			assertTrue(pool.release(s.type));
		}
		assertEquals(0, pool.overflowCount());
	}

	@Test
	void staleSlotsAreReusedWithoutAnExplicitRelease() {
		for (int i = 0; i < GlintSlotPool.SLOTS_PER_KIND; i++) {
			assertNotNull(pool.acquire(GlintKind.GLINT, look(1.5f + i), 10));
		}
		assertNull(pool.acquire(GlintKind.GLINT, look(50.0f), 10));
		// next frame: nothing was released, but all slots belong to the previous frame
		GlintSlot<FakeType> s = pool.acquire(GlintKind.GLINT, look(50.0f), 11);
		assertNotNull(s);
		assertEquals(50.0f, s.params.speed);
		assertEquals(11, s.frame);
	}

	@Test
	void reusingAStaleMatchingSlotRefreshesItsFrame() {
		GlintSlot<FakeType> a = pool.acquire(GlintKind.GLINT, look(2.0f), 10);
		GlintSlot<FakeType> b = pool.acquire(GlintKind.GLINT, look(2.0f), 11);
		assertSame(a, b);
		assertEquals(11, a.frame);
	}

	@Test
	void releaseStaleOnlyFreesOlderFrames() {
		GlintSlot<FakeType> old = pool.acquire(GlintKind.GLINT, look(2.0f), 10);
		GlintSlot<FakeType> fresh = pool.acquire(GlintKind.GLINT_TRANSLUCENT, look(2.0f), 11);
		GlintSlot<FakeType> off = pool.acquireOff(GlintKind.ENTITY_GLINT, 10);
		pool.releaseStale(11);
		assertFalse(old.inUse);
		assertTrue(fresh.inUse);
		assertFalse(off.inUse);
	}

	@Test
	void offTypeIsSharedPerKindAndTrackedLikeASlot() {
		GlintSlot<FakeType> off1 = pool.acquireOff(GlintKind.ENTITY_GLINT, 3);
		GlintSlot<FakeType> off2 = pool.acquireOff(GlintKind.ENTITY_GLINT, 3);
		assertSame(off1, off2);
		assertTrue(off1.isOff());
		assertTrue(off1.inUse);
		assertEquals("entity_glint_off", off1.type.name);
		assertNotSame(off1, pool.acquireOff(GlintKind.GLINT, 3));
		assertTrue(pool.release(off1.type));
		assertFalse(off1.inUse);
		assertSame(off1, pool.slotOf(off1.type));
	}

	@Test
	void offTypeIsNeverHandedOutForALook() {
		for (int i = 0; i < GlintSlotPool.SLOTS_PER_KIND; i++) {
			GlintSlot<FakeType> s = pool.acquire(GlintKind.GLINT, look(1.5f + i), 1);
			assertFalse(s.isOff());
		}
	}

	@Test
	void iterationCoversEverySlotAndLookupUsesIdentity() {
		pool.acquire(GlintKind.GLINT, look(2.0f), 1);
		pool.acquireOff(GlintKind.ARMOR_ENTITY_GLINT, 1);
		assertEquals(2 * (GlintSlotPool.SLOTS_PER_KIND + 1), pool.count());
		IdentityHashMap<FakeType, Boolean> seen = new IdentityHashMap<>();
		for (int i = 0; i < pool.count(); i++) {
			GlintSlot<FakeType> s = pool.at(i);
			assertSame(s, pool.slotOf(s.type));
			seen.put(s.type, true);
		}
		assertEquals(pool.count(), seen.size());
	}
}
