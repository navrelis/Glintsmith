package navrelis.glintsmith.smoke.neoforge.mixin;

import com.mojang.blaze3d.platform.Window;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The game window of the smoke run is created without the keyboard focus and does not take it when shown, so a run
 * never steals the focus from the user. NeoForge replaces the glfwCreateWindow call of the Window constructor by
 * {@code ImmediateWindowHandler.setupMinecraftWindow}; without the early display window (the run task writes
 * {@code earlyWindowControl = false} to fml.toml) that creates the window itself, with the hints set right before.
 */
@Mixin(Window.class)
abstract class SmokeWindowMixin {
	@Inject(method = "<init>", at = @At(value = "INVOKE",
		target = "Lnet/neoforged/fml/loading/ImmediateWindowHandler;setupMinecraftWindow(Ljava/util/function/IntSupplier;Ljava/util/function/IntSupplier;Ljava/util/function/Supplier;Ljava/util/function/LongSupplier;)J",
		remap = false))
	private void glintsmithSmoke$createWithoutFocus(CallbackInfo ci) {
		GLFW.glfwWindowHint(GLFW.GLFW_FOCUSED, GLFW.GLFW_FALSE);
		GLFW.glfwWindowHint(GLFW.GLFW_FOCUS_ON_SHOW, GLFW.GLFW_FALSE);
	}
}
