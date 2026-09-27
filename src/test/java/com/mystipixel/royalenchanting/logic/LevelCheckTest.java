package com.mystipixel.royalenchanting.logic;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LevelCheckTest {

    private static final CostTable COSTS = new CostTable(List.of(5, 10, 15, 20, 30), List.of(1, 2, 3, 4, 5));
    private static final LevelCheck.Requirement TIER_12_16 = new LevelCheck.Requirement(12, 16);
    private static final LevelCheck.Requirement FREE = new LevelCheck.Requirement(0, 0);

    private static LevelCheck.State rich() {
        return new LevelCheck.State(50, 20, 100, 64);
    }

    @Test
    void defaultCostsMatchTheApprovedTable() {
        assertEquals(List.of(5, 10, 15, 20, 30), List.of(COSTS.xp(1), COSTS.xp(2), COSTS.xp(3), COSTS.xp(4), COSTS.xp(5)));
        assertEquals(List.of(1, 2, 3, 4, 5), List.of(COSTS.lapis(1), COSTS.lapis(2), COSTS.lapis(3), COSTS.lapis(4), COSTS.lapis(5)));
        assertThrows(IllegalArgumentException.class, () -> COSTS.xp(6));
        assertThrows(IllegalArgumentException.class, () -> COSTS.lapis(0));
    }

    @Test
    void tableMaxIsTheLowestOfEnchantPricedAndCap() {
        assertEquals(5, COSTS.tableMax(5, 0));
        assertEquals(3, COSTS.tableMax(3, 0));   // unbreaking
        assertEquals(1, COSTS.tableMax(1, 0));   // replenish
        assertEquals(5, COSTS.tableMax(10, 0));  // only five levels are priced
        assertEquals(2, COSTS.tableMax(5, 2));   // per-enchant cap
        assertEquals(5, COSTS.tableMax(5, 9));
    }

    @Test
    void applyingAFreshEnchantWithEverythingMet() {
        LevelCheck.Result r = LevelCheck.evaluate(3, 0, 5, COSTS, FREE, rich());
        assertEquals(LevelCheck.Action.APPLY, r.action());
        assertEquals(15, r.xpCost());
        assertEquals(3, r.lapisCost());
        assertTrue(r.allowed());
    }

    @Test
    void upgradingToAHigherLevelIsAnApply() {
        LevelCheck.Result r = LevelCheck.evaluate(5, 2, 5, COSTS, FREE, rich());
        assertEquals(LevelCheck.Action.APPLY, r.action());
        assertEquals(30, r.xpCost());
        assertTrue(r.allowed());
    }

    @Test
    void clickingTheCurrentLevelRemovesForTheSameCost() {
        LevelCheck.Result r = LevelCheck.evaluate(4, 4, 5, COSTS, FREE, rich());
        assertEquals(LevelCheck.Action.REMOVE, r.action());
        assertEquals(20, r.xpCost());
        assertEquals(4, r.lapisCost());
        assertTrue(r.allowed());
    }

    @Test
    void lowerThanCurrentIsRefused() {
        LevelCheck.Result r = LevelCheck.evaluate(2, 4, 5, COSTS, FREE, rich());
        assertEquals(LevelCheck.Action.NONE, r.action());
        assertTrue(r.problems().contains(LevelCheck.Problem.HIGHER_LEVEL));
        assertFalse(r.allowed());
    }

    @Test
    void aboveTableMaxCanNeitherBeRemovedNorChanged() {
        for (int level = 1; level <= 5; level++) {
            LevelCheck.Result r = LevelCheck.evaluate(level, 6, 5, COSTS, FREE, rich());
            assertEquals(LevelCheck.Action.NONE, r.action());
            assertTrue(r.problems().contains(LevelCheck.Problem.ABOVE_TABLE_MAX));
            assertFalse(r.allowed());
        }
    }

    @Test
    void lockedBySkill() {
        LevelCheck.State state = new LevelCheck.State(11, 20, 100, 64);
        LevelCheck.Result r = LevelCheck.evaluate(1, 0, 5, COSTS, TIER_12_16, state);
        assertEquals(Set.of(LevelCheck.Problem.SKILL), r.problems());
        assertFalse(r.allowed());
        assertFalse(LevelCheck.unlocked(TIER_12_16, state));
    }

    @Test
    void lockedByBookshelfPower() {
        LevelCheck.State state = new LevelCheck.State(12, 15, 100, 64);
        LevelCheck.Result r = LevelCheck.evaluate(1, 0, 5, COSTS, TIER_12_16, state);
        assertEquals(Set.of(LevelCheck.Problem.BOOKSHELF_POWER), r.problems());
        assertFalse(LevelCheck.unlocked(TIER_12_16, state));
    }

    @Test
    void exactlyMeetingRequirementsUnlocks() {
        LevelCheck.State state = new LevelCheck.State(12, 16, 30, 5);
        assertTrue(LevelCheck.unlocked(TIER_12_16, state));
        assertTrue(LevelCheck.evaluate(5, 0, 5, COSTS, TIER_12_16, state).allowed());
    }

    @Test
    void notEnoughXpOrLapis() {
        LevelCheck.State poor = new LevelCheck.State(50, 20, 14, 2);
        LevelCheck.Result r = LevelCheck.evaluate(3, 0, 5, COSTS, FREE, poor);
        assertEquals(Set.of(LevelCheck.Problem.XP, LevelCheck.Problem.LAPIS), r.problems());
        // Removing costs the same, so it needs the same XP and lapis.
        LevelCheck.Result remove = LevelCheck.evaluate(3, 3, 5, COSTS, FREE, poor);
        assertEquals(LevelCheck.Action.REMOVE, remove.action());
        assertFalse(remove.allowed());
    }
}
