package com.mystipixel.royalenchanting.logic;

/** Roman numerals for enchant levels. */
public final class Roman {

    private static final int[] VALUES = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
    private static final String[] SYMBOLS = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};

    private Roman() {
    }

    public static String of(int number) {
        if (number <= 0 || number >= 4000) {
            return Integer.toString(number);
        }
        StringBuilder sb = new StringBuilder();
        int n = number;
        for (int i = 0; i < VALUES.length; i++) {
            while (n >= VALUES[i]) {
                sb.append(SYMBOLS[i]);
                n -= VALUES[i];
            }
        }
        return sb.toString();
    }
}
