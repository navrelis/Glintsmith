package navrelis.glintsmith.modelfix;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Loads the mixin targets through the loader's class loader, so a broken target or injection point fails the build.
 */
class MixinApplicationTest {
	@Test
	void spanImplementsSpanAccess() throws Exception {
		Class<?> span = Class.forName("net.minecraft.client.renderer.block.model.ItemModelGenerator$Span");
		assertTrue(SpanAccess.class.isAssignableFrom(span), "SpanMixin was not applied");
	}

	@Test
	void injectionsApplyToItemModelGenerator() throws Exception {
		// Loading the class applies the mixins; a missing injection point (require = 1) throws here.
		Class<?> generator = Class.forName("net.minecraft.client.renderer.block.model.ItemModelGenerator");
		Class.forName("net.minecraft.client.renderer.texture.TextureAtlasSprite");
		boolean hasHandler = false;
		for (var method : generator.getDeclaredMethods()) {
			hasHandler |= method.getName().startsWith("glintsmith$");
		}
		assertTrue(hasHandler, "ItemModelGeneratorMixin was not applied");
	}
}
