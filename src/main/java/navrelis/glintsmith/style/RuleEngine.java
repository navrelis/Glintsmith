package navrelis.glintsmith.style;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
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

	// Reused for lookups so a cache hit allocates nothing; never stored in a map (a copy is inserted on a miss).
	private static final StyleKey STYLE_LOOKUP = new StyleKey();
	private static final EnchantKey ENCHANT_LOOKUP = new EnchantKey();

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

		StyleKey lookup = STYLE_LOOKUP;
		lookup.set(item, enchantments, stored, foil, target, category);
		ResolvedStyle result = STYLES.get(lookup);
		if (result == null) {
			StyleKey key = lookup.copy();
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
		EnchantKey lookup = ENCHANT_LOOKUP;
		lookup.set(enchantments, stored);
		int[] colors = ENCHANTMENT_COLORS.get(lookup);
		if (colors == null) {
			EnchantKey key = lookup.copy();
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

	/**
	 * Map key with mutable fields so one instance ({@link #STYLE_LOOKUP}) can be refilled for every lookup.
	 * Instances stored in {@link #STYLES} are copies and are never modified.
	 */
	static final class StyleKey {
		private Item item;
		private ItemEnchantments enchantments;
		private ItemEnchantments stored;
		private boolean foil;
		private OutlineTarget target;
		private GlintCategory category;

		void set(Item item, ItemEnchantments enchantments, ItemEnchantments stored, boolean foil,
			OutlineTarget target, GlintCategory category) {
			this.item = item;
			this.enchantments = enchantments;
			this.stored = stored;
			this.foil = foil;
			this.target = target;
			this.category = category;
		}

		StyleKey copy() {
			StyleKey key = new StyleKey();
			key.set(item, enchantments, stored, foil, target, category);
			return key;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			return o instanceof StyleKey k && item == k.item && foil == k.foil && target == k.target
				&& category == k.category && Objects.equals(enchantments, k.enchantments)
				&& Objects.equals(stored, k.stored);
		}

		@Override
		public int hashCode() {
			int h = System.identityHashCode(item);
			h = 31 * h + Objects.hashCode(enchantments);
			h = 31 * h + Objects.hashCode(stored);
			h = 31 * h + (foil ? 1 : 0);
			h = 31 * h + System.identityHashCode(target);
			h = 31 * h + System.identityHashCode(category);
			return h;
		}
	}

	/** Same scheme as {@link StyleKey}, with {@link #ENCHANT_LOOKUP} as the reusable instance. */
	static final class EnchantKey {
		private ItemEnchantments enchantments;
		private ItemEnchantments stored;

		void set(ItemEnchantments enchantments, ItemEnchantments stored) {
			this.enchantments = enchantments;
			this.stored = stored;
		}

		EnchantKey copy() {
			EnchantKey key = new EnchantKey();
			key.set(enchantments, stored);
			return key;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			return o instanceof EnchantKey k && Objects.equals(enchantments, k.enchantments)
				&& Objects.equals(stored, k.stored);
		}

		@Override
		public int hashCode() {
			return 31 * Objects.hashCode(enchantments) + Objects.hashCode(stored);
		}
	}
}
