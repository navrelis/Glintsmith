package navrelis.glintsmith.render.outline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import navrelis.glintsmith.style.GlintCategory;
import navrelis.glintsmith.style.OutlineTarget;
import net.minecraft.world.item.ItemDisplayContext;
import org.junit.jupiter.api.Test;

class OutlineContextsTest {
	@Test
	void everyDisplayContextHasATarget() {
		assertEquals(OutlineTarget.FIRST_PERSON, OutlineContexts.targetOf(ItemDisplayContext.FIRST_PERSON_LEFT_HAND));
		assertEquals(OutlineTarget.FIRST_PERSON, OutlineContexts.targetOf(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND));
		assertEquals(OutlineTarget.THIRD_PERSON, OutlineContexts.targetOf(ItemDisplayContext.THIRD_PERSON_LEFT_HAND));
		assertEquals(OutlineTarget.THIRD_PERSON, OutlineContexts.targetOf(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND));
		assertEquals(OutlineTarget.DROPPED, OutlineContexts.targetOf(ItemDisplayContext.GROUND));
		assertEquals(OutlineTarget.ITEM_FRAME, OutlineContexts.targetOf(ItemDisplayContext.FIXED));
		assertEquals(OutlineTarget.GUI, OutlineContexts.targetOf(ItemDisplayContext.GUI));
		assertEquals(OutlineTarget.OTHER, OutlineContexts.targetOf(ItemDisplayContext.HEAD));
		assertEquals(OutlineTarget.OTHER, OutlineContexts.targetOf(ItemDisplayContext.NONE));
		assertEquals(OutlineTarget.OTHER, OutlineContexts.targetOf(null));
		for (ItemDisplayContext context : ItemDisplayContext.values()) {
			assertTrue(OutlineContexts.targetOf(context) != null, context.name());
		}
	}

	@Test
	void theWorldPhaseTreatsForeignContextsAsOther() {
		assertEquals(OutlineTarget.THIRD_PERSON, OutlineContexts.forPhase(OutlinePhase.WORLD, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND));
		assertEquals(OutlineTarget.DROPPED, OutlineContexts.forPhase(OutlinePhase.WORLD, ItemDisplayContext.GROUND));
		assertEquals(OutlineTarget.ITEM_FRAME, OutlineContexts.forPhase(OutlinePhase.WORLD, ItemDisplayContext.FIXED));
		assertEquals(OutlineTarget.OTHER, OutlineContexts.forPhase(OutlinePhase.WORLD, ItemDisplayContext.HEAD));
		assertEquals(OutlineTarget.OTHER, OutlineContexts.forPhase(OutlinePhase.WORLD, ItemDisplayContext.FIRST_PERSON_LEFT_HAND));
		assertEquals(OutlineTarget.OTHER, OutlineContexts.forPhase(OutlinePhase.WORLD, ItemDisplayContext.GUI));
	}

	@Test
	void handAndGuiPhasesOnlyTakeTheirOwnContexts() {
		assertEquals(OutlineTarget.FIRST_PERSON, OutlineContexts.forPhase(OutlinePhase.HAND, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND));
		assertNull(OutlineContexts.forPhase(OutlinePhase.HAND, ItemDisplayContext.GUI));
		assertEquals(OutlineTarget.GUI, OutlineContexts.forPhase(OutlinePhase.GUI, ItemDisplayContext.GUI));
		assertNull(OutlineContexts.forPhase(OutlinePhase.GUI, ItemDisplayContext.GROUND));
		assertNull(OutlineContexts.forPhase(OutlinePhase.NONE, ItemDisplayContext.GUI));
		assertNull(OutlineContexts.forPhase(null, ItemDisplayContext.GUI));
	}

	@Test
	void everyHeldItemInTheHandPhaseIsFirstPerson() {
		// First-person animation mods (Punchy) draw the held item with the third-person transforms of their arm model.
		for (ItemDisplayContext context : new ItemDisplayContext[] {ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
			ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, ItemDisplayContext.THIRD_PERSON_LEFT_HAND,
			ItemDisplayContext.THIRD_PERSON_RIGHT_HAND}) {
			assertEquals(OutlineTarget.FIRST_PERSON, OutlineContexts.forPhase(OutlinePhase.HAND, context), context.name());
		}
	}

	@Test
	void theHandPhaseIgnoresContextsThatAreNotHeldItems() {
		for (ItemDisplayContext context : new ItemDisplayContext[] {ItemDisplayContext.GUI, ItemDisplayContext.GROUND,
			ItemDisplayContext.FIXED, ItemDisplayContext.HEAD, ItemDisplayContext.NONE}) {
			assertNull(OutlineContexts.forPhase(OutlinePhase.HAND, context), context.name());
		}
		assertNull(OutlineContexts.forPhase(OutlinePhase.HAND, null));
	}

	@Test
	void everyTargetTheHandPhaseProducesIsAcceptedByIt() {
		for (ItemDisplayContext context : ItemDisplayContext.values()) {
			OutlineTarget target = OutlineContexts.forPhase(OutlinePhase.HAND, context);
			if (target != null) {
				assertTrue(OutlineContexts.phaseAccepts(OutlinePhase.HAND, target), context.name());
			}
		}
	}

	@Test
	void thePreviewPhaseOutlinesEveryContextAsFirstPerson() {
		for (ItemDisplayContext context : ItemDisplayContext.values()) {
			assertEquals(OutlineTarget.FIRST_PERSON, OutlineContexts.forPhase(OutlinePhase.PREVIEW, context), context.name());
		}
		assertEquals(OutlineTarget.FIRST_PERSON, OutlineContexts.forPhase(OutlinePhase.PREVIEW, null));
	}

	@Test
	void thePreviewPhaseOnlyAcceptsFirstPerson() {
		for (OutlineTarget target : OutlineTarget.values()) {
			assertEquals(target == OutlineTarget.FIRST_PERSON, OutlineContexts.phaseAccepts(OutlinePhase.PREVIEW, target),
				target.name());
		}
		assertFalse(OutlineContexts.phaseAccepts(OutlinePhase.PREVIEW, null));
	}

	@Test
	void phaseAcceptance() {
		assertTrue(OutlineContexts.phaseAccepts(OutlinePhase.WORLD, OutlineTarget.ARMOR));
		assertTrue(OutlineContexts.phaseAccepts(OutlinePhase.WORLD, OutlineTarget.OTHER));
		assertFalse(OutlineContexts.phaseAccepts(OutlinePhase.WORLD, OutlineTarget.FIRST_PERSON));
		assertFalse(OutlineContexts.phaseAccepts(OutlinePhase.WORLD, OutlineTarget.GUI));
		assertTrue(OutlineContexts.phaseAccepts(OutlinePhase.HAND, OutlineTarget.FIRST_PERSON));
		assertFalse(OutlineContexts.phaseAccepts(OutlinePhase.HAND, OutlineTarget.ARMOR));
		assertTrue(OutlineContexts.phaseAccepts(OutlinePhase.GUI, OutlineTarget.GUI));
		assertFalse(OutlineContexts.phaseAccepts(OutlinePhase.GUI, OutlineTarget.OTHER));
		assertFalse(OutlineContexts.phaseAccepts(OutlinePhase.NONE, OutlineTarget.GUI));
	}

	@Test
	void glintCategoryFollowsTheTarget() {
		assertEquals(GlintCategory.GUI, OutlineContexts.categoryOf(OutlineTarget.GUI));
		assertEquals(GlintCategory.ARMOR, OutlineContexts.categoryOf(OutlineTarget.ARMOR));
		assertEquals(GlintCategory.ITEM, OutlineContexts.categoryOf(OutlineTarget.DROPPED));
		assertEquals(GlintCategory.ITEM, OutlineContexts.categoryOf(OutlineTarget.FIRST_PERSON));
	}
}
