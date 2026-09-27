package com.mystipixel.royalenchanting.table;

import com.mystipixel.royalenchanting.config.Settings;
import com.mystipixel.royalenchanting.config.TableEntry;
import com.mystipixel.royalenchanting.hook.EcoSkillsHook;
import com.mystipixel.royalenchanting.hook.Hooks;
import com.mystipixel.royalenchanting.logic.BookshelfCounter;
import com.mystipixel.royalenchanting.logic.LevelCheck;
import com.mystipixel.royalenchanting.reward.RewardService;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.logging.Logger;

/**
 * The table's view of the world: which enchants go on an item, bookshelf power, and the player's skill,
 * XP and lapis.
 *
 * <p>Everything about enchants goes through the Bukkit API. EcoEnchants (2026.38, checked with javap)
 * registers each of its enchants in the server's enchantment registry as {@code minecraft:<id>}, as an
 * {@code EcoEnchantsCraftEnchantment} whose {@code getMaxLevel()} is the config's max-level,
 * {@code conflictsWith()} uses the config's conflicts, and {@code canEnchantItem()} checks the enchant's
 * targets plus type limits, required enchants and conflicts against what the item already carries.
 */
public final class TableService {

    /** One enchant the table can offer for the current item. */
    public record Option(TableEntry entry, Enchantment enchantment, int tableMax, int currentLevel) {
    }

    private final Logger log;
    private final Hooks hooks;
    private final Set<String> missingReported = new HashSet<>();

    public TableService(Logger log, Hooks hooks) {
        this.log = log;
        this.hooks = hooks;
    }

    public void clearReports() {
        missingReported.clear();
    }

    /** Looked up on every use: an EcoEnchants reload re-registers its enchants. */
    public Enchantment resolve(TableEntry entry) {
        Registry<Enchantment> registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT);
        Enchantment enchantment = registry.get(entry.key());
        if (enchantment == null && missingReported.add(entry.id())) {
            log.warning("enchants.yml: no enchantment is registered as " + entry.key()
                    + " (is EcoEnchants loaded and the id right?); '" + entry.id() + "' is not offered.");
        }
        return enchantment;
    }

    public static boolean isBook(ItemStack item) {
        Material type = item.getType();
        return type == Material.BOOK || type == Material.ENCHANTED_BOOK || type == Material.WRITABLE_BOOK
                || type == Material.WRITTEN_BOOK || type == Material.KNOWLEDGE_BOOK;
    }

    /** Every table enchant that can go on this kind of item, in config order. */
    public List<Option> options(Settings settings, ItemStack item) {
        List<Option> out = new ArrayList<>();
        if (item == null || item.getType().isAir() || isBook(item)) {
            return out;
        }
        ItemStack bare = stripped(item);
        for (TableEntry entry : settings.entries().values()) {
            if (Settings.ALWAYS_BLOCKED.contains(entry.key())) {
                continue;
            }
            Enchantment enchantment = resolve(entry);
            if (enchantment == null || !enchantment.canEnchantItem(bare)) {
                continue;
            }
            int tableMax = settings.costs().tableMax(enchantment.getMaxLevel(), entry.maxLevel());
            if (tableMax < 1) {
                continue;
            }
            out.add(new Option(entry, enchantment, tableMax, item.getEnchantmentLevel(enchantment)));
        }
        return out;
    }

    /** One of the item with every enchant taken off: "does this enchant fit this item at all". */
    private static ItemStack stripped(ItemStack item) {
        ItemStack bare = item.clone();
        bare.setAmount(1);
        for (Enchantment e : new ArrayList<>(bare.getEnchantments().keySet())) {
            bare.removeEnchantment(e);
        }
        return bare;
    }

    /** The item's enchants that applying {@code enchantment} would replace. */
    public static Map<Enchantment, Integer> conflicts(ItemStack item, Enchantment enchantment) {
        Map<Enchantment, Integer> out = new LinkedHashMap<>();
        for (Map.Entry<Enchantment, Integer> e : item.getEnchantments().entrySet()) {
            Enchantment other = e.getKey();
            if (!other.equals(enchantment)
                    && (enchantment.conflictsWith(other) || other.conflictsWith(enchantment))) {
                out.put(other, e.getValue());
            }
        }
        return out;
    }

    /**
     * The item with {@code enchantment} at {@code level}, conflicting enchants removed; null when the
     * enchant still can't go on (an EcoEnchants type limit, required enchant or enchant-count limit).
     */
    public static ItemStack applied(ItemStack item, Enchantment enchantment, int level) {
        ItemStack result = item.clone();
        for (Enchantment conflict : conflicts(item, enchantment).keySet()) {
            result.removeEnchantment(conflict);
        }
        result.removeEnchantment(enchantment);
        if (!enchantment.canEnchantItem(result)) {
            return null;
        }
        result.addUnsafeEnchantment(enchantment, level);
        return result;
    }

    public static ItemStack removed(ItemStack item, Enchantment enchantment) {
        ItemStack result = item.clone();
        result.removeEnchantment(enchantment);
        return result;
    }

    // ------------------------------------------------------------------ bookshelves

    public static int bookshelfPower(Block table, int maxPower) {
        return BookshelfCounter.power(new BookshelfCounter.Probe() {
            @Override
            public boolean isProvider(int dx, int dy, int dz) {
                return Tag.ENCHANTMENT_POWER_PROVIDER.isTagged(table.getRelative(dx, dy, dz).getType());
            }

            @Override
            public boolean isTransmitter(int dx, int dy, int dz) {
                return Tag.ENCHANTMENT_POWER_TRANSMITTER.isTagged(table.getRelative(dx, dy, dz).getType());
            }
        }, maxPower);
    }

    // ------------------------------------------------------------------ the player

    public LevelCheck.State state(Player player, Settings settings, int power) {
        return new LevelCheck.State(skillLevel(player, settings), power, player.getLevel(), lapis(player));
    }

    public int skillLevel(Player player, Settings settings) {
        EcoSkillsHook skills = hooks.skills();
        if (skills != null) {
            OptionalInt level = skills.level(player, settings.skillId());
            if (level.isPresent()) {
                return level.getAsInt();
            }
        }
        return RewardService.parseInt(hooks.placeholders(player, settings.skillPlaceholder())).orElse(0);
    }

    private static final ItemStack PLAIN_LAPIS = new ItemStack(Material.LAPIS_LAZULI);

    /** Plain lapis only: a custom item on a lapis base (enchanted lapis) is not spent as lapis. */
    private static boolean isLapis(ItemStack item) {
        return item != null && item.getType() == Material.LAPIS_LAZULI && item.isSimilar(PLAIN_LAPIS);
    }

    public static int lapis(Player player) {
        int count = 0;
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (isLapis(item)) {
                count += item.getAmount();
            }
        }
        return count;
    }

    /** Takes {@code amount} plain lapis; the caller has checked there is enough. */
    public static void takeLapis(Player player, int amount) {
        PlayerInventory inv = player.getInventory();
        ItemStack[] storage = inv.getStorageContents();
        int left = amount;
        for (int slot = 0; slot < storage.length && left > 0; slot++) {
            ItemStack item = storage[slot];
            if (!isLapis(item)) {
                continue;
            }
            int take = Math.min(left, item.getAmount());
            left -= take;
            if (take == item.getAmount()) {
                inv.setItem(slot, null);
            } else {
                item.setAmount(item.getAmount() - take);
                inv.setItem(slot, item);
            }
        }
    }
}
