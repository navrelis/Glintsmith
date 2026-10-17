package navrelis.glintsmith.render.glint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import navrelis.glintsmith.style.GlintCategory;
import navrelis.glintsmith.style.OutlineTarget;
import net.minecraft.world.item.ItemDisplayContext;

/**
 * The category decision table (design 5.3), the context stack and the display context mapping.
 */
class GlintContextTest {
	@BeforeEach
	@AfterEach
	void clean() {
		GlintContext.reset();
	}

	private static GlintCategory decide(GlintKind kind) {
		return GlintCategories.decide(kind, GlintContext.top());
	}

	@Test
	void withoutAFrameTheKindDecides() {
		assertNull(GlintContext.top());
		assertEquals(GlintCategory.ITEM, decide(GlintKind.GLINT));
		assertEquals(GlintCategory.ITEM, decide(GlintKind.GLINT_TRANSLUCENT));
		assertEquals(GlintCategory.ENTITY, decide(GlintKind.ENTITY_GLINT));
		assertEquals(GlintCategory.ENTITY, decide(GlintKind.ENTITY_GLINT_DIRECT));
		assertEquals(GlintCategory.ARMOR, decide(GlintKind.ARMOR_ENTITY_GLINT));
	}

	@Test
	void guiFrameWinsOverEverything() {
		GlintContext.pushItem(null, ItemDisplayContext.GUI);
		for (GlintKind kind : GlintKind.values()) {
			assertEquals(GlintCategory.GUI, decide(kind), kind.name());
		}
		GlintContext.pushBewlr(null, ItemDisplayContext.GUI);
		assertEquals(GlintCategory.GUI, decide(GlintKind.GLINT));
		assertEquals(GlintCategory.GUI, decide(GlintKind.ARMOR_ENTITY_GLINT));
	}

	@Test
	void armorFrameMakesEverythingArmor() {
		GlintContext.pushArmor(null);
		for (GlintKind kind : GlintKind.values()) {
			assertEquals(GlintCategory.ARMOR, decide(kind), kind.name());
		}
	}

	@Test
	void bewlrFrameMakesItemKindsEntity() {
		GlintContext.pushItem(null, ItemDisplayContext.GROUND);
		assertEquals(GlintCategory.ITEM, decide(GlintKind.GLINT));
		GlintContext.pushBewlr(null, ItemDisplayContext.GROUND);
		assertEquals(GlintCategory.ENTITY, decide(GlintKind.GLINT));
		assertEquals(GlintCategory.ENTITY, decide(GlintKind.GLINT_TRANSLUCENT));
		assertEquals(GlintCategory.ENTITY, decide(GlintKind.ENTITY_GLINT));
		// the armor kind is checked before the BEWLR ancestor
		assertEquals(GlintCategory.ARMOR, decide(GlintKind.ARMOR_ENTITY_GLINT));
	}

	@Test
	void plainItemFrameKeepsTheItemCategory() {
		GlintContext.pushItem(null, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND);
		assertEquals(GlintCategory.ITEM, decide(GlintKind.GLINT));
		assertEquals(GlintCategory.ITEM, decide(GlintKind.GLINT_TRANSLUCENT));
		assertEquals(GlintCategory.ENTITY, decide(GlintKind.ENTITY_GLINT_DIRECT));
	}

	@Test
	void ancestorsAreInheritedAndRestoredOnPop() {
		GlintContext.pushItem(null, ItemDisplayContext.GUI);
		GlintContext.pushItem(null, ItemDisplayContext.FIXED); // nested render inside a GUI item
		assertTrue(GlintContext.top().guiAncestor);
		assertEquals(GlintCategory.GUI, decide(GlintKind.GLINT));
		GlintContext.pop();
		GlintContext.pop();
		assertNull(GlintContext.top());

		GlintContext.pushArmor(null);
		GlintContext.pushItem(null, ItemDisplayContext.NONE);
		assertTrue(GlintContext.top().armorAncestor);
		assertFalse(GlintContext.top().guiAncestor);
		assertEquals(GlintCategory.ARMOR, decide(GlintKind.GLINT));
		GlintContext.pop();
		assertEquals(GlintContext.ARMOR_FRAME, GlintContext.top().kind);
		GlintContext.pop();
		assertEquals(GlintCategory.ITEM, decide(GlintKind.GLINT));
	}

	@Test
	void ruleTargetFollowsTheCategory() {
		GlintContext.pushItem(null, ItemDisplayContext.GROUND);
		GlintContext.Frame f = GlintContext.top();
		assertEquals(OutlineTarget.DROPPED, GlintCategories.targetFor(GlintCategory.ITEM, f));
		assertEquals(OutlineTarget.DROPPED, GlintCategories.targetFor(GlintCategory.ENTITY, f));
		assertEquals(OutlineTarget.GUI, GlintCategories.targetFor(GlintCategory.GUI, f));
		assertEquals(OutlineTarget.ARMOR, GlintCategories.targetFor(GlintCategory.ARMOR, f));
	}

	@Test
	void displayContextMapping() {
		assertEquals(OutlineTarget.FIRST_PERSON, GlintContext.targetOf(ItemDisplayContext.FIRST_PERSON_LEFT_HAND));
		assertEquals(OutlineTarget.FIRST_PERSON, GlintContext.targetOf(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND));
		assertEquals(OutlineTarget.THIRD_PERSON, GlintContext.targetOf(ItemDisplayContext.THIRD_PERSON_LEFT_HAND));
		assertEquals(OutlineTarget.THIRD_PERSON, GlintContext.targetOf(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND));
		assertEquals(OutlineTarget.DROPPED, GlintContext.targetOf(ItemDisplayContext.GROUND));
		assertEquals(OutlineTarget.ITEM_FRAME, GlintContext.targetOf(ItemDisplayContext.FIXED));
		assertEquals(OutlineTarget.GUI, GlintContext.targetOf(ItemDisplayContext.GUI));
		assertEquals(OutlineTarget.OTHER, GlintContext.targetOf(ItemDisplayContext.HEAD));
		assertEquals(OutlineTarget.OTHER, GlintContext.targetOf(ItemDisplayContext.NONE));
		assertEquals(OutlineTarget.OTHER, GlintContext.targetOf(null));
		for (ItemDisplayContext context : ItemDisplayContext.values()) {
			assertNotNull(GlintContext.targetOf(context), context.name());
		}
	}

	@Test
	void overflowIsIgnoredAndStaysBalanced() {
		for (int i = 0; i < GlintContext.CAPACITY + 5; i++) {
			GlintContext.pushItem(null, i == 0 ? ItemDisplayContext.GUI : ItemDisplayContext.GROUND);
		}
		assertEquals(GlintContext.CAPACITY, GlintContext.depth());
		GlintContext.Frame top = GlintContext.top();
		for (int i = 0; i < 5; i++) {
			GlintContext.pop(); // the ignored pushes
			assertSame(top, GlintContext.top());
		}
		for (int i = 0; i < GlintContext.CAPACITY; i++) {
			GlintContext.pop();
		}
		assertEquals(0, GlintContext.depth());
		assertNull(GlintContext.top());
	}

	@Test
	void extraPopsAreHarmless() {
		GlintContext.pop();
		GlintContext.pop();
		assertEquals(0, GlintContext.depth());
		GlintContext.pushArmor(null);
		assertEquals(1, GlintContext.depth());
	}

	@Test
	void lookCacheIsPerCategoryAndClearedOnPush() {
		GlintContext.pushItem(null, ItemDisplayContext.GROUND);
		GlintContext.Frame f = GlintContext.top();
		assertFalse(f.isResolved(GlintCategory.ITEM));
		f.cache(GlintCategory.ITEM, null);
		assertTrue(f.isResolved(GlintCategory.ITEM));
		assertFalse(f.isResolved(GlintCategory.ENTITY));
		GlintContext.pop();
		GlintContext.pushItem(null, ItemDisplayContext.GROUND);
		assertFalse(GlintContext.top().isResolved(GlintCategory.ITEM));
	}
}
