package navrelis.glintsmith.gui.preview;

import java.util.Optional;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import org.jetbrains.annotations.Nullable;

/**
 * The stacks the preview can show: what the player holds, or an enchanted netherite sword.
 */
public final class PreviewItems {
	/** The sample sword is built once per registry access, not every frame. */
	@Nullable
	private static RegistryAccess cachedAccess;
	@Nullable
	private static Sample cachedSample;
	@Nullable
	private static Sample cachedBare;

	private PreviewItems() {
	}

	/**
	 * @param stack the sword; never modified by the preview
	 * @param noWorld the enchantments could not be applied (no world, no registries), so the sword only carries the
	 *        glint override flag
	 */
	public record Sample(ItemStack stack, boolean noWorld) {
	}

	/** The item in the main hand, or {@link ItemStack#EMPTY} without a player. */
	public static ItemStack held(Minecraft mc) {
		return mc.player == null ? ItemStack.EMPTY : mc.player.getMainHandItem();
	}

	/** An enchanted netherite sword (Sharpness V, Unbreaking III, Mending), or a glinting plain one without a world. */
	public static Sample sample(Minecraft mc) {
		RegistryAccess access = null;
		if (mc.level != null) {
			access = mc.level.registryAccess();
		} else if (mc.getConnection() != null) {
			access = mc.getConnection().registryAccess();
		}
		if (access == null) {
			return bare();
		}
		if (access != cachedAccess || cachedSample == null) {
			cachedAccess = access;
			cachedSample = build(access);
		}
		return cachedSample;
	}

	private static Sample build(RegistryAccess access) {
		try {
			HolderLookup.RegistryLookup<Enchantment> registry = access.lookupOrThrow(Registries.ENCHANTMENT);
			ItemStack sword = new ItemStack(Items.NETHERITE_SWORD);
			int applied = 0;
			applied += enchant(registry, sword, Enchantments.SHARPNESS, 5);
			applied += enchant(registry, sword, Enchantments.UNBREAKING, 3);
			applied += enchant(registry, sword, Enchantments.MENDING, 1);
			if (applied > 0) {
				return new Sample(sword, false);
			}
		} catch (RuntimeException ignored) {
			// registry missing or not frozen yet: fall through to the plain sword
		}
		return bare();
	}

	private static int enchant(HolderLookup.RegistryLookup<Enchantment> registry, ItemStack sword,
		ResourceKey<Enchantment> key, int level) {
		Optional<Holder.Reference<Enchantment>> holder = registry.get(key);
		if (holder.isEmpty()) {
			return 0;
		}
		sword.enchant(holder.get(), level);
		return 1;
	}

	private static Sample bare() {
		if (cachedBare == null) {
			ItemStack sword = new ItemStack(Items.NETHERITE_SWORD);
			sword.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
			cachedBare = new Sample(sword, true);
		}
		return cachedBare;
	}
}
