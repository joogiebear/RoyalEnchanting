package com.mystipixel.royalenchanting.logic;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Plays clicks against one item the way MenuService.enchant does: evaluate, then pay only when
// paysRewards says so and remember the level that was paid.
class RewardLoopTest {

    private static final CostTable COSTS = new CostTable(List.of(5, 10, 15, 20, 30), List.of(0, 0, 0, 0, 0));
    private static final LevelCheck.Requirement FREE = new LevelCheck.Requirement(0, 0);
    private static final LevelCheck.State RICH = new LevelCheck.State(50, 20, 1000, 0);

    private static final class Item {
        int level;
        int highestRewarded;
        int payouts;

        void click(int clicked) {
            LevelCheck.Result r = LevelCheck.evaluate(clicked, level, 5, COSTS, FREE, RICH);
            assertTrue(r.allowed());
            if (LevelCheck.paysRewards(r.action(), clicked, highestRewarded)) {
                payouts++;
                highestRewarded = clicked;
            }
            level = r.action() == LevelCheck.Action.APPLY ? clicked : 0;
        }
    }

    @Test
    void applyRemoveCyclingPaysOnlyTheFirstApply() {
        Item sword = new Item();
        for (int i = 0; i < 20; i++) {
            sword.click(1);
        }
        assertEquals(0, sword.level);
        assertEquals(1, sword.payouts);
    }

    @Test
    void removingNeverPays() {
        assertFalse(LevelCheck.paysRewards(LevelCheck.Action.REMOVE, 3, 0));
        assertFalse(LevelCheck.paysRewards(LevelCheck.Action.NONE, 3, 0));
    }

    @Test
    void eachUpgradePaysOnce() {
        Item sword = new Item();
        sword.click(1);
        sword.click(2);
        sword.click(3);
        assertEquals(3, sword.payouts);

        sword.click(3);
        sword.click(1);
        sword.click(2);
        sword.click(3);
        assertEquals(3, sword.payouts);

        sword.click(4);
        assertEquals(4, sword.payouts);
    }

    @Test
    void anItemNeverPaidForPaysOnItsFirstApply() {
        // e.g. a level the item got from an anvil book, removed at the table and then applied here
        assertTrue(LevelCheck.paysRewards(LevelCheck.Action.APPLY, 3, 0));
        assertFalse(LevelCheck.paysRewards(LevelCheck.Action.APPLY, 3, 3));
        assertFalse(LevelCheck.paysRewards(LevelCheck.Action.APPLY, 2, 3));
    }
}
