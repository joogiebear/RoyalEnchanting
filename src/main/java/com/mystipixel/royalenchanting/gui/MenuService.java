package com.mystipixel.royalenchanting.gui;

import com.mystipixel.royalenchanting.RoyalEnchantingPlugin;
import com.mystipixel.royalenchanting.config.Settings;
import com.mystipixel.royalenchanting.config.TableEntry;
import com.mystipixel.royalenchanting.logic.LevelCheck;
import com.mystipixel.royalenchanting.logic.Roman;
import com.mystipixel.royalenchanting.logic.SortMode;
import com.mystipixel.royalenchanting.message.MessageManager;
import com.mystipixel.royalenchanting.reward.RewardService;
import com.mystipixel.royalenchanting.table.TableService;
import com.mystipixel.royalenchanting.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Opens, draws and drives the enchanting menu. */
public final class MenuService {

    static final int SIZE = 54;
    static final int INPUT = 19;
    static final int INFO = 28;
    static final int HEADER = 13;
    static final int EMPTY_NOTICE = 23;
    static final int PREVIOUS = 45;
    static final int BOOKSHELF = 48;
    static final int CLOSE = 49;
    static final int SORT = 50;
    static final int NEXT = 53;
    // the enchant list: three rows of five, right of the item slot
    static final int[] GRID = {12, 13, 14, 15, 16, 21, 22, 23, 24, 25, 30, 31, 32, 33, 34};
    static final int[] LEVEL_ROWS = {21, 22, 23, 24, 25, 30, 31, 32, 33, 34};

    private final RoyalEnchantingPlugin plugin;
    private final Map<UUID, EnchantMenu> open = new ConcurrentHashMap<>();
    private final Map<UUID, SortMode> sortChoice = new HashMap<>();

    public MenuService(RoyalEnchantingPlugin plugin) {
        this.plugin = plugin;
    }

    private Settings settings() {
        return plugin.settings();
    }

    private MessageManager messages() {
        return plugin.messages();
    }

    private TableService table() {
        return plugin.table();
    }

    public Collection<EnchantMenu> openMenus() {
        return List.copyOf(open.values());
    }

    public Optional<EnchantMenu> menuOf(Player player) {
        return Optional.ofNullable(open.get(player.getUniqueId()));
    }

    public void open(Player player, Block tableBlock) {
        EnchantMenu previous = open.get(player.getUniqueId());
        if (previous != null) {
            finish(player, previous);
        }
        EnchantMenu menu = new EnchantMenu(player.getUniqueId(), tableBlock,
                sortChoice.getOrDefault(player.getUniqueId(), SortMode.DEFAULT));
        Inventory inventory = Bukkit.createInventory(menu, SIZE, Text.chat(messages().get("gui.title")));
        menu.attach(inventory);
        render(player, menu);
        open.put(player.getUniqueId(), menu);
        player.openInventory(inventory);
    }

    /** Gives the item back (at the player's feet when full or dead). Safe to call more than once. */
    public void finish(Player player, EnchantMenu menu) {
        open.remove(menu.owner(), menu);
        ItemStack item = menu.takeInput();
        if (item == null || item.getType().isAir()) {
            return;
        }
        if (player.isDead()) {
            player.getWorld().dropItemNaturally(player.getLocation(), item);
            return;
        }
        give(player, item);
    }

    /** Ends the menu for a death; the caller drops the item (or keeps it, under keepInventory). */
    public ItemStack takeForDeath(Player player) {
        EnchantMenu menu = open.remove(player.getUniqueId());
        return menu == null ? null : menu.takeInput();
    }

    private void give(Player player, ItemStack item) {
        Map<Integer, ItemStack> left = player.getInventory().addItem(item);
        if (!left.isEmpty()) {
            for (ItemStack rest : left.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), rest);
            }
            messages().send(player, "item-dropped");
        }
    }

    /** Hands every item back and closes every menu: used on disable. */
    public void closeAll() {
        for (EnchantMenu menu : openMenus()) {
            Player player = Bukkit.getPlayer(menu.owner());
            if (player != null) {
                finish(player, menu);
                player.closeInventory();
            }
        }
        open.clear();
    }

    public void refreshAll() {
        for (EnchantMenu menu : openMenus()) {
            Player player = Bukkit.getPlayer(menu.owner());
            if (player != null) {
                if (menu.selected() != null && !settings().entries().containsKey(menu.selected())) {
                    menu.showList();
                }
                render(player, menu);
            }
        }
    }

    public void forget(UUID player) {
        sortChoice.remove(player);
    }

    void clickTop(Player player, EnchantMenu menu, int slot, boolean shift) {
        if (slot == INPUT) {
            clickInput(player, menu, shift);
            return;
        }
        if (slot == CLOSE) {
            Bukkit.getScheduler().runTask(plugin, () -> player.closeInventory());
            return;
        }
        if (menu.view() == EnchantMenu.View.LIST) {
            if (slot == SORT) {
                SortMode next = menu.sort().next();
                menu.setSort(next);
                sortChoice.put(player.getUniqueId(), next);
                menu.setPage(0);
                click(player);
                render(player, menu);
            } else if (slot == PREVIOUS && menu.page() > 0) {
                menu.setPage(menu.page() - 1);
                click(player);
                render(player, menu);
            } else if (slot == NEXT && menu.hasNextPage) {
                menu.setPage(menu.page() + 1);
                click(player);
                render(player, menu);
            } else if (menu.entrySlots.containsKey(slot)) {
                menu.showLevels(menu.entrySlots.get(slot));
                click(player);
                render(player, menu);
            }
        } else {
            if (slot == PREVIOUS) {
                menu.showList();
                click(player);
                render(player, menu);
            } else if (menu.levelSlots.containsKey(slot)) {
                enchant(player, menu, menu.levelSlots.get(slot));
            }
        }
    }

    private void clickInput(Player player, EnchantMenu menu, boolean shift) {
        ItemStack cursor = player.getItemOnCursor();
        boolean cursorEmpty = cursor.getType().isAir();
        if (menu.hasInput()) {
            if (shift) {
                give(player, menu.takeInput());
            } else if (cursorEmpty) {
                player.setItemOnCursor(menu.takeInput());
            } else {
                fail(player);
                return;
            }
        } else if (!cursorEmpty) {
            if (TableService.isBook(cursor)) {
                messages().send(player, "books-not-allowed");
                fail(player);
                return;
            }
            ItemStack one = cursor.clone();
            one.setAmount(1);
            ItemStack rest = null;
            if (cursor.getAmount() > 1) {
                rest = cursor.clone();
                rest.setAmount(cursor.getAmount() - 1);
            }
            menu.setInput(one);
            player.setItemOnCursor(rest);
        } else {
            return;
        }
        menu.showList();
        menu.setPage(0);
        render(player, menu);
        resync(player);
    }

    void insertFromInventory(Player player, EnchantMenu menu, Inventory from, int slot) {
        ItemStack item = from.getItem(slot);
        if (item == null || item.getType().isAir()) {
            return;
        }
        if (menu.hasInput()) {
            fail(player);
            return;
        }
        if (TableService.isBook(item)) {
            messages().send(player, "books-not-allowed");
            fail(player);
            return;
        }
        ItemStack one = item.clone();
        one.setAmount(1);
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
            from.setItem(slot, item);
        } else {
            from.setItem(slot, null);
        }
        menu.setInput(one);
        menu.showList();
        menu.setPage(0);
        render(player, menu);
        resync(player);
    }

    // next tick, once the cancelled click has been answered; never moves items itself
    private void resync(Player player) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                player.updateInventory();
            }
        });
    }

    private void enchant(Player player, EnchantMenu menu, int level) {
        Settings settings = settings();
        TableEntry entry = settings.entries().get(menu.selected());
        if (!menu.hasInput() || entry == null) {
            menu.showList();
            render(player, menu);
            return;
        }
        TableService.Option option = findOption(settings, menu.input(), entry.id());
        if (option == null) {
            messages().send(player, "fail-not-applicable");
            fail(player);
            menu.showList();
            render(player, menu);
            return;
        }
        int power = TableService.bookshelfPower(menu.table(), settings.maxPower());
        LevelCheck.State state = table().state(player, settings, power);
        LevelCheck.Result result = LevelCheck.evaluate(level, option.currentLevel(), option.tableMax(),
                settings.costs(), entry.requirement(), state);
        if (!result.allowed()) {
            sendProblem(player, entry, result);
            fail(player);
            render(player, menu);
            return;
        }
        Enchantment enchantment = option.enchantment();
        ItemStack changed = result.action() == LevelCheck.Action.APPLY
                ? TableService.applied(menu.input(), enchantment, level)
                : TableService.removed(menu.input(), enchantment);
        if (changed == null) {
            messages().send(player, "fail-incompatible");
            fail(player);
            render(player, menu);
            return;
        }

        player.giveExpLevels(-result.xpCost());
        TableService.takeLapis(player, result.lapisCost());
        menu.setInput(changed);
        refund(player, settings, state.skillLevel(), result.xpCost(), result.lapisCost());

        boolean apply = result.action() == LevelCheck.Action.APPLY;
        String shown = entry.name() + " " + Roman.of(level);
        messages().send(player, apply ? "applied" : "removed", "enchant", shown,
                "xp", Integer.toString(result.xpCost()), "lapis", Integer.toString(result.lapisCost()));
        sound(player, settings.sounds().success());
        plugin.rewards().reward(player, settings, new RewardService.Enchanting(apply ? "apply" : "remove",
                entry.id(), entry.name(), level, result.xpCost(), result.lapisCost()));
        render(player, menu);
    }

    // Reimbursement and Overcompensation perks: a chance, by Enchanting level, to get the cost back
    private void refund(Player player, Settings settings, int skillLevel, int xp, int lapis) {
        var random = java.util.concurrent.ThreadLocalRandom.current();
        try {
            if (xp > 0 && settings.xpRefund().hits(skillLevel, random.nextDouble(100))) {
                player.giveExpLevels(xp);
                messages().send(player, "refund-xp", "xp", Integer.toString(xp));
            }
            if (lapis > 0 && settings.lapisRefund().hits(skillLevel, random.nextDouble(100))) {
                var left = player.getInventory().addItem(new ItemStack(org.bukkit.Material.LAPIS_LAZULI, lapis));
                left.values().forEach(stack -> player.getWorld().dropItemNaturally(player.getLocation(), stack));
                messages().send(player, "refund-lapis", "lapis", Integer.toString(lapis));
            }
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Bad refund chance formula: " + e.getMessage());
        }
    }

    private void sendProblem(Player player, TableEntry entry, LevelCheck.Result result) {
        var problems = result.problems();
        var cost = plugin.settings().costs();
        if (problems.contains(LevelCheck.Problem.ABOVE_TABLE_MAX)) {
            messages().send(player, "fail-above-max");
        } else if (problems.contains(LevelCheck.Problem.HIGHER_LEVEL)) {
            messages().send(player, "fail-higher-level");
        } else if (problems.contains(LevelCheck.Problem.SKILL)) {
            messages().send(player, "fail-skill", "required", Integer.toString(entry.requirement().skillLevel()));
        } else if (problems.contains(LevelCheck.Problem.BOOKSHELF_POWER)) {
            messages().send(player, "fail-bookshelf-power", "required",
                    Integer.toString(entry.requirement().bookshelfPower()));
        } else if (problems.contains(LevelCheck.Problem.XP)) {
            messages().send(player, "fail-xp", "required", Integer.toString(cost.xp(result.level())));
        } else if (problems.contains(LevelCheck.Problem.LAPIS)) {
            messages().send(player, "fail-lapis", "required", Integer.toString(cost.lapis(result.level())));
        }
    }

    private TableService.Option findOption(Settings settings, ItemStack item, String id) {
        for (TableService.Option option : table().options(settings, item)) {
            if (option.entry().id().equals(id)) {
                return option;
            }
        }
        return null;
    }

    void render(Player player, EnchantMenu menu) {
        if (menu.table().getType() != Material.ENCHANTING_TABLE) {
            // The table was broken under an open menu: end it (the close hands the item back).
            Bukkit.getScheduler().runTask(plugin, () -> player.closeInventory());
            return;
        }
        Settings settings = settings();
        ItemStack[] contents = new ItemStack[SIZE];
        ItemStack filler = filler();
        for (int i = 0; i < SIZE; i++) {
            contents[i] = filler;
        }
        menu.entrySlots.clear();
        menu.levelSlots.clear();
        menu.hasNextPage = false;
        contents[INPUT] = menu.hasInput() ? menu.input().clone() : null;

        int power = TableService.bookshelfPower(menu.table(), settings.maxPower());
        LevelCheck.State state = table().state(player, settings, power);
        contents[BOOKSHELF] = button(Material.BOOKSHELF, "gui.bookshelf",
                "power", Integer.toString(power), "max", Integer.toString(settings.maxPower()),
                "skill", Integer.toString(state.skillLevel()));
        contents[CLOSE] = button(Material.BARRIER, "gui.close");

        List<TableService.Option> options = menu.hasInput() ? table().options(settings, menu.input()) : List.of();
        if (menu.view() == EnchantMenu.View.LEVELS) {
            TableService.Option option = null;
            for (TableService.Option o : options) {
                if (o.entry().id().equals(menu.selected())) {
                    option = o;
                }
            }
            if (option == null) {
                menu.showList();
            } else {
                drawLevels(player, menu, contents, option, state);
            }
        }
        if (menu.view() == EnchantMenu.View.LIST) {
            drawList(player, menu, contents, options, state);
        }
        menu.getInventory().setContents(contents);
    }

    private void drawList(Player player, EnchantMenu menu, ItemStack[] contents, List<TableService.Option> options,
                          LevelCheck.State state) {
        contents[INFO] = button(Material.ENCHANTING_TABLE, "gui.enchant-item");
        contents[SORT] = sortButton(menu.sort());
        if (!menu.hasInput()) {
            contents[EMPTY_NOTICE] = button(Material.GRAY_DYE, "gui.no-item");
            return;
        }
        if (options.isEmpty()) {
            contents[EMPTY_NOTICE] = button(Material.GRAY_DYE, "gui.no-enchants");
            return;
        }
        List<TableService.Option> sorted = menu.sort().sort(options, o -> o.entry().name(), o -> o.currentLevel() > 0);
        int pages = (sorted.size() + GRID.length - 1) / GRID.length;
        if (menu.page() >= pages) {
            menu.setPage(pages - 1);
        }
        int from = menu.page() * GRID.length;
        for (int i = 0; i < GRID.length && from + i < sorted.size(); i++) {
            TableService.Option option = sorted.get(from + i);
            contents[GRID[i]] = enchantIcon(player, menu.input(), option, state, false);
            menu.entrySlots.put(GRID[i], option.entry().id());
        }
        if (menu.page() > 0) {
            contents[PREVIOUS] = button(Material.ARROW, "gui.previous-page", "page", Integer.toString(menu.page()));
        }
        if (menu.page() + 1 < pages) {
            contents[NEXT] = button(Material.ARROW, "gui.next-page", "page", Integer.toString(menu.page() + 2));
            menu.hasNextPage = true;
        }
    }

    private void drawLevels(Player player, EnchantMenu menu, ItemStack[] contents, TableService.Option option,
                            LevelCheck.State state) {
        Settings settings = settings();
        contents[HEADER] = enchantIcon(player, menu.input(), option, state, true);
        contents[PREVIOUS] = button(Material.ARROW, "gui.back");
        int[] slots = levelSlots(option.tableMax());
        Map<Enchantment, Integer> replaces = TableService.conflicts(menu.input(), option.enchantment());
        for (int i = 0; i < slots.length; i++) {
            int level = i + 1;
            LevelCheck.Result result = LevelCheck.evaluate(level, option.currentLevel(), option.tableMax(),
                    settings.costs(), option.entry().requirement(), state);
            contents[slots[i]] = levelIcon(player, option, result, state, replaces);
            menu.levelSlots.put(slots[i], level);
        }
    }

    // up to five levels sit centred on the middle row; six to ten fill it and the row below
    static int[] levelSlots(int levels) {
        int n = Math.max(0, Math.min(levels, LEVEL_ROWS.length));
        int[] out = new int[n];
        if (n <= 5) {
            int start = 23 - n / 2;
            for (int i = 0; i < n; i++) {
                out[i] = start + i;
            }
        } else {
            System.arraycopy(LEVEL_ROWS, 0, out, 0, n);
        }
        return out;
    }

    private ItemStack enchantIcon(Player player, ItemStack input, TableService.Option option, LevelCheck.State state,
                                  boolean header) {
        MessageManager m = messages();
        TableEntry entry = option.entry();
        boolean unlocked = LevelCheck.unlocked(entry.requirement(), state);
        String name = m.get(unlocked ? "gui.enchant.name" : "gui.enchant.name-locked", "enchant", entry.name());
        List<String> lore = new ArrayList<>(description(player, option,
                Math.max(1, Math.min(option.currentLevel(), option.tableMax()))));
        if (!lore.isEmpty()) {
            lore.add("");
        }
        if (option.currentLevel() > 0) {
            lore.add(m.get("gui.enchant.current", "enchant", entry.name(), "level", Roman.of(option.currentLevel())));
        } else {
            lore.add(m.get("gui.enchant.current-none"));
        }
        lore.add(m.get("gui.enchant.max-level", "max", Roman.of(option.tableMax())));
        for (Map.Entry<Enchantment, Integer> conflict : TableService.conflicts(input, option.enchantment()).entrySet()) {
            lore.add(m.get("gui.enchant.replaces", "enchant", enchantName(conflict.getKey()),
                    "level", Roman.of(conflict.getValue())));
        }
        addRequirements(lore, entry, state);
        if (!header) {
            lore.add("");
            lore.add(m.get(unlocked ? "gui.enchant.click" : "gui.enchant.click-locked"));
        }
        ItemStack icon = icon(unlocked ? Material.ENCHANTED_BOOK : Material.BOOK, name, lore);
        glint(icon, option.currentLevel() > 0);
        return icon;
    }

    private ItemStack levelIcon(Player player, TableService.Option option, LevelCheck.Result result,
                                LevelCheck.State state, Map<Enchantment, Integer> replaces) {
        MessageManager m = messages();
        TableEntry entry = option.entry();
        String numeral = Roman.of(result.level());
        String nameKey = result.allowed() ? (result.action() == LevelCheck.Action.REMOVE
                ? "gui.level.name-remove" : "gui.level.name") : "gui.level.name-unavailable";
        String name = m.get(nameKey, "enchant", entry.name(), "level", numeral);
        List<String> lore = new ArrayList<>(description(player, option, result.level()));
        if (!lore.isEmpty()) {
            lore.add("");
        }
        lore.addAll(m.list("gui.level.cost", "xp", Integer.toString(result.xpCost()),
                "lapis", Integer.toString(result.lapisCost())));
        if (result.action() == LevelCheck.Action.APPLY) {
            for (Map.Entry<Enchantment, Integer> conflict : replaces.entrySet()) {
                lore.add(m.get("gui.enchant.replaces", "enchant", enchantName(conflict.getKey()),
                        "level", Roman.of(conflict.getValue())));
            }
        }
        addRequirements(lore, entry, state);
        lore.add("");
        lore.add(m.get(statusKey(result)));
        ItemStack icon = icon(result.allowed() ? Material.ENCHANTED_BOOK : Material.BOOK, name, lore);
        glint(icon, result.level() == option.currentLevel());
        return icon;
    }

    private static String statusKey(LevelCheck.Result result) {
        var problems = result.problems();
        if (problems.contains(LevelCheck.Problem.ABOVE_TABLE_MAX)) {
            return "gui.level.status-above-max";
        }
        if (problems.contains(LevelCheck.Problem.HIGHER_LEVEL)) {
            return "gui.level.status-higher-level";
        }
        if (problems.contains(LevelCheck.Problem.SKILL) || problems.contains(LevelCheck.Problem.BOOKSHELF_POWER)) {
            return "gui.level.status-locked";
        }
        if (problems.contains(LevelCheck.Problem.XP)) {
            return "gui.level.status-no-xp";
        }
        if (problems.contains(LevelCheck.Problem.LAPIS)) {
            return "gui.level.status-no-lapis";
        }
        return result.action() == LevelCheck.Action.REMOVE ? "gui.level.status-remove" : "gui.level.status-apply";
    }

    private void addRequirements(List<String> lore, TableEntry entry, LevelCheck.State state) {
        MessageManager m = messages();
        int skill = entry.requirement().skillLevel();
        int power = entry.requirement().bookshelfPower();
        if (skill <= 0 && power <= 0) {
            return;
        }
        lore.add("");
        lore.add(m.get("gui.requirements.header"));
        if (skill > 0) {
            lore.add(m.get(state.skillLevel() >= skill ? "gui.requirements.skill-met" : "gui.requirements.skill-missing",
                    "required", Integer.toString(skill), "current", Integer.toString(state.skillLevel())));
        }
        if (power > 0) {
            lore.add(m.get(state.bookshelfPower() >= power ? "gui.requirements.power-met" : "gui.requirements.power-missing",
                    "required", Integer.toString(power), "current", Integer.toString(state.bookshelfPower())));
        }
    }

    private List<String> description(Player player, TableService.Option option, int level) {
        if (!settings().showDescriptions()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (String line : plugin.descriptions().describe(option.enchantment(), level, player)) {
            out.add(messages().get("gui.enchant.description-line", "line", line));
        }
        return out;
    }

    // a conflicting enchant's name: from enchants.yml when it's a table enchant, else from its id
    private String enchantName(Enchantment enchantment) {
        for (TableEntry entry : settings().entries().values()) {
            if (entry.key().equals(enchantment.getKey())) {
                return entry.name();
            }
        }
        // description() is a translatable component; its plain form would be the translation key.
        return Settings.prettify(enchantment.getKey().getKey());
    }

    private ItemStack sortButton(SortMode current) {
        MessageManager m = messages();
        List<String> lore = new ArrayList<>(m.list("gui.sort.lore"));
        for (SortMode mode : SortMode.values()) {
            String label = m.get("gui.sort.modes." + mode.name().toLowerCase(java.util.Locale.ROOT));
            lore.add(m.get(mode == current ? "gui.sort.selected" : "gui.sort.unselected", "mode", label));
        }
        lore.add("");
        lore.add(m.get("gui.sort.click"));
        return icon(Material.HOPPER, m.get("gui.sort.name"), lore);
    }

    private ItemStack button(Material material, String key, String... replacements) {
        return icon(material, messages().get(key + ".name", replacements), messages().list(key + ".lore", replacements));
    }

    private static ItemStack icon(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Text.item(name));
        meta.lore(Text.items(lore));
        meta.addItemFlags(ItemFlag.values());
        item.setItemMeta(meta);
        return item;
    }

    private static void glint(ItemStack item, boolean on) {
        ItemMeta meta = item.getItemMeta();
        meta.setEnchantmentGlintOverride(on);
        item.setItemMeta(meta);
    }

    private ItemStack filler() {
        ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setHideTooltip(true);
        item.setItemMeta(meta);
        return item;
    }

    private void click(Player player) {
        sound(player, settings().sounds().click());
    }

    private void fail(Player player) {
        sound(player, settings().sounds().fail());
    }

    private static void sound(Player player, String key) {
        if (key != null && !key.isBlank()) {
            player.playSound(player.getLocation(), key, 1f, 1f);
        }
    }
}
