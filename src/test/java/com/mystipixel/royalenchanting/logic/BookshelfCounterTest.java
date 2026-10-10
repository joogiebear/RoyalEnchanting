package com.mystipixel.royalenchanting.logic;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookshelfCounterTest {

    // a tiny world around the table at 0,0,0: shelf positions and solid (non-air) blocks
    private static final class World implements BookshelfCounter.Probe {
        final Set<String> shelves = new HashSet<>();
        final Set<String> solid = new HashSet<>();

        static String k(int x, int y, int z) {
            return x + "," + y + "," + z;
        }

        World shelf(int x, int y, int z) {
            shelves.add(k(x, y, z));
            return this;
        }

        World solid(int x, int y, int z) {
            solid.add(k(x, y, z));
            return this;
        }

        @Override
        public boolean isProvider(int dx, int dy, int dz) {
            return shelves.contains(k(dx, dy, dz));
        }

        @Override
        public boolean isTransmitter(int dx, int dy, int dz) {
            return !shelves.contains(k(dx, dy, dz)) && !solid.contains(k(dx, dy, dz));
        }
    }

    private static World fullRing() {
        World w = new World();
        for (int[] o : BookshelfCounter.OFFSETS) {
            w.shelf(o[0], o[1], o[2]);
        }
        return w;
    }

    @Test
    void vanillaHasThirtyTwoCandidatePositions() {
        assertEquals(32, BookshelfCounter.OFFSETS.size());
        for (int[] o : BookshelfCounter.OFFSETS) {
            assertTrue(Math.abs(o[0]) == 2 || Math.abs(o[2]) == 2);
            assertTrue(o[1] == 0 || o[1] == 1);
        }
    }

    @Test
    void emptySurroundingsGiveNoPower() {
        assertEquals(0, BookshelfCounter.power(new World(), 20));
    }

    @Test
    void fullTwoHighRingCountsThirtyTwoAndCapsAtMax() {
        World w = fullRing();
        assertEquals(32, BookshelfCounter.count(w));
        assertEquals(20, BookshelfCounter.power(w, 20));
        assertEquals(15, BookshelfCounter.power(w, 15));
    }

    @Test
    void shelvesTouchingTheTableDoNotCount() {
        World w = new World().shelf(1, 0, 0).shelf(-1, 0, 1).shelf(0, 1, 1);
        assertEquals(0, BookshelfCounter.count(w));
    }

    @Test
    void shelvesTwoLevelsUpDoNotCount() {
        World w = new World().shelf(2, 2, 0).shelf(-2, -1, 0);
        assertEquals(0, BookshelfCounter.count(w));
    }

    @Test
    void gapBlockMustBeAirLike() {
        // Shelf at (2,0,0): the gap is (1,0,0). A solid block there blocks it.
        World open = new World().shelf(2, 0, 0);
        assertEquals(1, BookshelfCounter.count(open));
        World blocked = new World().shelf(2, 0, 0).solid(1, 0, 0);
        assertEquals(0, BookshelfCounter.count(blocked));
    }

    @Test
    void cornerAndEdgeGapsUseTruncatingDivision() {
        // (2,0,2) checks (1,0,1); (2,0,1) checks (1,0,0); (-2,1,-1) checks (-1,1,0).
        assertEquals(0, BookshelfCounter.count(new World().shelf(2, 0, 2).solid(1, 0, 1)));
        assertEquals(1, BookshelfCounter.count(new World().shelf(2, 0, 2).solid(1, 0, 0)));
        assertEquals(0, BookshelfCounter.count(new World().shelf(2, 0, 1).solid(1, 0, 0)));
        assertEquals(0, BookshelfCounter.count(new World().shelf(-2, 1, -1).solid(-1, 1, 0)));
        assertEquals(1, BookshelfCounter.count(new World().shelf(-2, 1, -1).solid(-1, 1, -1)));
    }

    @Test
    void oneObstructionOnlyBlocksTheShelvesBehindIt() {
        World w = fullRing().solid(1, 0, 0);
        // (1,0,0) is the gap for (2,0,-1), (2,0,0) and (2,0,1) only.
        assertEquals(29, BookshelfCounter.count(w));
    }

    @Test
    void negativeMaxIsZero() {
        assertEquals(0, BookshelfCounter.power(fullRing(), -3));
    }
}
