package navrelis.glintsmith.modelfix;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Loads the mixin targets through the loader's class loader, so a broken target or injection point fails the build.
 * {@code defaultRequire} is 0, so a missed injection point is silent; the tests look for the handlers MixinExtras leaves
 * in the target class. They assume the mod 'modelfix' is not on the test classpath.
 */
class MixinApplicationTest {
	private static final String GENERATOR = "net.minecraft.client.renderer.block.model.ItemModelGenerator";

	@Test
	void spanImplementsSpanAccess() throws Exception {
		Class<?> span = Class.forName(GENERATOR + "$Span");
		assertTrue(SpanAccess.class.isAssignableFrom(span), "SpanMixin was not applied");
	}

	@Test
	void sideElementsHandlerIsApplied() throws Exception {
		assertTrue(hasMethod(Class.forName(GENERATOR), "modifyReturnValue$", "$glintsmith$adjustSideElements"),
			"ItemModelGeneratorMixin was not applied");
	}

	@Test
	void layerGuardHandlerIsApplied() throws Exception {
		assertTrue(hasMethod(Class.forName(GENERATOR), "modifyReturnValue$", "$glintsmith$guardLayer"),
			"ItemModelGeneratorMixin (edge guard) was not applied");
	}

	@Test
	void spanHandlerIsApplied() throws Exception {
		assertTrue(hasMethod(Class.forName(GENERATOR), "wrapOperation$", "$glintsmith$matchContiguousSpan"),
			"ItemModelGeneratorSpanMixin was not applied");
	}

	@Test
	void uvShrinkHandlerIsApplied() throws Exception {
		Class<?> sprite = Class.forName("net.minecraft.client.renderer.texture.TextureAtlasSprite");
		assertTrue(hasMethod(sprite, "modifyReturnValue$", "$glintsmith$scaleShrink"), "TextureAtlasSpriteMixin was not applied");
	}

	private static boolean hasMethod(Class<?> target, String prefix, String suffix) {
		for (var method : target.getDeclaredMethods()) {
			String name = method.getName();
			if (name.startsWith(prefix) && name.contains(suffix)) {
				return true;
			}
		}
		return false;
	}
}
