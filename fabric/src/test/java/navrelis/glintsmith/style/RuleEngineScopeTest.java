package navrelis.glintsmith.style;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import navrelis.glintsmith.config.ConfigManager;
import navrelis.glintsmith.config.GlintsmithConfig;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The preview scope of {@link RuleEngine}: scoped lookups use the scope config and leave the live caches and the
 * one-entry fast path alone.
 */
class RuleEngineScopeTest {
	@BeforeAll
	static void bootstrap() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap(); // idempotent
	}

	@AfterEach
	void closeScope() {
		RuleEngine.endScope();
	}

	private static ItemStack glinting() {
		ItemStack stack = new ItemStack(Items.DIAMOND_SWORD);
		stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
		return stack;
	}

	private static ItemStack sharpened() {
		Holder.Reference<Enchantment> sharpness = Holder.Reference.createStandAlone(new HolderOwner<>() {
		}, Enchantments.SHARPNESS);
		ItemEnchantments.Mutable enchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
		enchantments.set(sharpness, 5);
		ItemStack stack = new ItemStack(Items.DIAMOND_SWORD);
		stack.set(DataComponents.ENCHANTMENTS, enchantments.toImmutable());
		return stack;
	}

	private static GlintsmithConfig snapshot() {
		GlintsmithConfig config = new GlintsmithConfig();
		config.outline.enabled = true;
		config.outline.thickness = 7.0f;
		config.outline.targets.firstPerson = true;
		config.enchantmentColors.put("minecraft:sharpness", "#123456");
		config.durabilityAlert.enabled = true;
		config.durabilityAlert.color = "#00FF00";
		return config.sanitize();
	}

	@Test
	void scopedLookupsUseTheScopeAndLeaveTheLiveResultAlone() {
		ItemStack stack = glinting();
		ResolvedStyle live = RuleEngine.resolve(stack, OutlineTarget.FIRST_PERSON, GlintCategory.ITEM);
		assertFalse(RuleEngine.inScope());

		RuleEngine.beginScope(snapshot());
		assertTrue(RuleEngine.inScope());
		ResolvedStyle scoped = RuleEngine.resolve(stack, OutlineTarget.FIRST_PERSON, GlintCategory.ITEM);
		assertNotNull(scoped.outline());
		assertEquals(7.0f, scoped.outline().thickness());
		RuleEngine.endScope();

		assertSame(live, RuleEngine.resolve(stack, OutlineTarget.FIRST_PERSON, GlintCategory.ITEM),
			"the fast path and the live cache still hold the live result");
		assertSame(live, RuleEngine.resolve(glinting(), OutlineTarget.FIRST_PERSON, GlintCategory.ITEM));
	}

	@Test
	void theSameSnapshotHitsItsCacheAndANewOneStartsFresh() {
		ItemStack stack = glinting();
		GlintsmithConfig first = snapshot();
		RuleEngine.beginScope(first);
		ResolvedStyle a = RuleEngine.resolve(stack, OutlineTarget.FIRST_PERSON, GlintCategory.ITEM);
		RuleEngine.endScope();

		RuleEngine.beginScope(first);
		assertSame(a, RuleEngine.resolve(stack, OutlineTarget.FIRST_PERSON, GlintCategory.ITEM));
		RuleEngine.endScope();

		GlintsmithConfig second = snapshot();
		second.outline.thickness = 3.0f;
		RuleEngine.beginScope(second);
		ResolvedStyle b = RuleEngine.resolve(stack, OutlineTarget.FIRST_PERSON, GlintCategory.ITEM);
		assertNotSame(a, b);
		assertEquals(3.0f, b.outline().thickness());
	}

	@Test
	void nullDoesNotOpenAScope() {
		RuleEngine.beginScope(null);
		assertFalse(RuleEngine.inScope());
	}

	@Test
	void scopedEnchantmentColoursOverrideWithoutTouchingTheLiveOnes() {
		ItemStack stack = sharpened();
		int[] live = RuleEngine.enchantmentColorsOf(stack);
		int[] liveCopy = live.clone();
		assertEquals(1, live.length);

		RuleEngine.beginScope(snapshot());
		assertArrayEquals(new int[] {0xFF123456}, RuleEngine.enchantmentColorsOf(stack));
		RuleEngine.endScope();

		assertSame(live, RuleEngine.enchantmentColorsOf(stack), "the live fast path was not overwritten");
		assertArrayEquals(liveCopy, live);
		assertArrayEquals(liveCopy, RuleEngine.enchantmentColorsOf(sharpened()));
	}

	@Test
	void outlineColourUsesTheScopedDurabilityAlert() {
		ItemStack stack = glinting();
		stack.setDamageValue(stack.getMaxDamage() - 1);
		GlintsmithConfig config = snapshot();
		RuleEngine.beginScope(config);
		OutlineLook look = RuleEngine.resolve(stack, OutlineTarget.FIRST_PERSON, GlintCategory.ITEM).outline();
		assertNotNull(look);
		float remaining = 1.0f - (float) stack.getDamageValue() / stack.getMaxDamage();
		int base = look.color().evaluate(0.3, null);
		int expected = Colors.multiplyAlpha(config.durabilityAlert.apply(base, remaining, 0.3), look.opacity());
		assertEquals(expected, RuleEngine.outlineColor(stack, look, 0.3));
		assertEquals(0x00FF00, expected & 0xFFFFFF, "pure alert colour a quarter period in");
		RuleEngine.endScope();

		int live = Colors.multiplyAlpha(ConfigManager.current().durabilityAlert.apply(base, remaining, 0.3), look.opacity());
		assertEquals(live, RuleEngine.outlineColor(stack, look, 0.3));
	}

	@Test
	void endingTwiceIsHarmless() {
		RuleEngine.beginScope(snapshot());
		RuleEngine.endScope();
		RuleEngine.endScope();
		assertFalse(RuleEngine.inScope());
	}
}
