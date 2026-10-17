package navrelis.glintsmith.render.outline;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.jetbrains.annotations.Nullable;

/**
 * The 256x2 RGBA texture the composite shader reads styles from. Row 0 holds the colour, row 1 thickness and
 * softness (see {@link StyleTable}). Created lazily on the render thread; only the used columns are uploaded.
 */
final class StyleTexture {
	static final int WIDTH = StyleTable.MAX_STYLES + 1;

	@Nullable
	private DynamicTexture texture;

	/**
	 * Uploads the styles of a table and returns the texture to bind.
	 */
	DynamicTexture upload(StyleTable table) {
		DynamicTexture tex = texture;
		if (tex == null) {
			tex = new DynamicTexture(WIDTH, 2, false);
			texture = tex;
		}
		NativeImage image = tex.getPixels();
		if (image == null) {
			throw new IllegalStateException("style texture has no pixels");
		}
		int n = Math.min(table.count(), StyleTable.MAX_STYLES);
		image.setPixelRGBA(0, 0, 0);
		image.setPixelRGBA(0, 1, 0);
		for (int i = 1; i <= n; i++) {
			image.setPixelRGBA(i, 0, table.colorPixel(i));
			image.setPixelRGBA(i, 1, table.paramPixel(i));
		}
		tex.bind();
		image.upload(0, 0, 0, 0, 0, n + 1, 2, false, false, false, false);
		return tex;
	}

	void close() {
		DynamicTexture tex = texture;
		texture = null;
		if (tex != null) {
			tex.close();
		}
	}
}
