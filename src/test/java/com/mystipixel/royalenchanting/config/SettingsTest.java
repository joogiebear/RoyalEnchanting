package com.mystipixel.royalenchanting.config;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The shipped config.yml and enchants.yml, parsed the way the plugin parses them. */
class SettingsTest {

    private static final Logger LOG = Logger.getLogger("SettingsTest");

    private static YamlConfiguration resource(String name) {
        var in = SettingsTest.class.getClassLoader().getResourceAsStream(name);
        assertNotNull(in, name);
        return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
    }

    private static Settings shipped() {
        return new Settings(resource("config.yml"), resource("enchants.yml"), LOG);
    }

    /** The approved unlock table: id -> {Enchanting level, Bookshelf Power}. */
    private static final Map<String, int[]> APPROVED = new java.util.LinkedHashMap<>();

    static {
        tier(0, 0, "harvesting", "prosperity", "sylvan_fortune", "swift_reel", "critical", "growth",
                "efficiency", "unbreaking", "sharpness", "power", "protection");
        tier(3, 4, "tremor", "sirens_call", "magic_seeker", "rooted", "stonebound", "barkskin", "tidebound");
        tier(5, 8, "green_thumb", "woodworker", "angler", "vicious", "sugar_rush", "smite", "bane_of_arthropods");
        tier(8, 12, "replenish", "timber", "arborist", "frail", "spiked_hook", "lifesteal", "scavenger",
                "infinite_quiver");
        tier(12, 16, "leafblower", "executioner", "giant_killer", "cleave", "snipe", "overload", "rejuvenate");
        tier(16, 20, "thunderlord", "smelting_touch");
    }

    private static void tier(int skill, int power, String... ids) {
        for (String id : ids) {
            APPROVED.put(id, new int[]{skill, power});
        }
    }

    @Test
    void shippedTableIsExactlyTheApprovedOne() {
        Map<String, TableEntry> entries = shipped().entries();
        assertEquals(List.copyOf(APPROVED.keySet()), List.copyOf(entries.keySet()));
        for (Map.Entry<String, int[]> e : APPROVED.entrySet()) {
            TableEntry entry = entries.get(e.getKey());
            assertEquals(e.getValue()[0], entry.requirement().skillLevel(), e.getKey());
            assertEquals(e.getValue()[1], entry.requirement().bookshelfPower(), e.getKey());
            assertEquals(NamespacedKey.minecraft(e.getKey()), entry.key());
            assertFalse(e.getKey().startsWith("turbo_"));
        }
        assertEquals("Siren's Call", entries.get("sirens_call").name());
    }

    @Test
    void shippedCostsAndRewards() {
        Settings s = shipped();
        assertEquals(20, s.maxPower());
        assertEquals(List.of(5, 10, 15, 20, 30), s.costs().xpLevels());
        assertEquals(List.of(0, 0, 0, 0, 0), s.costs().lapis());   // XP only, like Hypixel
        assertFalse(s.lapisRefund().enabled());
        assertEquals("enchanting", s.skillId());
        assertEquals("3.5 * %levels% ^ 1.5", s.skillReward().amount());
        assertEquals("enchanter", s.jobReward().job());
        assertEquals("%levels% * %job_level% * 0.5", s.jobReward().money());
        assertTrue(s.jobReward().requireActive());
    }

    @Test
    void fortuneLootingAndLuckOfTheSeaAreNeverOffered() {
        YamlConfiguration enchants = new YamlConfiguration();
        enchants.set("enchants.fortune.skill-level", 0);
        enchants.set("enchants.looting.skill-level", 0);
        enchants.set("enchants.luck_of_the_sea.skill-level", 0);
        enchants.set("enchants.sneaky.key", "minecraft:fortune");
        enchants.set("enchants.sharpness.skill-level", 0);
        enchants.set("enchants.silk_touch.skill-level", 0);
        YamlConfiguration config = resource("config.yml");
        config.set("blocked-enchants", List.of("silk_touch"));
        Map<String, TableEntry> entries = new Settings(config, enchants, LOG).entries();
        assertEquals(List.of("sharpness"), List.copyOf(entries.keySet()));
    }

    @Test
    void optionalFields() {
        YamlConfiguration enchants = new YamlConfiguration();
        enchants.set("enchants.sugar_rush.max-level", 2);
        enchants.set("enchants.off.enabled", false);
        enchants.set("enchants.other:thing.skill-level", 1);
        Map<String, TableEntry> entries = new Settings(resource("config.yml"), enchants, LOG).entries();
        assertEquals("Sugar Rush", entries.get("sugar_rush").name());
        assertEquals(2, entries.get("sugar_rush").maxLevel());
        assertFalse(entries.containsKey("off"));
        assertEquals(NamespacedKey.fromString("other:thing"), entries.get("other:thing").key());
    }
}
