package navrelis.glintsmith.mixin.glint;

import navrelis.glintsmith.render.glint.GlintSystem;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlas;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * G10: the size of the block atlas after it has been stitched and uploaded (every resource reload), for the atlas
 * factor of the glint scale. {@code upload} is where vanilla takes the new size from the stitcher's preparations.
 */
@Mixin(TextureAtlas.class)
public abstract class TextureAtlasGlintMixin {
	@Inject(method = "upload(Lnet/minecraft/client/renderer/texture/SpriteLoader$Preparations;)V", at = @At("TAIL"), require = 0)
	private void glintsmith$blockAtlasSize(SpriteLoader.Preparations preparations, CallbackInfo ci) {
		GlintSystem.onAtlasUploaded(((TextureAtlas) (Object) this).location(), preparations);
	}
}
