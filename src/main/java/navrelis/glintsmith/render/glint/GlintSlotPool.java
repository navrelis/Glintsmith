package navrelis.glintsmith.render.glint;

import java.util.Arrays;
import java.util.IdentityHashMap;

import navrelis.glintsmith.Glintsmith;

/**
 * Fixed pools of glint render types: {@value #SLOTS_PER_KIND} look slots and one off type for each {@link GlintKind},
 * created lazily per kind. Render thread only. Steady state allocates nothing.
 * <p>
 * The pool does not touch OpenGL or Minecraft: the type objects come from a {@link Factory}, so the pooling rules can
 * be tested with plain objects.
 *
 * @param <T> the render type class
 */
final class GlintSlotPool<T> {
	static final int SLOTS_PER_KIND = 32;

	/** Creates the types. */
	interface Factory<T> {
		/** Builds the render type of a look slot. */
		T createSlotType(GlintSlot<T> slot);

		/** Builds the invisible render type of a kind. */
		T createOffType(GlintSlot<T> slot);
	}

	private final Factory<T> factory;
	@SuppressWarnings("unchecked")
	private final GlintSlot<T>[][] slots = new GlintSlot[GlintKind.COUNT][];
	@SuppressWarnings("unchecked")
	private final GlintSlot<T>[] offs = new GlintSlot[GlintKind.COUNT];
	private final IdentityHashMap<T, GlintSlot<T>> byType = new IdentityHashMap<>();
	@SuppressWarnings("unchecked")
	private GlintSlot<T>[] all = new GlintSlot[16];
	private int allCount;
	private int overflowCount;
	private boolean overflowLogged;

	GlintSlotPool(Factory<T> factory) {
		this.factory = factory;
	}

	/**
	 * Finds or assigns a slot for {@code params}.
	 * <ol>
	 * <li>A slot that is in use with the same params is shared.</li>
	 * <li>Otherwise the first slot that is free (or stale: acquired in an earlier frame) takes a copy of the params.</li>
	 * <li>If all {@value #SLOTS_PER_KIND} are busy this frame, null is returned and the caller keeps the vanilla type.</li>
	 * </ol>
	 */
	GlintSlot<T> acquire(GlintKind kind, GlintParams params, long frame) {
		GlintSlot<T>[] list = slots(kind);
		GlintSlot<T> free = null;
		for (int i = 0; i < list.length; i++) {
			GlintSlot<T> s = list[i];
			if (s.inUse && s.params.sameAs(params)) {
				s.frame = frame;
				return s;
			}
			if (free == null && (!s.inUse || s.frame != frame)) {
				free = s;
			}
		}
		if (free == null) {
			overflowCount++;
			if (!overflowLogged) {
				overflowLogged = true;
				Glintsmith.LOGGER.debug("All {} glint slots of {} are in use this frame; the vanilla glint is drawn for the rest.",
					SLOTS_PER_KIND, kind.id);
			}
			return null;
		}
		free.params.copyFrom(params);
		free.inUse = true;
		free.frame = frame;
		return free;
	}

	/**
	 * The invisible type of a kind, marked in use for this frame.
	 */
	GlintSlot<T> acquireOff(GlintKind kind, long frame) {
		slots(kind);
		GlintSlot<T> off = offs[kind.ordinal()];
		off.inUse = true;
		off.frame = frame;
		return off;
	}

	/**
	 * Frees the slot of a render type whose batch was drawn.
	 *
	 * @return true if the type belongs to this pool
	 */
	boolean release(T type) {
		GlintSlot<T> s = byType.get(type);
		if (s == null) {
			return false;
		}
		s.inUse = false;
		return true;
	}

	/**
	 * Frees every slot that was acquired in an earlier frame than {@code currentFrame}.
	 */
	void releaseStale(long currentFrame) {
		for (int i = 0; i < allCount; i++) {
			GlintSlot<T> s = all[i];
			if (s.inUse && s.frame != currentFrame) {
				s.inUse = false;
			}
		}
	}

	/**
	 * @return the number of slots (including off types) created so far; use with {@link #at(int)} to iterate without
	 *         allocating
	 */
	int count() {
		return allCount;
	}

	GlintSlot<T> at(int index) {
		return all[index];
	}

	/**
	 * @return how often {@link #acquire} found every slot busy
	 */
	int overflowCount() {
		return overflowCount;
	}

	/**
	 * @return the slot owning a type, or null
	 */
	GlintSlot<T> slotOf(T type) {
		return byType.get(type);
	}

	/**
	 * @return true once the pool of this kind has been created
	 */
	boolean isCreated(GlintKind kind) {
		return slots[kind.ordinal()] != null;
	}

	private GlintSlot<T>[] slots(GlintKind kind) {
		GlintSlot<T>[] list = slots[kind.ordinal()];
		if (list == null) {
			list = create(kind);
		}
		return list;
	}

	@SuppressWarnings("unchecked")
	private GlintSlot<T>[] create(GlintKind kind) {
		GlintSlot<T>[] list = new GlintSlot[SLOTS_PER_KIND];
		for (int i = 0; i < SLOTS_PER_KIND; i++) {
			GlintSlot<T> slot = new GlintSlot<>(kind, i);
			slot.type = factory.createSlotType(slot);
			track(slot);
			list[i] = slot;
		}
		GlintSlot<T> off = new GlintSlot<>(kind, -1);
		off.type = factory.createOffType(off);
		track(off);
		offs[kind.ordinal()] = off;
		slots[kind.ordinal()] = list;
		return list;
	}

	private void track(GlintSlot<T> slot) {
		byType.put(slot.type, slot);
		if (allCount == all.length) {
			all = Arrays.copyOf(all, allCount * 2);
		}
		all[allCount++] = slot;
	}
}
