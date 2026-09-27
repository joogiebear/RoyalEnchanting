package com.mystipixel.royalenchanting.logic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Counts bookshelves around an enchanting table exactly the way vanilla does
 * ({@code EnchantingTableBlock.BOOKSHELF_OFFSETS} and {@code isValidBookShelf}).
 *
 * <p>The candidates are the outer ring of the 5x5 square centred on the table, on the table's own level
 * and the level above: every offset with x and z in [-2, 2], y in [0, 1] and |x| == 2 or |z| == 2, which
 * is 32 positions. A candidate counts when it is a power provider (a bookshelf) and the block halfway
 * between it and the table, at {@code (x / 2, y, z / 2)} with Java's truncating division, is a power
 * transmitter (air or another replaceable block). That is the one-block gap vanilla requires.
 *
 * <p>Vanilla caps the table at 15; this plugin caps power at a configurable maximum (20 by default).
 */
public final class BookshelfCounter {

    /** What sits at an offset from the table. Coordinates are relative to the table block. */
    public interface Probe {
        boolean isProvider(int dx, int dy, int dz);

        boolean isTransmitter(int dx, int dy, int dz);
    }

    /** The vanilla candidate offsets, each {dx, dy, dz}. */
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

    /** Bookshelf Power: the count, capped at {@code maxPower}. */
    public static int power(Probe probe, int maxPower) {
        return Math.max(0, Math.min(count(probe), maxPower));
    }
}
