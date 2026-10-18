package navrelis.glintsmith.gui.preview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import navrelis.glintsmith.gui.preview.PreviewLayout.Layout;
import navrelis.glintsmith.gui.preview.PreviewLayout.Mode;
import org.junit.jupiter.api.Test;

class PreviewLayoutTest {
	private static final int TOP = 24;

	/** A screen whose list area is {@code areaH} pixels high. */
	private static Layout side(int screenW, int areaH) {
		return PreviewLayout.side(screenW, TOP, TOP + areaH);
	}

	private static void assertSide(Layout layout, int x, int w, int itemH, boolean caption, boolean toggle) {
		assertEquals(Mode.SIDE, layout.mode());
		assertEquals(x, layout.x());
		assertEquals(TOP + 4, layout.y());
		assertEquals(w, layout.w());
		assertEquals(itemH, layout.itemH());
		assertEquals(caption, layout.caption());
		assertEquals(toggle, layout.toggle());
	}

	@Test
	void narrowWindowsHideTheSidePanel() {
		assertEquals(Mode.HIDDEN, side(480, 120).mode());
		assertEquals(Mode.HIDDEN, side(480, 300).mode());
	}

	@Test
	void mediumWindowsShrinkTheWidth() {
		assertSide(side(640, 120), 497, 137, 72, true, true);
		assertSide(side(640, 300), 497, 137, 137, true, true);
	}

	@Test
	void wideWindowsUseTheMaximumWidth() {
		assertSide(side(960, 120), 657, 180, 72, true, true);
		assertSide(side(960, 300), 657, 180, 180, true, true);
		assertSide(side(1920, 120), 1137, 180, 72, true, true);
		assertSide(side(1920, 300), 1137, 180, 180, true, true);
	}

	@Test
	void widthThresholdIsSeventyTwo() {
		assertSide(side(510, 300), 432, 72, 72, true, true);
		assertEquals(Mode.HIDDEN, side(508, 300).mode());
		assertSide(side(509, 300), 431, 72, 72, true, true);
	}

	@Test
	void lowAreasDropTheToggleThenTheCaption() {
		// avail = areaH - 8: with both the item gets avail - 40, without the toggle avail - 22
		assertSide(side(960, 70), 657, 180, 40, true, false);
		assertSide(side(960, 60), 657, 180, 52, false, false);
		assertSide(side(960, 48), 657, 180, 40, false, false);
		assertEquals(Mode.HIDDEN, side(960, 47).mode());
		assertEquals(Mode.HIDDEN, side(960, 0).mode());
		assertEquals(Mode.HIDDEN, side(960, -20).mode());
	}

	@Test
	void theItemBoxNeverExceedsTheWidth() {
		for (int screenW : new int[] {520, 640, 800, 960, 1920}) {
			Layout layout = side(screenW, 400);
			assertTrue(layout.itemH() <= layout.w());
			assertTrue(layout.x() + layout.w() <= screenW - PreviewLayout.RIGHT_MARGIN);
		}
	}

	@Test
	void totalHeightCoversTheVisibleParts() {
		Layout both = side(960, 120);
		assertEquals(both.itemH() + 2 + PreviewLayout.CAPTION_H + 2 + PreviewLayout.TOGGLE_H, both.totalH());
		assertEquals(both.y() + both.itemH() + 2, both.captionY());
		assertEquals(both.captionY() + PreviewLayout.CAPTION_H + 2, both.toggleY());
		assertTrue(both.y() + both.totalH() <= TOP + 120, "inside the list area");
		Layout none = side(960, 60);
		assertEquals(none.itemH(), none.totalH());
	}

	@Test
	void footerSitsLeftOfTheButtons() {
		// 3 buttons of 104 and two gaps of 4 = 320 wide, centred in 480: 80 px free on the left
		Layout layout = PreviewLayout.footer(480, 300, 56, 320);
		assertEquals(Mode.FOOTER, layout.mode());
		assertEquals(4, layout.x());
		assertEquals(300 - 56 + 4, layout.y());
		assertEquals(48, layout.w(), "limited by the footer height");
		assertEquals(48, layout.itemH());
		assertFalse(layout.caption());
		assertFalse(layout.toggle());
		assertEquals(48, layout.totalH());

		Layout wide = PreviewLayout.footer(1000, 300, 56, 320);
		assertEquals(48, wide.w());
		Layout tight = PreviewLayout.footer(420, 300, 56, 320);
		assertEquals(Mode.FOOTER, tight.mode());
		assertEquals(42, tight.w(), "limited by the space beside the buttons");
	}

	@Test
	void footerThresholdIsThirtyTwo() {
		assertEquals(32, PreviewLayout.footer(400, 300, 56, 320).w());
		assertEquals(Mode.HIDDEN, PreviewLayout.footer(398, 300, 56, 320).mode());
		assertEquals(Mode.HIDDEN, PreviewLayout.footer(320, 300, 56, 320).mode());
		assertEquals(Mode.HIDDEN, PreviewLayout.footer(1000, 300, 39, 320).mode(), "a short footer");
		assertEquals(32, PreviewLayout.footer(1000, 300, 40, 320).w());
	}

	@Test
	void hiddenLayoutIsEmpty() {
		Layout hidden = Layout.HIDDEN;
		assertEquals(Mode.HIDDEN, hidden.mode());
		assertEquals(0, hidden.totalH());
		assertFalse(hidden.caption());
		assertFalse(hidden.toggle());
	}
}
