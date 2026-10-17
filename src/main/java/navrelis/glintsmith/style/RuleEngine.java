package navrelis.glintsmith.style;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import navrelis.glintsmith.config.ConfigManager;
import navrelis.glintsmith.config.GlintsmithConfig;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * Minecraft adapter of the {@link RuleMatcher}: builds {@link ItemFacts} from an {@link ItemStack} and caches results.
 * <p>
 * <b>Render thread only.</b> The caches are plain {@link HashMap}s without synchronisation. They are cleared when the
 * config changes (a listener is registered when this class loads) and must be cleared by {@link #invalidate()} when
 * tags reload (hooked up by the resource reload code).
 */
public final class RuleEngine {
	private static final int MAX_ENTRIES = 4096;
	private static final int[] NO_COLORS = new int[0];

	private static final Map<StyleKey, ResolvedStyle> STYLES = new HashMap<>();
	private static final Map<EnchantKey, int[]> ENCHANTMENT_COLORS = new HashMap<>();

	// One-entry fast path: the same stack is usually asked several times per frame.
	private static Item lastItem;
	private static ItemEnchantments lastEnchantments;
	private static ItemEnchantments lastStored;
	private static boolean lastFoil;
	private static OutlineTarget lastTarget;
	private static GlintCategory lastCategory;
	private static ResolvedStyle lastResult = ResolvedStyle.NONE;

	private static ItemEnchantments lastColorEnchantments;
	private static ItemEnchantments lastColorStored;
	private static int[] lastColors = NO_COLORS;

	static {
		ConfigManager.instance().addListener(config -> invalidate());
	}

	private RuleEngine() {
	}

	/**
	 * Drops every cached result. Call when the config, tags or enchantments change.
	 */
	public static void invalidate() {
		STYLES.clear();
		ENCHANTMENT_COLORS.clear();
		lastItem = null;
		lastEnchantments = null;
		lastStored = null;
		lastResult = ResolvedStyle.NONE;
		lastColorEnchantments = null;
		lastColorStored = null;
		lastColors = NO_COLORS;
	}

	/**
	 * Resolves what to draw for a stack. Empty stacks give {@link ResolvedStyle#NONE}.
	 */
	public static ResolvedStyle resolve(ItemStack stack, OutlineTarget target, GlintCategory category) {
		if (stack == null || stack.isEmpty()) {
			return ResolvedStyle.NONE;
		}
		Item item = stack.getItem();
		ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
		ItemEnchantments stored = stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
		boolean foil = stack.hasFoil();

		if (lastItem == item && lastEnchantments == enchantments && lastStored == stored && lastFoil == foil
			&& lastTarget == target && lastCategory == category) {
			return lastResult;
		}

		StyleKey key = new StyleKey(item, enchantments, stored, foil, target, category);
		ResolvedStyle result = STYLES.get(key);
		if (result == null) {
			if (STYLES.size() >= MAX_ENTRIES) {
				STYLES.clear();
			}
			try {
				result = RuleMatcher.resolve(factsOf(stack, enchantments, stored, foil), target, category, ConfigManager.current());
			} catch (RuntimeException e) {
				result = ResolvedStyle.NONE;
			}
			STYLES.put(key, result);
		}

		lastItem = item;
		lastEnchantments = enchantments;
		lastStored = stored;
		lastFoil = foil;
		lastTarget = target;
		lastCategory = category;
		lastResult = result;
		return result;
	}

	/**
	 * The animated ARGB outline colour of a stack: enchantment colours, durability alert, then opacity.
	 */
	public static int outlineColor(ItemStack stack, OutlineLook look, double timeSeconds) {
		int color = look.color().evaluate(timeSeconds, look.color().usesEnchantmentColors() ? enchantmentColorsOf(stack) : null);
		if (stack.isDamageableItem()) {
			int max = stack.getMaxDamage();
			if (max > 0) {
				float remaining = 1.0f - (float) stack.getDamageValue() / max;
				color = ConfigManager.current().durabilityAlert.apply(color, remaining, timeSeconds);
			}
		}
		return Colors.multiplyAlpha(color, look.opacity());
	}

	/**
	 * The animated ARGB tint for the glint of a stack, or {@code 0xFFFFFFFF} when the look has no tint.
	 */
	public static int glintTint(ItemStack stack, GlintLook look, double timeSeconds) {
		if (look.tint() == null) {
			return 0xFFFFFFFF;
		}
		return look.tint().evaluate(timeSeconds, look.tint().usesEnchantmentColors() ? enchantmentColorsOf(stack) : null);
	}

	/**
	 * ARGB colours of the enchantments on a stack, sorted by id, cached per enchantment component.
	 */
	public static int[] enchantmentColorsOf(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return NO_COLORS;
		}
		ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
		ItemEnchantments stored = stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
		if (lastColorEnchantments == enchantments && lastColorStored == stored) {
			return lastColors;
		}
		EnchantKey key = new EnchantKey(enchantments, stored);
		int[] colors = ENCHANTMENT_COLORS.get(key);
		if (colors == null) {
			if (ENCHANTMENT_COLORS.size() >= MAX_ENTRIES) {
				ENCHANTMENT_COLORS.clear();
			}
			Set<String> ids = new HashSet<>();
			collectIds(enchantments, ids);
			collectIds(stored, ids);
			String[] sorted = ids.toArray(new String[0]);
			Arrays.sort(sorted);
			Map<String, String> overrides = ConfigManager.current().enchantmentColors;
			colors = new int[sorted.length];
			for (int i = 0; i < sorted.length; i++) {
				colors[i] = EnchantmentColors.colorFor(sorted[i], overrides);
			}
			ENCHANTMENT_COLORS.put(key, colors);
		}
		lastColorEnchantments = enchantments;
		lastColorStored = stored;
		lastColors = colors;
		return colors;
	}

	private static ItemFacts factsOf(ItemStack stack, ItemEnchantments enchantments, ItemEnchantments stored, boolean foil) {
		String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
		Set<String> itemTags = new HashSet<>();
		stack.getItemHolder().tags().forEach(tag -> itemTags.add(tag.location().toString()));

		Set<String> ids = new HashSet<>();
		Set<String> tags = new HashSet<>();
		collect(enchantments, ids, tags);
		collect(stored, ids, tags);
		return new ItemFacts(itemId, itemTags, ids, tags, !ids.isEmpty(), foil);
	}

	private static void collectIds(ItemEnchantments source, Set<String> ids) {
		collect(source, ids, null);
	}

	private static void collect(ItemEnchantments source, Set<String> ids, Set<String> tags) {
		for (Object2IntMap.Entry<Holder<Enchantment>> entry : source.entrySet()) {
			Holder<Enchantment> holder = entry.getKey();
			holder.unwrapKey().ifPresent(key -> ids.add(key.location().toString()));
			if (tags != null) {
				holder.tags().forEach((TagKey<Enchantment> tag) -> {
					ResourceLocation location = tag.location();
					tags.add(location.toString());
				});
			}
		}
	}

	private record StyleKey(Item item, ItemEnchantments enchantments, ItemEnchantments stored, boolean foil,
		OutlineTarget target, GlintCategory category) {
	}

	private record EnchantKey(ItemEnchantments enchantments, ItemEnchantments stored) {
	}
}
