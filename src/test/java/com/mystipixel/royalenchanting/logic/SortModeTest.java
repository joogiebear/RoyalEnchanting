package com.mystipixel.royalenchanting.logic;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SortModeTest {

    private static final List<String> DEFAULT = List.of("Sharpness", "critical", "Vicious", "Lifesteal", "Cleave");
    private static final Set<String> ON_ITEM = Set.of("Sharpness", "Lifesteal");

    private static List<String> sort(SortMode mode) {
        return mode.sort(DEFAULT, s -> s, ON_ITEM::contains);
    }

    @Test
    void cyclesThroughAllModes() {
        assertEquals(SortMode.MISSING_FIRST, SortMode.DEFAULT.next());
        assertEquals(SortMode.A_TO_Z, SortMode.MISSING_FIRST.next());
        assertEquals(SortMode.Z_TO_A, SortMode.A_TO_Z.next());
        assertEquals(SortMode.DEFAULT, SortMode.Z_TO_A.next());
    }

    @Test
    void defaultKeepsConfigOrder() {
        assertEquals(DEFAULT, sort(SortMode.DEFAULT));
    }

    @Test
    void missingFirstKeepsOrderWithinEachHalf() {
        assertEquals(List.of("critical", "Vicious", "Cleave", "Sharpness", "Lifesteal"), sort(SortMode.MISSING_FIRST));
    }

    @Test
    void alphabeticalIgnoresCase() {
        assertEquals(List.of("Cleave", "critical", "Lifesteal", "Sharpness", "Vicious"), sort(SortMode.A_TO_Z));
        assertEquals(List.of("Vicious", "Sharpness", "Lifesteal", "critical", "Cleave"), sort(SortMode.Z_TO_A));
    }
}
