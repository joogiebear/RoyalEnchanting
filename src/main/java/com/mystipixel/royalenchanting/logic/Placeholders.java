package com.mystipixel.royalenchanting.logic;

import java.util.Locale;
import java.util.Map;

/** Replaces {@code %name%} tokens from a map. Tokens not in the map are left alone. */
public final class Placeholders {

    private Placeholders() {
    }

    public static String apply(String text, Map<String, String> values) {
        if (text == null || values.isEmpty() || text.indexOf('%') < 0) {
            return text;
        }
        String out = text;
        for (Map.Entry<String, String> e : values.entrySet()) {
            out = out.replace("%" + e.getKey() + "%", e.getValue());
        }
        return out;
    }

    /** A number as it should read in a command or message: no ".0" on whole values, two decimals otherwise. */
    public static String number(double value) {
        if (value == Math.rint(value) && Math.abs(value) < 1e15) {
            return Long.toString((long) value);
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
