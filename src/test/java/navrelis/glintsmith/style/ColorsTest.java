package navrelis.glintsmith.style;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ColorsTest {
	@Test
	void parsesAllHexForms() {
		assertEquals(0xFFAABBCC, Colors.parseHex("#ABC"));
		assertEquals(0xFF123456, Colors.parseHex("#123456"));
		assertEquals(0x80123456, Colors.parseHex("#80123456"));
		assertEquals(0xFF123456, Colors.parseHex("123456"));
		assertEquals(0xFFABCDEF, Colors.parseHex("#abcdef"));
		assertEquals(0xFFABCDEF, Colors.parseHex("  #AbCdEf "));
	}

	@Test
	void rejectsBadInput() {
		for (String bad : new String[] {null, "", "#", "#12", "#1234", "#12345", "#1234567", "#123456789", "#GGGGGG", "#12 456", "#-12345", "#+12345"}) {
			assertThrows(IllegalArgumentException.class, () -> Colors.parseHex(bad), String.valueOf(bad));
		}
	}

	@Test
	void toHexRoundTrips() {
		assertEquals("#D088FC", Colors.toHex(0xFFD088FC));
		assertEquals("#80D088FC", Colors.toHex(0x80D088FC));
		assertEquals("#000000", Colors.toHex(0xFF000000));
		for (int argb : new int[] {0xFF000000, 0xFFFFFFFF, 0x12345678, 0xFFD088FC, 0x00FFFFFF}) {
			assertEquals(argb, Colors.parseHex(Colors.toHex(argb)));
		}
	}

	@Test
	void hsvPrimaries() {
		assertEquals(0xFFFF0000, Colors.hsv(0.0f, 1.0f, 1.0f));
		assertEquals(0xFF00FF00, Colors.hsv(1.0f / 3.0f, 1.0f, 1.0f));
		assertEquals(0xFF0000FF, Colors.hsv(2.0f / 3.0f, 1.0f, 1.0f));
		assertEquals(0xFFFFFFFF, Colors.hsv(0.4f, 0.0f, 1.0f));
		assertEquals(0xFF000000, Colors.hsv(0.4f, 1.0f, 0.0f));
		assertEquals(Colors.hsv(0.25f, 1, 1), Colors.hsv(1.25f, 1, 1));
		assertEquals(Colors.hsv(0.75f, 1, 1), Colors.hsv(-0.25f, 1, 1));
	}

	@Test
	void oklabEndpointsAreExact() {
		int a = 0xFF336699;
		int b = 0xFFFF8800;
		assertEquals(a, Colors.lerpOklab(a, b, 0.0f));
		assertEquals(b, Colors.lerpOklab(a, b, 1.0f));
		assertEquals(a, Colors.lerpOklab(a, b, -3.0f));
		assertEquals(b, Colors.lerpOklab(a, b, 3.0f));
	}

	@Test
	void oklabNearEndpointsIsCloseToEndpoints() {
		int a = 0xFF336699;
		int b = 0xFFFF8800;
		assertClose(a, Colors.lerpOklab(a, b, 0.0001f), 1);
		assertClose(b, Colors.lerpOklab(a, b, 0.9999f), 1);
		assertClose(a, Colors.lerpOklab(a, a, 0.5f), 1);
	}

	@Test
	void oklabMidpointOfBlackAndWhiteIsPerceptualGrey() {
		int mid = Colors.lerpOklab(0xFF000000, 0xFFFFFFFF, 0.5f);
		int r = mid >> 16 & 0xFF;
		assertEquals(r, mid >> 8 & 0xFF);
		assertEquals(r, mid & 0xFF);
		// Plain sRGB lerp would give 128; OkLab L = 0.5 is about 99.
		assertTrue(r > 90 && r < 110, "mid grey was " + r);
	}

	@Test
	void oklabInterpolatesAlphaLinearly() {
		int mid = Colors.lerpOklab(0x00FF0000, 0xFFFF0000, 0.5f);
		assertEquals(128, mid >>> 24);
	}

	@Test
	void oklabMidpointStaysInsideTheHueRange() {
		// Red to green passes through a bright yellow-orange in OkLab, not a dark mud.
		int mid = Colors.lerpOklab(0xFFFF0000, 0xFF00FF00, 0.5f);
		assertTrue((mid >> 16 & 0xFF) > 150);
		assertTrue((mid >> 8 & 0xFF) > 150);
	}

	@Test
	void alphaHelpers() {
		assertEquals(0x80123456, Colors.withAlpha(0xFF123456, 0.5f));
		assertEquals(0x00123456, Colors.withAlpha(0xFF123456, -1.0f));
		assertEquals(0xFF123456, Colors.withAlpha(0x00123456, 2.0f));
		assertEquals(0x80123456, Colors.multiplyAlpha(0xFF123456, 0.5f));
		assertEquals(0xFF123456, Colors.multiplyAlpha(0xFF123456, 1.0f));
		assertEquals(0x40123456, Colors.multiplyAlpha(0x80123456, 0.5f));
	}

	private static void assertClose(int expected, int actual, int tolerance) {
		for (int shift = 0; shift <= 24; shift += 8) {
			int e = expected >>> shift & 0xFF;
			int a = actual >>> shift & 0xFF;
			assertTrue(Math.abs(e - a) <= tolerance, "channel at shift " + shift + ": expected " + e + " got " + a);
		}
	}
}
