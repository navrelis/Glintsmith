package navrelis.glintsmith;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SmokeTest {
	@Test
	void modIdIsStable() {
		assertEquals("glintsmith", Glintsmith.MOD_ID);
	}
}
