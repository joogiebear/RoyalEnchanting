package com.mystipixel.royalenchanting.logic;

import java.util.EnumSet;
import java.util.Set;

/** Decides what clicking one level of one enchant would do, and what stands in the way. */
public final class LevelCheck {

    /** What the enchant asks of the player before the table will touch it. */
    public record Requirement(int skillLevel, int bookshelfPower) {
    }

    /** The player and table, as they are right now. */
    public record State(int skillLevel, int bookshelfPower, int xpLevels, int lapis) {
    }

    public enum Action {
        /** Put this level on the item (replacing a lower level of the same enchant). */
        APPLY,
        /** The item already has exactly this level: clicking removes the enchant, for the same cost. */
        REMOVE,
        NONE
    }

    public enum Problem {
        SKILL,
        BOOKSHELF_POWER,
        XP,
        LAPIS,
        /** The item has a higher level of this enchant than the one clicked. */
        HIGHER_LEVEL,
        /** The item's level is above what the table offers (e.g. a VI from a book): hands off. */
        ABOVE_TABLE_MAX
    }

    public record Result(int level, Action action, int xpCost, int lapisCost, Set<Problem> problems) {
        public boolean allowed() {
            return action != Action.NONE && problems.isEmpty();
        }
    }

    private LevelCheck() {
    }

    /**
     * Whether a successful click pays the skill and job rewards. Only an apply above the highest level of
     * this enchant the item has already been paid for does, so removing and re-applying earns nothing.
     */
    public static boolean paysRewards(Action action, int level, int highestRewarded) {
        return action == Action.APPLY && level > highestRewarded;
    }

    public static boolean unlocked(Requirement requirement, State state) {
        return state.skillLevel() >= requirement.skillLevel()
                && state.bookshelfPower() >= requirement.bookshelfPower();
    }

    // level is 1..tableMax; currentLevel is 0 when the item lacks the enchant
    public static Result evaluate(int level, int currentLevel, int tableMax, CostTable costs,
                                  Requirement requirement, State state) {
        int xp = costs.xp(level);
        int lapis = costs.lapis(level);
        EnumSet<Problem> problems = EnumSet.noneOf(Problem.class);
        Action action;
        if (currentLevel > tableMax) {
            problems.add(Problem.ABOVE_TABLE_MAX);
            action = Action.NONE;
        } else if (level < currentLevel) {
            problems.add(Problem.HIGHER_LEVEL);
            action = Action.NONE;
        } else {
            action = level == currentLevel ? Action.REMOVE : Action.APPLY;
        }
        if (state.skillLevel() < requirement.skillLevel()) {
            problems.add(Problem.SKILL);
        }
        if (state.bookshelfPower() < requirement.bookshelfPower()) {
            problems.add(Problem.BOOKSHELF_POWER);
        }
        if (state.xpLevels() < xp) {
            problems.add(Problem.XP);
        }
        if (state.lapis() < lapis) {
            problems.add(Problem.LAPIS);
        }
        return new Result(level, action, xp, lapis, Set.copyOf(problems));
    }
}
