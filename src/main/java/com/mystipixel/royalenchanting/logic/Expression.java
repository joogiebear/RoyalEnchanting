package com.mystipixel.royalenchanting.logic;

import java.util.Locale;

/**
 * A small arithmetic evaluator for the reward formulas: numbers, + - * / % ^ (power, right-associative),
 * parentheses, unary minus and the functions min, max, floor, ceil, round, sqrt, abs. Placeholders are
 * substituted before evaluation, so by the time a formula gets here it is plain arithmetic.
 */
public final class Expression {

    private final String src;
    private int pos;

    private Expression(String src) {
        this.src = src;
    }

    public static double evaluate(String expression) {
        if (expression == null || expression.isBlank()) {
            throw new IllegalArgumentException("Empty expression");
        }
        Expression parser = new Expression(expression);
        double value = parser.parseSum();
        parser.skipSpaces();
        if (parser.pos < parser.src.length()) {
            throw parser.error("Unexpected '" + parser.src.charAt(parser.pos) + "'");
        }
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException("Expression is not a finite number: " + expression);
        }
        return value;
    }

    private double parseSum() {
        double value = parseProduct();
        while (true) {
            if (eat('+')) {
                value += parseProduct();
            } else if (eat('-')) {
                value -= parseProduct();
            } else {
                return value;
            }
        }
    }

    private double parseProduct() {
        double value = parseUnary();
        while (true) {
            if (eat('*')) {
                value *= parseUnary();
            } else if (eat('/')) {
                value /= parseUnary();
            } else if (eat('%')) {
                value %= parseUnary();
            } else {
                return value;
            }
        }
    }

    // Unary minus binds looser than ^, as in maths: -2 ^ 2 is -4.
    private double parseUnary() {
        if (eat('-')) {
            return -parseUnary();
        }
        if (eat('+')) {
            return parseUnary();
        }
        return parsePower();
    }

    private double parsePower() {
        double base = parseAtom();
        if (eat('^')) {
            return Math.pow(base, parseUnary()); // right-associative: 2 ^ 3 ^ 2 is 2 ^ 9
        }
        return base;
    }

    private double parseAtom() {
        skipSpaces();
        if (eat('(')) {
            double value = parseSum();
            expect(')');
            return value;
        }
        int start = pos;
        if (pos < src.length() && (Character.isDigit(src.charAt(pos)) || src.charAt(pos) == '.')) {
            while (pos < src.length() && (Character.isDigit(src.charAt(pos)) || src.charAt(pos) == '.')) {
                pos++;
            }
            try {
                return Double.parseDouble(src.substring(start, pos));
            } catch (NumberFormatException e) {
                throw error("Bad number '" + src.substring(start, pos) + "'");
            }
        }
        if (pos < src.length() && Character.isLetter(src.charAt(pos))) {
            while (pos < src.length() && Character.isLetter(src.charAt(pos))) {
                pos++;
            }
            String name = src.substring(start, pos).toLowerCase(Locale.ROOT);
            expect('(');
            double a = parseSum();
            boolean two = eat(',');
            double b = two ? parseSum() : 0;
            expect(')');
            return function(name, a, b, two);
        }
        throw error(pos < src.length() ? "Unexpected '" + src.charAt(pos) + "'" : "Unexpected end");
    }

    private double function(String name, double a, double b, boolean two) {
        return switch (name) {
            case "min" -> arity(name, two, true, Math.min(a, b));
            case "max" -> arity(name, two, true, Math.max(a, b));
            case "floor" -> arity(name, two, false, Math.floor(a));
            case "ceil" -> arity(name, two, false, Math.ceil(a));
            case "round" -> arity(name, two, false, Math.round(a));
            case "sqrt" -> arity(name, two, false, Math.sqrt(a));
            case "abs" -> arity(name, two, false, Math.abs(a));
            default -> throw error("Unknown function '" + name + "'");
        };
    }

    private double arity(String name, boolean got, boolean wanted, double value) {
        if (got != wanted) {
            throw error(name + "() takes " + (wanted ? "two arguments" : "one argument"));
        }
        return value;
    }

    private boolean eat(char c) {
        skipSpaces();
        if (pos < src.length() && src.charAt(pos) == c) {
            pos++;
            return true;
        }
        return false;
    }

    private void expect(char c) {
        if (!eat(c)) {
            throw error("Expected '" + c + "'");
        }
    }

    private void skipSpaces() {
        while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) {
            pos++;
        }
    }

    private IllegalArgumentException error(String message) {
        return new IllegalArgumentException(message + " at position " + pos + " in \"" + src + "\"");
    }
}
