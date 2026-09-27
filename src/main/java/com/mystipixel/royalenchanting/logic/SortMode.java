package com.mystipixel.royalenchanting.logic;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Predicate;

/** The enchant list's Sort button, cycled in this order. */
public enum SortMode {
    DEFAULT,
    MISSING_FIRST,
    A_TO_Z,
    Z_TO_A;

    public SortMode next() {
        SortMode[] all = values();
        return all[(ordinal() + 1) % all.length];
    }

    /**
     * Sorts a copy of {@code entries}, which arrive in the default (config) order.
     *
     * @param onItem true for enchants the item already has; MISSING_FIRST puts the others first
     */
    public <T> List<T> sort(List<T> entries, Function<T, String> name, Predicate<T> onItem) {
        List<T> out = new ArrayList<>(entries);
        Comparator<T> byName = Comparator.comparing(e -> name.apply(e).toLowerCase(Locale.ROOT));
        switch (this) {
            case DEFAULT -> {
            }
            // List.sort is stable, so each half keeps the default order.
            case MISSING_FIRST -> out.sort(Comparator.comparing(onItem::test));
            case A_TO_Z -> out.sort(byName);
            case Z_TO_A -> out.sort(byName.reversed());
        }
        return out;
    }
}
