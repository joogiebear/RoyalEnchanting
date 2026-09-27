package com.mystipixel.royalenchanting.logic;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExpressionTest {

    private static double eval(String formula, Map<String, String> values) {
        return Expression.evaluate(Placeholders.apply(formula, values));
    }

    @Test
    void arithmeticAndPrecedence() {
        assertEquals(7, Expression.evaluate("1 + 2 * 3"));
        assertEquals(9, Expression.evaluate("(1 + 2) * 3"));
        assertEquals(512, Expression.evaluate("2 ^ 3 ^ 2"));
        assertEquals(-4, Expression.evaluate("-2 ^ 2"));
        assertEquals(0.25, Expression.evaluate("2 ^ -2"));
        assertEquals(2.5, Expression.evaluate("10 / 4"));
        assertEquals(1, Expression.evaluate("10 % 3"));
        assertEquals(3, Expression.evaluate("max(1, min(3, 5))"));
        assertEquals(4, Expression.evaluate("floor(4.9)"));
        assertEquals(5, Expression.evaluate("round(4.5)"));
        assertEquals(3, Expression.evaluate("sqrt(9)"));
    }

    @Test
    void skillXpFormulaIsHypixels() {
        // 3.5 * levels ^ 1.5, levels = XP levels spent
        String formula = "3.5 * %levels% ^ 1.5";
        assertEquals(3.5 * Math.pow(5, 1.5), eval(formula, Map.of("levels", "5")), 1e-9);
        assertEquals(39.131, eval(formula, Map.of("levels", "5")), 1e-3);
        assertEquals(110.680, eval(formula, Map.of("levels", "10")), 1e-3);
        assertEquals(575.108, eval(formula, Map.of("levels", "30")), 1e-3);
    }

    @Test
    void enchanterJobPayFormula() {
        // levels * enchanter job level * 0.5
        String formula = "%levels% * %job_level% * 0.5";
        assertEquals(50, eval(formula, Map.of("levels", "20", "job_level", "5")));
        assertEquals(0, eval(formula, Map.of("levels", "20", "job_level", "0")));
        assertEquals(7.5, eval("%levels% * 0.5", Map.of("levels", "15")));
    }

    @Test
    void badFormulasFailLoudly() {
        assertThrows(IllegalArgumentException.class, () -> Expression.evaluate(""));
        assertThrows(IllegalArgumentException.class, () -> Expression.evaluate("%levels% * 2"));
        assertThrows(IllegalArgumentException.class, () -> Expression.evaluate("2 +"));
        assertThrows(IllegalArgumentException.class, () -> Expression.evaluate("(2"));
        assertThrows(IllegalArgumentException.class, () -> Expression.evaluate("foo(2)"));
        assertThrows(IllegalArgumentException.class, () -> Expression.evaluate("min(2)"));
        assertThrows(IllegalArgumentException.class, () -> Expression.evaluate("1 / 0"));
    }

    @Test
    void numbersPrintCleanly() {
        assertEquals("39", Placeholders.number(39));
        assertEquals("39.13", Placeholders.number(39.131));
        assertEquals("-2", Placeholders.number(-2));
    }

    @Test
    void romanNumerals() {
        assertEquals("I", Roman.of(1));
        assertEquals("IV", Roman.of(4));
        assertEquals("V", Roman.of(5));
        assertEquals("IX", Roman.of(9));
        assertEquals("X", Roman.of(10));
        assertEquals("0", Roman.of(0));
    }
}
