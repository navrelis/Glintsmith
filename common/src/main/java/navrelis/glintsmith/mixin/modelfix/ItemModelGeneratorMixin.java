package navrelis.glintsmith.mixin.modelfix;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import java.util.List;
import java.util.Map;
import navrelis.glintsmith.Glintsmith;
import navrelis.glintsmith.modelfix.GapFix;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.ItemModelGenerator;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Closes the cracks next to the front and back faces of generated item models, and keeps their edges from sampling the
 * neighbouring sprite of the block atlas (edge guard, {@link GapFix#EDGE_GUARD_TEXELS}).
 *
 * <p>The precise span handling lives in {@link ItemModelGeneratorSpanMixin}.
 */
@Mixin(ItemModelGenerator.class)
public abstract class ItemModelGeneratorMixin {
	/** Edge guard for the front and back faces: the layer element is the first element of every frame list. */
	@ModifyReturnValue(method = "processFrames", at = @At("RETURN"), require = 0)
	private List<BlockElement> glintsmith$guardLayer(List<BlockElement> elements, @Local(argsOnly = true) SpriteContents contents) {
		try {
			if (!GapFix.effective().enabled() || elements.isEmpty()) {
				return elements;
			}
			BlockElement layer = elements.get(0);
			BlockElementFace south = layer.faces.get(Direction.SOUTH);
			BlockElementFace north = layer.faces.get(Direction.NORTH);
			if (layer.faces.size() != 2 || south == null || north == null) {
				return elements;
			}
			GapFix.guardLayerElement(layer.from, layer.to, texelsToModel(GapFix.EDGE_GUARD_TEXELS, contents.width()),
				texelsToModel(GapFix.EDGE_GUARD_TEXELS, contents.height()), south.uv().uvs, north.uv().uvs);
		} catch (Throwable t) {
			fail(t);
		}
		return elements;
	}

	/**
	 * Edge guard for the side faces (UV inset across the thickness, side faces on the sprite border moved inwards to meet
	 * the guarded front face), then nudges every single-face side element inwards and/or outwards, in place.
	 */
	@ModifyReturnValue(method = "createSideElements", at = @At("RETURN"), require = 0)
	private List<BlockElement> glintsmith$adjustSideElements(List<BlockElement> elements, @Local(argsOnly = true) SpriteContents contents) {
		try {
			GapFix.Settings settings = GapFix.effective();
			if (!settings.enabled()) {
				return elements;
			}
			float guardX = texelsToModel(GapFix.EDGE_GUARD_TEXELS, contents.width());
			float guardY = texelsToModel(GapFix.EDGE_GUARD_TEXELS, contents.height());
			float insetX = texelsToModel(GapFix.SIDE_THICKNESS_INSET_TEXELS, contents.width());
			float insetY = texelsToModel(GapFix.SIDE_THICKNESS_INSET_TEXELS, contents.height());
			for (BlockElement element : elements) {
				Map<Direction, BlockElementFace> faces = element.faces;
				if (faces.size() != 1) {
					continue;
				}
				Direction face = faces.keySet().iterator().next();
				boolean horizontal = face == Direction.UP || face == Direction.DOWN;
				// UP and DOWN faces span the thickness on v (texel rows), EAST and WEST on u (texel columns).
				GapFix.insetSideThickness(face, faces.get(face).uv().uvs, horizontal ? insetY : insetX);
				if (GapFix.isOnSpriteBorder(face, element.from)) {
					GapFix.adjustSideElement(face, element.from, element.to, 0.0F, horizontal ? guardY : guardX);
				}
				GapFix.adjustSideElement(face, element.from, element.to, settings.sideExpansion(), settings.sideIndent());
			}
		} catch (Throwable t) {
			fail(t);
		}
		return elements;
	}

	private static float texelsToModel(float texels, int spriteSize) {
		return texels * 16.0F / spriteSize;
	}

	private static void fail(Throwable t) {
		if (!GapFix.hasFailed()) {
			Glintsmith.LOGGER.error("Model gap fix failed while generating an item model, disabling it for this session.", t);
			GapFix.markFailed();
		}
	}
}
