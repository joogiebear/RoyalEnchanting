package com.mystipixel.royalenchanting.hook;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.OptionalInt;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * EcoSkills' public API, reached by reflection so this plugin neither compiles against nor requires the
 * shaded Kotlin jar. Signatures checked with javap against EcoSkills 2026.38:
 * <pre>
 *   com.willfp.ecoskills.skills.Skills.INSTANCE.getByID(String) -> Skill   (RegistrableCategory)
 *   com.willfp.ecoskills.api.EcoSkillsAPI.getSkillLevel(OfflinePlayer, Skill) -> int
 *   com.willfp.ecoskills.api.EcoSkillsAPI.gainSkillXP(OfflinePlayer, Skill, double)   (with multipliers)
 *   com.willfp.ecoskills.api.EcoSkillsAPI.giveSkillXP(OfflinePlayer, Skill, double)   (exact amount)
 * </pre>
 */
public final class EcoSkillsHook {

    private final Logger log;
    private final Object skillsCategory;
    private final Method getById;
    private final Method getSkillLevel;
    private final Method gainSkillXp;
    private final Method giveSkillXp;
    private boolean warned;

    private EcoSkillsHook(Logger log, Object skillsCategory, Method getById, Method getSkillLevel,
                          Method gainSkillXp, Method giveSkillXp) {
        this.log = log;
        this.skillsCategory = skillsCategory;
        this.getById = getById;
        this.getSkillLevel = getSkillLevel;
        this.gainSkillXp = gainSkillXp;
        this.giveSkillXp = giveSkillXp;
    }

    /** Null when EcoSkills is absent or its API has changed shape. */
    public static EcoSkillsHook create(Logger log) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("EcoSkills");
        if (plugin == null || !plugin.isEnabled()) {
            return null;
        }
        try {
            ClassLoader loader = plugin.getClass().getClassLoader();
            Class<?> skills = Class.forName("com.willfp.ecoskills.skills.Skills", true, loader);
            Class<?> skill = Class.forName("com.willfp.ecoskills.skills.Skill", true, loader);
            Class<?> api = Class.forName("com.willfp.ecoskills.api.EcoSkillsAPI", true, loader);
            Object instance = skills.getField("INSTANCE").get(null);
            return new EcoSkillsHook(log, instance,
                    skills.getMethod("getByID", String.class),
                    api.getMethod("getSkillLevel", OfflinePlayer.class, skill),
                    api.getMethod("gainSkillXP", OfflinePlayer.class, skill, double.class),
                    api.getMethod("giveSkillXP", OfflinePlayer.class, skill, double.class));
        } catch (ReflectiveOperationException | LinkageError e) {
            log.log(Level.WARNING, "EcoSkills is installed but its API could not be reached; "
                    + "falling back to PlaceholderAPI for skill levels and skipping skill XP rewards.", e);
            return null;
        }
    }

    private Object skill(String id) throws ReflectiveOperationException {
        return getById.invoke(skillsCategory, id);
    }

    /** Empty when the skill doesn't exist or the call failed. */
    public OptionalInt level(OfflinePlayer player, String skillId) {
        try {
            Object skill = skill(skillId);
            if (skill == null) {
                warnOnce("EcoSkills has no skill '" + skillId + "'.", null);
                return OptionalInt.empty();
            }
            return OptionalInt.of((Integer) getSkillLevel.invoke(null, player, skill));
        } catch (ReflectiveOperationException | RuntimeException e) {
            warnOnce("Reading an EcoSkills level failed.", e);
            return OptionalInt.empty();
        }
    }

    /** True when the XP was handed to EcoSkills. */
    public boolean giveXp(OfflinePlayer player, String skillId, double amount, boolean applyMultipliers) {
        try {
            Object skill = skill(skillId);
            if (skill == null) {
                warnOnce("EcoSkills has no skill '" + skillId + "'.", null);
                return false;
            }
            (applyMultipliers ? gainSkillXp : giveSkillXp).invoke(null, player, skill, amount);
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            warnOnce("Giving EcoSkills XP failed.", e);
            return false;
        }
    }

    private void warnOnce(String message, Throwable cause) {
        if (!warned) {
            warned = true;
            log.log(Level.WARNING, message + " (further EcoSkills errors are not logged)", cause);
        }
    }
}
