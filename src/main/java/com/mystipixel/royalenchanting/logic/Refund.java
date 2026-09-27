package com.mystipixel.royalenchanting.logic;

import java.util.Map;

/**
 * A chance to get part of an enchant's cost back, unlocked by Enchanting level. Stands in for EcoSkills'
 * Reimbursement (XP back) and Overcompensation (lapis back) perks, whose effects only hook the vanilla
 * table. {@code chance} is a percentage formula with {@code %skill%}, the player's Enchanting level.
 */
public record Refund(boolean enabled, int minSkill, String chance) {

    public static final Refund NONE = new Refund(false, 0, "0");

    /** The percentage chance (0-100) for a player at this Enchanting level; 0 below {@code minSkill}. */
    public double chanceFor(int skillLevel) {
        if (!enabled || skillLevel < minSkill) {
            return 0;
        }
        double value = Expression.evaluate(Placeholders.apply(chance, Map.of("skill", Integer.toString(skillLevel))));
        return Math.max(0, Math.min(100, value));
    }

    /** Whether a roll in [0, 100) lands inside this refund's chance. */
    public boolean hits(int skillLevel, double roll) {
        return roll < chanceFor(skillLevel);
    }
}
