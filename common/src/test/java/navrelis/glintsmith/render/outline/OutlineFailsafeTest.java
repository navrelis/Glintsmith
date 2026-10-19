package navrelis.glintsmith.render.outline;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

class OutlineFailsafeTest {
	@AfterEach
	void rearm() {
		OutlineFailsafe.setOnTrip(null);
		OutlineFailsafe.resetForTests();
	}

	@Test
	void tripsOnceAndStaysTripped() {
		OutlineFailsafe.resetForTests();
		AtomicInteger hookRuns = new AtomicInteger();
		OutlineFailsafe.setOnTrip(hookRuns::incrementAndGet);
		assertFalse(OutlineFailsafe.tripped());

		OutlineFailsafe.trip("test entry point", new IllegalStateException("first"));
		assertTrue(OutlineFailsafe.tripped());
		assertEquals(1, OutlineFailsafe.tripCount(), "the first trip logs one line");
		assertEquals(1, hookRuns.get());

		OutlineFailsafe.trip("test entry point", new IllegalStateException("second"));
		OutlineFailsafe.trip("another entry point", new RuntimeException("third"));
		assertTrue(OutlineFailsafe.tripped());
		assertEquals(1, OutlineFailsafe.tripCount(), "later trips log nothing");
		assertEquals(1, hookRuns.get(), "the cleanup runs once");
	}

	@Test
	void logsExactlyOneErrorLine() {
		OutlineFailsafe.resetForTests();
		Object raw = LogManager.getLogger("Glintsmith/Outline");
		Assumptions.assumeTrue(raw instanceof org.apache.logging.log4j.core.Logger, "log4j-core is not the logging backend");
		org.apache.logging.log4j.core.Logger logger = (org.apache.logging.log4j.core.Logger) raw;
		List<String> lines = new CopyOnWriteArrayList<>();
		AbstractAppender appender = new AbstractAppender("glintsmith-failsafe-test", null, null, true, Property.EMPTY_ARRAY) {
			@Override
			public void append(LogEvent event) {
				if (event.getLevel() == Level.ERROR) {
					lines.add(event.getMessage().getFormattedMessage());
				}
			}
		};
		appender.start();
		logger.addAppender(appender);
		try {
			OutlineFailsafe.trip("the compositor", new IllegalStateException("boom"));
			OutlineFailsafe.trip("the compositor", new IllegalStateException("boom again"));
		} finally {
			logger.removeAppender(appender);
			appender.stop();
		}
		assertEquals(List.of("[Glintsmith] Outline rendering disabled for this session after an error in the compositor"), lines);
	}

	@Test
	void aFailingHookOrNullCauseNeverThrows() {
		OutlineFailsafe.resetForTests();
		OutlineFailsafe.setOnTrip(() -> {
			throw new IllegalStateException("cleanup failed");
		});
		assertDoesNotThrow(() -> OutlineFailsafe.trip("test", null));
		assertTrue(OutlineFailsafe.tripped());
	}
}
