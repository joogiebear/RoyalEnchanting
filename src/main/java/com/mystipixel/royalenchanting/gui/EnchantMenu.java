package com.mystipixel.royalenchanting.gui;

import com.mystipixel.royalenchanting.logic.SortMode;
import org.bukkit.block.Block;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * One player's open table. The item lives in {@code input}, never only in the inventory, and every way
 * the menu ends (close, quit, death, reload, shutdown) hands it back exactly once via {@link #takeInput()}.
 */
public final class EnchantMenu implements InventoryHolder {

    public enum View {
        LIST,
        LEVELS
    }

    private final UUID owner;
    private final Block table;
    private Inventory inventory;
    private ItemStack input;
    private View view = View.LIST;
    private String selected;
    private int page;
    private SortMode sort;
    // rebuilt on every render
    final Map<Integer, String> entrySlots = new HashMap<>();
    final Map<Integer, Integer> levelSlots = new HashMap<>();
    boolean hasNextPage;

    EnchantMenu(UUID owner, Block table, SortMode sort) {
        this.owner = owner;
        this.table = table;
        this.sort = sort;
    }

    void attach(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public UUID owner() {
        return owner;
    }

    public Block table() {
        return table;
    }

    public ItemStack input() {
        return input;
    }

    public boolean hasInput() {
        return input != null && !input.getType().isAir();
    }

    void setInput(ItemStack input) {
        this.input = input;
    }

    /** Removes and returns the item, so it can only ever be handed back once. */
    public ItemStack takeInput() {
        ItemStack out = input;
        input = null;
        return out;
    }

    public View view() {
        return view;
    }

    void showList() {
        this.view = View.LIST;
        this.selected = null;
    }

    void showLevels(String entryId) {
        this.view = View.LEVELS;
        this.selected = entryId;
    }

    public String selected() {
        return selected;
    }

    public int page() {
        return page;
    }

    void setPage(int page) {
        this.page = Math.max(0, page);
    }

    public SortMode sort() {
        return sort;
    }

    void setSort(SortMode sort) {
        this.sort = sort;
    }
}
