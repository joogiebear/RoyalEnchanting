package com.mystipixel.royalenchanting.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RefundTest {

    // The server's perks: Reimbursement from Enchanting 10 (0.5% per level), Overcompensation from 20 (0.75%).
    private final Refund xp = new Refund(true, 10, "(%skill% - 9) * 0.5");
    private final Refund lapis = new Refund(true, 20, "(%skill% - 19) * 0.75");

    @Test
    void lockedBelowItsLevel() {
        assertEquals(0, xp.chanceFor(9));
        assertEquals(0, lapis.chanceFor(19));
    }

    @Test
    void scalesPerLevelFromItsStart() {
        assertEquals(0.5, xp.chanceFor(10));
        assertEquals(20.5, xp.chanceFor(50));
        assertEquals(0.75, lapis.chanceFor(20));
        assertEquals(23.25, lapis.chanceFor(50));
    }

    @Test
    void disabledAndClamped() {
        assertEquals(0, Refund.NONE.chanceFor(50));
        assertEquals(100, new Refund(true, 0, "%skill% * 10").chanceFor(50));
    }

    @Test
    void rollsInsideTheChanceHit() {
        assertTrue(xp.hits(50, 20.4));
        assertFalse(xp.hits(50, 20.5));
    }
}
