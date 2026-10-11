package com.mystipixel.royalenchanting.reward;

import com.mystipixel.royalenchanting.config.Settings;
import com.mystipixel.royalenchanting.hook.EcoJobsHook;
import com.mystipixel.royalenchanting.hook.EcoSkillsHook;
import com.mystipixel.royalenchanting.hook.EconGuardHook;
import com.mystipixel.royalenchanting.hook.Hooks;
import com.mystipixel.royalenchanting.logic.Expression;
import com.mystipixel.royalenchanting.logic.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Pays out for an apply. EnchantItemEvent never fires for this table, so EcoSkills' and EcoJobs' own
 * {@code enchant_item} triggers don't either; this stands in for them.
 */
public final class RewardService {

    public record Enchanting(String action, String enchantId, String enchantName, int enchantLevel,
                             int levelsSpent, int lapisSpent) {
    }

    private static final java.util.regex.Pattern FIRST_INT = java.util.regex.Pattern.compile("-?\\d+");

    private final Logger log;
    private final Hooks hooks;
    private final Set<String> reported = new HashSet<>();

    public RewardService(Logger log, Hooks hooks) {
        this.log = log;
        this.hooks = hooks;
    }

    /**
     * Pays the skill and job rewards when {@code pays} is true, then runs the configured commands either
     * way. With {@code pays} false the %skill_xp%, %job_money% and %job_xp% placeholders are 0.
     */
    public void reward(Player player, Settings settings, Enchanting what, boolean pays) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("player", player.getName());
        values.put("uuid", player.getUniqueId().toString());
        values.put("action", what.action());
        values.put("enchant", what.enchantId());
        values.put("enchant_name", what.enchantName());
        values.put("enchant_level", Integer.toString(what.enchantLevel()));
        values.put("levels", Integer.toString(what.levelsSpent()));
        values.put("lapis", Integer.toString(what.lapisSpent()));
        values.put("job_level", Integer.toString(jobLevel(player, settings.jobReward()).orElse(0)));

        values.put("skill_xp", "0");
        Settings.SkillReward skill = settings.skillReward();
        if (pays && skill.enabled()) {
            Double xp = evaluate(player, "rewards.skill-xp.amount", skill.amount(), values);
            if (xp != null && xp > 0) {
                values.put("skill_xp", Placeholders.number(xp));
                EcoSkillsHook hook = hooks.skills();
                if (hook == null) {
                    reportOnce("skill", "EcoSkills is not available; Enchanting skill XP from the table is skipped.");
                } else {
                    hook.giveXp(player, skill.skill(), xp, skill.applyMultipliers());
                }
            }
        }

        values.put("job_money", "0");
        values.put("job_xp", "0");
        Settings.JobReward job = settings.jobReward();
        if (pays && job.enabled() && jobApplies(player, job)) {
            Double money = evaluate(player, "rewards.job.money", job.money(), values);
            if (money != null && money > 0) {
                values.put("job_money", Placeholders.number(money));
                if (!hooks.deposit(player, money)) {
                    reportOnce("vault", "No Vault economy accepted the enchanter job pay; it was not paid.");
                } else {
                    EconGuardHook econGuard = hooks.econGuard();
                    if (econGuard != null) {
                        econGuard.reportJobPay(player.getUniqueId(), player.getName(), money);
                    }
                }
            }
            Double xp = evaluate(player, "rewards.job.xp", job.xp(), values);
            if (xp != null && xp > 0) {
                values.put("job_xp", Placeholders.number(xp));
                EcoJobsHook hook = hooks.jobs();
                if (hook != null) {
                    hook.giveXp(player, job.job(), xp);
                }
            }
        }

        for (String command : settings.commands()) {
            String line = hooks.placeholders(player, Placeholders.apply(command, values));
            if (line != null && !line.isBlank()) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), line.startsWith("/") ? line.substring(1) : line);
            }
        }
    }

    // only players with the job active are paid, like EcoJobs' own triggers, unless configured otherwise
    private boolean jobApplies(Player player, Settings.JobReward job) {
        if (!job.requireActive()) {
            return true;
        }
        EcoJobsHook hook = hooks.jobs();
        if (hook == null) {
            reportOnce("jobs", "EcoJobs is not available, so whether the enchanter job is active can't be "
                    + "checked; job rewards from the table are skipped.");
            return false;
        }
        return hook.isActive(player, job.job()).orElse(false);
    }

    private OptionalInt jobLevel(Player player, Settings.JobReward job) {
        EcoJobsHook hook = hooks.jobs();
        if (hook != null) {
            OptionalInt level = hook.level(player, job.job());
            if (level.isPresent()) {
                return level;
            }
        }
        return parseInt(hooks.placeholders(player, job.levelPlaceholder()));
    }

    private Double evaluate(Player player, String path, String formula, Map<String, String> values) {
        String resolved = hooks.placeholders(player, Placeholders.apply(formula, values));
        try {
            return Expression.evaluate(resolved);
        } catch (IllegalArgumentException e) {
            reportOnce(path, "config.yml " + path + " could not be evaluated (" + e.getMessage()
                    + "); that reward is skipped.");
            return null;
        }
    }

    /** Parses a placeholder's output; an unresolved or non-numeric one is empty. */
    public static OptionalInt parseInt(String text) {
        if (text == null) {
            return OptionalInt.empty();
        }
        java.util.regex.Matcher m = FIRST_INT.matcher(text.replace(",", ""));
        if (!m.find()) {
            return OptionalInt.empty();
        }
        try {
            return OptionalInt.of(Integer.parseInt(m.group()));
        } catch (NumberFormatException e) {
            return OptionalInt.empty();
        }
    }

    private void reportOnce(String key, String message) {
        if (reported.add(key)) {
            log.warning(message);
        }
    }

    /** Called on reload so a fixed config gets its warnings again if it breaks later. */
    public void clearReports() {
        reported.clear();
    }
}
