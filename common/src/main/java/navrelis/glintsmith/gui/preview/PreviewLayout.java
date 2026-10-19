package navrelis.glintsmith.gui.preview;

/**
 * Where the preview panel goes. The panel sits to the right of the 310 px option column; when that does not fit
 * the main screen moves it into the footer, other screens hide it. Pure Java, all values in GUI pixels.
 */
public final class PreviewLayout {
	/** Distance from the screen centre to the left edge of the panel (the option column ends 155 px from it). */
	public static final int GAP_FROM_CENTER = 177;
	public static final int RIGHT_MARGIN = 6;
	public static final int MIN_W = 72;
	public static final int MAX_W = 180;
	public static final int CAPTION_H = 22;
	public static final int TOGGLE_H = 14;
	public static final int MIN_ITEM = 40;
	/** Smallest item box that is still worth drawing in the footer. */
	public static final int MIN_FOOTER = 32;
	/** Space between the item box, the caption and the toggle strip. */
	private static final int PAD = 2;

	private PreviewLayout() {
	}

	public enum Mode {
		SIDE,
		FOOTER,
		HIDDEN
	}

	/**
	 * @param x left edge of the item box
	 * @param y top edge of the item box
	 * @param w width of the panel
	 * @param itemH height of the item box (the frame around the 3D preview)
	 * @param caption whether the two caption lines fit below the item box
	 * @param toggle whether the source toggle strip fits below the caption
	 */
	public record Layout(Mode mode, int x, int y, int w, int itemH, boolean caption, boolean toggle) {
		public static final Layout HIDDEN = new Layout(Mode.HIDDEN, 0, 0, 0, 0, false, false);

		/** Height of the whole panel: item box, caption and toggle strip. */
		public int totalH() {
			int h = itemH;
			if (caption) {
				h += PAD + CAPTION_H;
			}
			if (toggle) {
				h += PAD + TOGGLE_H;
			}
			return h;
		}

		/** Top of the caption lines. */
		public int captionY() {
			return y + itemH + PAD;
		}

		/** Top of the toggle strip. */
		public int toggleY() {
			return captionY() + CAPTION_H + PAD;
		}
	}

	/**
	 * The panel right of the option column.
	 *
	 * @param areaTop first pixel row below the header
	 * @param areaBottom first pixel row of the footer
	 */
	public static Layout side(int screenW, int areaTop, int areaBottom) {
		int x = screenW / 2 + GAP_FROM_CENTER;
		int w = Math.min(MAX_W, screenW - RIGHT_MARGIN - x);
		if (w < MIN_W) {
			return Layout.HIDDEN;
		}
		int avail = areaBottom - areaTop - 8;
		boolean caption = true;
		boolean toggle = true;
		int itemH = Math.min(w, avail - CAPTION_H - TOGGLE_H - 4);
		if (itemH < MIN_ITEM) {
			toggle = false;
			itemH = Math.min(w, avail - CAPTION_H);
		}
		if (itemH < MIN_ITEM) {
			caption = false;
			itemH = Math.min(w, avail);
		}
		if (itemH < MIN_ITEM) {
			return Layout.HIDDEN;
		}
		return new Layout(Mode.SIDE, x, areaTop + 4, w, itemH, caption, toggle);
	}

	/**
	 * The fallback of the main screen: a square at the bottom left, beside the centred footer buttons.
	 *
	 * @param footerH height of the footer
	 * @param widestRowW width of the widest footer button row
	 */
	public static Layout footer(int screenW, int screenH, int footerH, int widestRowW) {
		int left = (screenW - widestRowW) / 2;
		int size = Math.min(left - 8, footerH - 8);
		if (size < MIN_FOOTER) {
			return Layout.HIDDEN;
		}
		return new Layout(Mode.FOOTER, 4, screenH - footerH + 4, size, size, false, false);
	}
}
