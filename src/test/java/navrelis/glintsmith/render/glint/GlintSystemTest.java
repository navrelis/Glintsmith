package navrelis.glintsmith.render.glint;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import net.minecraft.client.renderer.MultiBufferSource;

/**
 * The parts of the facade that need no OpenGL: the entry points are fast exits while the settings are vanilla or the
 * main source has not been captured, and they never throw (a failure would trip the failsafe instead).
 */
class GlintSystemTest {
	@BeforeEach
	@AfterEach
	void reset() {
		GlintFailsafe.resetForTests();
		GlintContext.reset();
	}

	@Test
	void substituteLeavesTheRequestedTypeAloneWhileEverythingIsVanilla() {
		// With the default settings (anyCustom is false) the source and the type are not even looked at.
		assertNull(GlintSystem.substitute(null, null));
		assertFalse(GlintFailsafe.tripped());
	}

	@Test
	void foreignSourcesAreIgnoredBeforeTheMainSourceExists() {
		try (ByteBufferBuilder bytes = new ByteBufferBuilder(256)) {
			MultiBufferSource.BufferSource other = MultiBufferSource.immediate(bytes);
			assertDoesNotThrow(() -> GlintSystem.afterEndBatch(other, null));
			assertNull(GlintSystem.substitute(other, null));
		}
		assertFalse(GlintFailsafe.tripped());
	}

	@Test
	void bufferedSubstituteLeavesTheRequestedTypeAloneWhileEverythingIsVanilla() {
		assertNull(GlintSystem.substituteBuffered(null));
		assertFalse(GlintFailsafe.tripped());
	}

	@Test
	void bufferedSwapNeedsCustomLooksAStartedClientNoShaderPackAndNoTrip() {
		assertTrue(GlintSystem.bufferedSwapAllowed(true, true, false, false));
		assertFalse(GlintSystem.bufferedSwapAllowed(false, true, false, false), "all vanilla");
		assertFalse(GlintSystem.bufferedSwapAllowed(true, false, false, false), "main source not captured yet");
		assertFalse(GlintSystem.bufferedSwapAllowed(true, true, true, false), "shader pack: world glint stays vanilla");
		assertFalse(GlintSystem.bufferedSwapAllowed(true, true, false, true), "failsafe tripped");
	}

	@Test
	void unknownBatchesAreIgnored() {
		assertDoesNotThrow(() -> GlintSystem.onBatchDrawn(null));
		assertFalse(GlintFailsafe.tripped());
	}

	@Test
	void contextBalanceSurvivesTheEntryPoints() {
		GlintContext.pushItem(null, null);
		GlintSystem.substitute(null, null);
		GlintContext.pop();
		assertEquals(0, GlintContext.depth());
	}
}
