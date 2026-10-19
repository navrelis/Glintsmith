package navrelis.glintsmith.smoke.forge.mixin;

import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import com.mojang.blaze3d.platform.Window;
import net.minecraftforge.fml.loading.ImmediateWindowHandler;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * The game window of the smoke run is created without the keyboard focus and does not take it when shown, so a run
 * never steals the focus from the user. Forge replaces the glfwCreateWindow call of the Window constructor by
 * {@code ImmediateWindowHandler.setupMinecraftWindow}; without the early display window (the run task writes
 * {@code earlyWindowControl = false} to fml.toml) that creates the window itself, with the hints set right before.
 * A redirect, because Forge's Mixin refuses an {@code @Inject} into the constructor here.
 */
@Mixin(Window.class)
abstract class SmokeWindowMixin {
	@Redirect(method = "<init>", at = @At(value = "INVOKE",
		target = "Lnet/minecraftforge/fml/loading/ImmediateWindowHandler;setupMinecraftWindow(Ljava/util/function/IntSupplier;Ljava/util/function/IntSupplier;Ljava/util/function/Supplier;Ljava/util/function/LongSupplier;)J",
		remap = false))
	private long glintsmithSmoke$createWithoutFocus(IntSupplier width, IntSupplier height, Supplier<String> title, LongSupplier monitor) {
		GLFW.glfwWindowHint(GLFW.GLFW_FOCUSED, GLFW.GLFW_FALSE);
		GLFW.glfwWindowHint(GLFW.GLFW_FOCUS_ON_SHOW, GLFW.GLFW_FALSE);
		return ImmediateWindowHandler.setupMinecraftWindow(width, height, title, monitor);
	}
}
