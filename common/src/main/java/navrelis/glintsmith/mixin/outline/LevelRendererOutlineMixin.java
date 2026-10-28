package navrelis.glintsmith.mixin.outline;

import navrelis.glintsmith.render.outline.OutlineSystem;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * O8: the point in {@code LevelRenderer.renderLevel} right before the translucent terrain (the {@code "translucent"}
 * profiler section, one in the Fabulous and one in the other branch), where under a shader pack the world phase is
 * closed and its mask drawn ({@link OutlineSystem#onBeforeTranslucent()}).
 * <p>
 * Iris 1.8.14 injects at the same constant: {@code batchedentityrendering.mixin.MixinLevelRenderer} (priority 999)
 * flushes the batched entities there and {@code iris.mixin.MixinLevelRenderer} (priority 1000) copies
 * {@code depthtex2}, renders the solid first-person hand through {@code renderHandsWithItems} and copies
 * {@code depthtex1}. Mixins are applied in ascending priority and callbacks at one point run in the order they were
 * injected, so priority 900 puts this hook before the hand: the hand's items are never captured as world items. If
 * the order were different, {@code OutlineSystem.beginHands} closes the phase the same way. Without a shader pack the
 * handler returns at once.
 */
@Mixin(value = LevelRenderer.class, priority = 900)
public abstract class LevelRendererOutlineMixin {
	@Inject(method = "renderLevel", at = @At(value = "CONSTANT", args = "stringValue=translucent"), require = 0)
	private void glintsmith$outlineBeforeTranslucent(CallbackInfo ci) {
		OutlineSystem.onBeforeTranslucent();
	}
}
