package com.mystipixel.royalenchanting.config;

import com.mystipixel.royalenchanting.logic.CostTable;
import com.mystipixel.royalenchanting.logic.LevelCheck;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/** config.yml and enchants.yml, parsed once per load/reload. */
public final class Settings {

    /**
     * Never offered, whatever enchants.yml says: the server's stat enchants replace them and they must
     * not be obtainable from the table.
     */
    public static final Set<NamespacedKey> ALWAYS_BLOCKED = Set.of(
            NamespacedKey.minecraft("fortune"),
            NamespacedKey.minecraft("looting"),
            NamespacedKey.minecraft("luck_of_the_sea"));

    /** Levels the level view has room for (two rows of five). */
    public static final int MAX_LEVELS = 10;

    public record SkillReward(boolean enabled, String skill, String amount, boolean applyMultipliers) {
    }

    public record JobReward(boolean enabled, String job, boolean requireActive, String levelPlaceholder,
                            String money, String xp) {
    }

    public record Sounds(String success, String fail, String click) {
    }

    private final int maxPower;
    private final CostTable costs;
    private final String skillId;
    private final String skillPlaceholder;
    private final boolean blockVanillaTable;
    private final boolean showDescriptions;
    private final SkillReward skillReward;
    private final JobReward jobReward;
    private final List<String> commands;
    private final com.mystipixel.royalenchanting.logic.Refund xpRefund;
    private final com.mystipixel.royalenchanting.logic.Refund lapisRefund;
    private final Sounds sounds;
    private final Map<String, TableEntry> entries;

    public Settings(FileConfiguration config, YamlConfiguration enchants, Logger log) {
        this.maxPower = Math.max(0, config.getInt("bookshelf-power.max", 20));
        List<Integer> xp = config.getIntegerList("costs.xp-levels");
        List<Integer> lapis = config.getIntegerList("costs.lapis");
        if (xp.isEmpty()) {
            xp = List.of(5, 10, 15, 20, 30);
        }
        if (lapis.isEmpty()) {
            lapis = List.of(1, 2, 3, 4, 5);
        }
        if (xp.size() != lapis.size()) {
            log.warning("costs.xp-levels and costs.lapis have different lengths; the table offers "
                    + Math.min(xp.size(), lapis.size()) + " levels.");
        }
        if (xp.size() > MAX_LEVELS || lapis.size() > MAX_LEVELS) {
            log.warning("The table shows at most " + MAX_LEVELS + " levels; costs beyond that are ignored.");
            xp = xp.subList(0, Math.min(xp.size(), MAX_LEVELS));
            lapis = lapis.subList(0, Math.min(lapis.size(), MAX_LEVELS));
        }
        this.costs = new CostTable(xp, lapis);
        this.skillId = config.getString("skill.id", "enchanting");
        this.skillPlaceholder = config.getString("skill.placeholder", "%ecoskills_enchanting%");
        this.blockVanillaTable = config.getBoolean("block-vanilla-table", true);
        this.showDescriptions = config.getBoolean("gui.show-descriptions", true);
        this.skillReward = new SkillReward(
                config.getBoolean("rewards.skill-xp.enabled", true),
                config.getString("rewards.skill-xp.skill", "enchanting"),
                config.getString("rewards.skill-xp.amount", "3.5 * %levels% ^ 1.5"),
                config.getBoolean("rewards.skill-xp.apply-multipliers", true));
        this.jobReward = new JobReward(
                config.getBoolean("rewards.job.enabled", true),
                config.getString("rewards.job.id", "enchanter"),
                config.getBoolean("rewards.job.require-active", true),
                config.getString("rewards.job.level-placeholder", "%ecojobs_enchanter_level%"),
                config.getString("rewards.job.money", "%levels% * %job_level% * 0.5"),
                config.getString("rewards.job.xp", "%levels% * 0.5"));
        this.commands = List.copyOf(config.getStringList("rewards.commands"));
        this.xpRefund = refund(config, "refunds.xp", 10, "(%skill% - 9) * 0.5");
        this.lapisRefund = refund(config, "refunds.lapis", 20, "(%skill% - 19) * 0.75");
        this.sounds = new Sounds(
                config.getString("sounds.success", "block.enchantment_table.use"),
                config.getString("sounds.fail", "entity.villager.no"),
                config.getString("sounds.click", "ui.button.click"));

        Set<NamespacedKey> blocked = new HashSet<>(ALWAYS_BLOCKED);
        for (String id : config.getStringList("blocked-enchants")) {
            NamespacedKey key = key(id);
            if (key != null) {
                blocked.add(key);
            }
        }
        this.entries = Collections.unmodifiableMap(parseEntries(enchants, blocked, log));
    }

    private static Map<String, TableEntry> parseEntries(YamlConfiguration file, Set<NamespacedKey> blocked,
                                                        Logger log) {
        Map<String, TableEntry> out = new LinkedHashMap<>();
        ConfigurationSection section = file.getConfigurationSection("enchants");
        if (section == null) {
            log.warning("enchants.yml has no 'enchants' section; the table offers nothing.");
            return out;
        }
        for (String id : section.getKeys(false)) {
            ConfigurationSection e = section.getConfigurationSection(id);
            if (e == null || !e.getBoolean("enabled", true)) {
                continue;
            }
            NamespacedKey key = key(e.getString("key", id));
            if (key == null) {
                log.warning("enchants.yml: '" + id + "' is not a valid enchantment key; skipped.");
                continue;
            }
            if (blocked.contains(key)) {
                log.warning("enchants.yml: '" + id + "' is blocked from the table and is not offered.");
                continue;
            }
            LevelCheck.Requirement requirement = new LevelCheck.Requirement(
                    Math.max(0, e.getInt("skill-level", 0)),
                    Math.max(0, e.getInt("bookshelf-power", 0)));
            String name = e.getString("name", prettify(key.getKey()));
            out.put(id, new TableEntry(id, key, name, requirement, Math.max(0, e.getInt("max-level", 0))));
        }
        return out;
    }

    /** {@code harvesting} and {@code minecraft:harvesting} are the same key; anything else keeps its namespace. */
    static NamespacedKey key(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String s = raw.trim().toLowerCase(Locale.ROOT);
        try {
            return s.contains(":") ? NamespacedKey.fromString(s) : NamespacedKey.minecraft(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static String prettify(String id) {
        List<String> words = new ArrayList<>();
        for (String part : id.split("_")) {
            if (!part.isEmpty()) {
                words.add(Character.toUpperCase(part.charAt(0)) + part.substring(1));
            }
        }
        return String.join(" ", words);
    }

    public int maxPower() {
        return maxPower;
    }

    public CostTable costs() {
        return costs;
    }

    public String skillId() {
        return skillId;
    }

    public String skillPlaceholder() {
        return skillPlaceholder;
    }

    public boolean blockVanillaTable() {
        return blockVanillaTable;
    }

    public boolean showDescriptions() {
        return showDescriptions;
    }

    private static com.mystipixel.royalenchanting.logic.Refund refund(FileConfiguration config, String path,
                                                                      int minSkill, String chance) {
        if (!config.isConfigurationSection(path)) {
            return com.mystipixel.royalenchanting.logic.Refund.NONE;
        }
        return new com.mystipixel.royalenchanting.logic.Refund(
                config.getBoolean(path + ".enabled", true),
                config.getInt(path + ".min-skill", minSkill),
                config.getString(path + ".chance", chance));
    }

    public com.mystipixel.royalenchanting.logic.Refund xpRefund() {
        return xpRefund;
    }

    public com.mystipixel.royalenchanting.logic.Refund lapisRefund() {
        return lapisRefund;
    }

    public SkillReward skillReward() {
        return skillReward;
    }

    public JobReward jobReward() {
        return jobReward;
    }

    public List<String> commands() {
        return commands;
    }

    public Sounds sounds() {
        return sounds;
    }

    /** In config order, which is the GUI's default sort. */
    public Map<String, TableEntry> entries() {
        return entries;
    }
}
