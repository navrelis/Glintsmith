package navrelis.glintsmith.render.outline;

import java.util.Arrays;

import net.minecraft.client.renderer.MultiBufferSource;

/**
 * Stack-like pool of {@link TeeBufferSource}s. Item renders nest only rarely (a renderer that draws another item
 * into a different buffer source), so the pool stays tiny and allocation-free after warm-up.
 */
final class TeePool {
	private TeeBufferSource[] tees = new TeeBufferSource[4];
	private int used;

	TeeBufferSource acquire(MultiBufferSource delegate, PhaseState phase, int index) {
		if (used == tees.length) {
			tees = Arrays.copyOf(tees, tees.length * 2);
		}
		TeeBufferSource tee = tees[used];
		if (tee == null) {
			tee = new TeeBufferSource();
			tees[used] = tee;
		}
		used++;
		tee.bind(delegate, phase, index);
		return tee;
	}

	/**
	 * Releases a tee returned by {@link #acquire}. Usually the most recent one; otherwise it is found and moved out
	 * of the used range.
	 */
	void release(TeeBufferSource tee) {
		for (int i = used - 1; i >= 0; i--) {
			if (tees[i] == tee) {
				tee.release();
				used--;
				if (i != used) {
					tees[i] = tees[used];
					tees[used] = tee;
				}
				return;
			}
		}
	}

	/**
	 * Releases every tee (frame end, failsafe).
	 */
	void releaseAll() {
		for (int i = 0; i < used; i++) {
			tees[i].release();
		}
		used = 0;
	}

	int inUse() {
		return used;
	}
}
