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
 * <p>
 * A <b>scope</b> ({@link #beginScope}) makes every lookup use another config, for example the settings screen's
 * preview snapshot, until {@link #endScope()}. Scoped lookups have their own caches and never read or write the live
 * caches or the fast path, so the world render after the preview still hits its cached results.
 */
public final class RuleEngine {
	private static final int MAX_ENTRIES = 4096;
	private static final int MAX_SCOPE_ENTRIES = 256;
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

	// Scope: null = the live config. scopeOwner is the config the SCOPE_* caches were filled for.
	private static GlintsmithConfig scope;
	private static GlintsmithConfig scopeOwner;
	private static final Map<StyleKey, ResolvedStyle> SCOPE_STYLES = new HashMap<>();
	private static final Map<EnchantKey, int[]> SCOPE_COLORS = new HashMap<>();

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
		SCOPE_STYLES.clear();
		SCOPE_COLORS.clear();
		scopeOwner = null;
	}

	/**
	 * Makes every lookup use {@code config} (sanitised) until {@link #endScope()}. A config other than the one of the
	 * last scope drops the scoped caches, so pass the same instance as long as nothing changed. Null is ignored.
	 */
	public static void beginScope(GlintsmithConfig config) {
		if (config == null) {
			return;
		}
		if (config != scopeOwner) {
			SCOPE_STYLES.clear();
			SCOPE_COLORS.clear();
			scopeOwner = config;
		}
		scope = config;
	}

	/**
	 * Back to the live config. Safe to call without a scope.
	 */
	public static void endScope() {
		scope = null;
	}

	/**
	 * @return whether a {@link #beginScope scope} is open
	 */
	public static boolean inScope() {
		return scope != null;
	}

	private static GlintsmithConfig config() {
		GlintsmithConfig scoped = scope;
		return scoped != null ? scoped : ConfigManager.current();
	}

	/**
	 * Resolves what to draw for a stack. Empty stacks give {@link ResolvedStyle#NONE}.
	 */
	public static ResolvedStyle resolve(ItemStack stack, OutlineTarget target, GlintCategory category) {
		if (stack == null || stack.isEmpty()) {
			return ResolvedStyle.NONE;
		}
		if (scope != null) {
			return resolveScoped(stack, target, category, scope);
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

	/** {@link #resolve} under a scope: own cache, no fast path. */
	private static ResolvedStyle resolveScoped(ItemStack stack, OutlineTarget target, GlintCategory category, GlintsmithConfig config) {
		ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
		ItemEnchantments stored = stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
		boolean foil = stack.hasFoil();
		StyleKey lookup = STYLE_LOOKUP;
		lookup.set(stack.getItem(), enchantments, stored, foil, target, category);
		ResolvedStyle result = SCOPE_STYLES.get(lookup);
		if (result == null) {
			StyleKey key = lookup.copy();
			if (SCOPE_STYLES.size() >= MAX_SCOPE_ENTRIES) {
				SCOPE_STYLES.clear();
			}
			try {
				result = RuleMatcher.resolve(factsOf(stack, enchantments, stored, foil), target, category, config);
			} catch (RuntimeException e) {
				result = ResolvedStyle.NONE;
			}
			SCOPE_STYLES.put(key, result);
		}
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
				color = config().durabilityAlert.apply(color, remaining, timeSeconds);
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
		GlintsmithConfig scoped = scope;
		if (scoped != null) {
			return colorsOf(enchantments, stored, SCOPE_COLORS, MAX_SCOPE_ENTRIES, scoped.enchantmentColors);
		}
		if (lastColorEnchantments == enchantments && lastColorStored == stored) {
			return lastColors;
		}
		int[] colors = colorsOf(enchantments, stored, ENCHANTMENT_COLORS, MAX_ENTRIES, ConfigManager.current().enchantmentColors);
		lastColorEnchantments = enchantments;
		lastColorStored = stored;
		lastColors = colors;
		return colors;
	}

	private static int[] colorsOf(ItemEnchantments enchantments, ItemEnchantments stored, Map<EnchantKey, int[]> cache,
		int maxEntries, Map<String, String> overrides) {
		EnchantKey lookup = ENCHANT_LOOKUP;
		lookup.set(enchantments, stored);
		int[] colors = cache.get(lookup);
		if (colors == null) {
			EnchantKey key = lookup.copy();
			if (cache.size() >= maxEntries) {
				cache.clear();
			}
			Set<String> ids = new HashSet<>();
			collectIds(enchantments, ids);
			collectIds(stored, ids);
			String[] sorted = ids.toArray(new String[0]);
			Arrays.sort(sorted);
			colors = new int[sorted.length];
			for (int i = 0; i < sorted.length; i++) {
				colors[i] = EnchantmentColors.colorFor(sorted[i], overrides);
			}
			cache.put(key, colors);
		}
		return colors;
	}

	/**
	 * The facts the rules see for a stack, built fresh (not cached). Empty stacks give facts without enchantments.
	 */
	public static ItemFacts factsOf(ItemStack stack) {
		ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
		ItemEnchantments stored = stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
		return factsOf(stack, enchantments, stored, stack.hasFoil());
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
