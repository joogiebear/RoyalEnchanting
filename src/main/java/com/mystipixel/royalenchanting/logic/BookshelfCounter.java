package com.mystipixel.royalenchanting.logic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Counts bookshelves the way vanilla does ({@code EnchantingTableBlock.BOOKSHELF_OFFSETS}): the 32-block
 * outer ring of the 5x5 square on the table's level and the one above, where the block halfway in,
 * at {@code (x / 2, y, z / 2)} with truncating division, must be a transmitter (air or replaceable).
 */
public final class BookshelfCounter {

    /** What sits at an offset from the table, relative to the table block. */
    public interface Probe {
        boolean isProvider(int dx, int dy, int dz);

        boolean isTransmitter(int dx, int dy, int dz);
    }

    public static final List<int[]> OFFSETS;

    static {
        List<int[]> offsets = new ArrayList<>();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = 0; dy <= 1; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (Math.abs(dx) == 2 || Math.abs(dz) == 2) {
                        offsets.add(new int[]{dx, dy, dz});
                    }
                }
            }
        }
        OFFSETS = Collections.unmodifiableList(offsets);
    }

    private BookshelfCounter() {
    }

    /** Every valid bookshelf, uncapped (at most 32). */
    public static int count(Probe probe) {
        int count = 0;
        for (int[] o : OFFSETS) {
            if (probe.isProvider(o[0], o[1], o[2]) && probe.isTransmitter(o[0] / 2, o[1], o[2] / 2)) {
                count++;
            }
        }
        return count;
    }

    public static int power(Probe probe, int maxPower) {
        return Math.max(0, Math.min(count(probe), maxPower));
    }
}
