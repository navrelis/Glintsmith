package navrelis.glintsmith.mixin;

import java.util.List;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Leaves out the mixins that cannot work next to another installed mod.
 *
 * <p>Runs before any Minecraft class is loaded, so it only compares class names and asks the loader which mods exist.
 */
public final class GlintsmithMixinPlugin implements IMixinConfigPlugin {
	/** Id of the mod that overwrites {@code ItemModelGenerator.createOrExpandSpan}. */
	public static final String MODELFIX_MOD_ID = "modelfix";

	/**
	 * Mixins that inject into a method 'modelfix' overwrites. Mixin refuses to inject into a method another mixin merged,
	 * even with {@code require = 0}, and the overwritten method would never reach our code anyway.
	 */
	public static final Set<String> MODELFIX_COLLISIONS = Set.of("navrelis.glintsmith.mixin.modelfix.ItemModelGeneratorSpanMixin");

	private static final Logger LOGGER = LoggerFactory.getLogger("glintsmith");

	/** Pure decision: whether a mixin class stays applied. */
	public static boolean shouldApply(String mixinClassName, boolean modelfixLoaded) {
		return !(modelfixLoaded && MODELFIX_COLLISIONS.contains(mixinClassName));
	}

	@Override
	public void onLoad(String mixinPackage) {
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		boolean modelfixLoaded = FabricLoader.getInstance().isModLoaded(MODELFIX_MOD_ID);
		boolean apply = shouldApply(mixinClassName, modelfixLoaded);
		if (!apply) {
			LOGGER.info("The mod '{}' is installed: skipping the mixin {}, which collides with it.", MODELFIX_MOD_ID, mixinClassName);
		}
		return apply;
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
	}

	@Override
	public List<String> getMixins() {
		return null;
	}

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}
}
