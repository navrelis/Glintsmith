package navrelis.glintsmith.render.glint;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GlintFailsafeTest {
	@BeforeEach
	@AfterEach
	void reset() {
		GlintFailsafe.resetForTests();
	}

	@Test
	void startsUntripped() {
		assertFalse(GlintFailsafe.tripped());
	}

	@Test
	void tripSwitchesGlintCustomisationOffForTheSession() {
		GlintFailsafe.trip("test", new IllegalStateException("boom"));
		assertTrue(GlintFailsafe.tripped());
	}

	@Test
	void repeatedAndOddTripsNeverThrow() {
		assertDoesNotThrow(() -> {
			GlintFailsafe.trip("first", new RuntimeException("one"));
			GlintFailsafe.trip("second", new RuntimeException("two"));
			GlintFailsafe.trip("no throwable", null);
		});
		assertTrue(GlintFailsafe.tripped());
	}
}
