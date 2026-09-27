package com.mystipixel.royalenchanting.logic;

import java.util.List;

/**
 * XP-level and lapis cost of each enchant level; index 0 is level I. The table offers as many levels as
 * both lists cover, so the default five entries cap every enchant at V.
 */
public record CostTable(List<Integer> xpLevels, List<Integer> lapis) {

    public CostTable {
        xpLevels = List.copyOf(xpLevels);
        lapis = List.copyOf(lapis);
    }

    /** The highest level this table has a price for. */
    public int pricedLevels() {
        return Math.min(xpLevels.size(), lapis.size());
    }

    public int xp(int level) {
        return valueAt(xpLevels, level);
    }

    public int lapis(int level) {
        return valueAt(lapis, level);
    }

    /**
     * The highest level the table offers for one enchant: its own maximum, the priced levels, and an
     * optional per-enchant cap ({@code <= 0} means none), whichever is lowest.
     */
    public int tableMax(int enchantMaxLevel, int configuredCap) {
        int max = Math.min(enchantMaxLevel, pricedLevels());
        if (configuredCap > 0) {
            max = Math.min(max, configuredCap);
        }
        return Math.max(0, max);
    }

    private static int valueAt(List<Integer> values, int level) {
        if (level < 1 || level > values.size()) {
            throw new IllegalArgumentException("No cost for level " + level);
        }
        return Math.max(0, values.get(level - 1));
    }
}
