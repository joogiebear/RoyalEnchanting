package com.mystipixel.royalenchanting.config;

import com.mystipixel.royalenchanting.logic.LevelCheck;
import org.bukkit.NamespacedKey;

/**
 * One enchant the table offers, from {@code enchants.yml}.
 *
 * @param id       the config key, e.g. {@code sharpness} or {@code harvesting}
 * @param key      the Bukkit registry key; EcoEnchants registers its enchants as {@code minecraft:<id>}
 * @param name     the name shown in the GUI
 * @param maxLevel a per-enchant cap below the enchant's own maximum, or 0 for none
 */
public record TableEntry(String id, NamespacedKey key, String name, LevelCheck.Requirement requirement,
                         int maxLevel) {
}
