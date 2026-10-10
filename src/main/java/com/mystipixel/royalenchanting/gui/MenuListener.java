package com.mystipixel.royalenchanting.gui;

import com.mystipixel.royalenchanting.RoyalEnchantingPlugin;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/** Opens the menu from enchanting tables, and keeps every click inside it under control. */
public final class MenuListener implements Listener {

    private final RoyalEnchantingPlugin plugin;
    private final MenuService menus;

    public MenuListener(RoyalEnchantingPlugin plugin, MenuService menus) {
        this.plugin = plugin;
        this.menus = menus;
    }

    // HIGH, so protection plugins have already denied the interaction where the player may not use this table.
    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.ENCHANTING_TABLE) {
            return;
        }
        if (event.useInteractedBlock() == Event.Result.DENY) {
            return;
        }
        Player player = event.getPlayer();
        // Sneaking with something in hand uses the item instead of the block, as in vanilla.
        if (player.isSneaking() && (!player.getInventory().getItemInMainHand().getType().isAir()
                || !player.getInventory().getItemInOffHand().getType().isAir())) {
            return;
        }
        event.setUseInteractedBlock(Event.Result.DENY);
        event.setUseItemInHand(Event.Result.DENY);
        if (event.getHand() != EquipmentSlot.HAND) {
            return; // the off-hand pass of the same click: the main hand already opened (or refused)
        }
        if (!player.hasPermission("royalenchanting.use")) {
            plugin.messages().send(player, "no-permission");
            return;
        }
        menus.open(player, block);
    }

    // a vanilla enchanting screen reached any other way would hand out Fortune
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onVanillaOpen(InventoryOpenEvent event) {
        if (event.getInventory().getType() == InventoryType.ENCHANTING && plugin.settings().blockVanillaTable()) {
            event.setCancelled(true);
        }
    }

    // HIGHEST: decides the final cancelled state, so no later listener can let vanilla move items across.
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder(false) instanceof EnchantMenu menu)) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) {
            event.setCancelled(true);
            return;
        }
        Inventory clicked = event.getClickedInventory();
        ClickType click = event.getClick();
        if (clicked != null && clicked.getHolder(false) instanceof EnchantMenu) {
            event.setCancelled(true);
            if (click == ClickType.LEFT || click == ClickType.RIGHT) {
                menus.clickTop(player, menu, event.getSlot(), false);
            } else if (click == ClickType.SHIFT_LEFT || click == ClickType.SHIFT_RIGHT) {
                menus.clickTop(player, menu, event.getSlot(), true);
            }
            return;
        }
        // Own inventory: shift-click moves one item in; double-click collect and hotbar swaps could pull
        // items across the boundary, so they are blocked.
        if (click == ClickType.SHIFT_LEFT || click == ClickType.SHIFT_RIGHT) {
            event.setCancelled(true);
            if (clicked != null) {
                menus.insertFromInventory(player, menu, clicked, event.getSlot());
            }
            return;
        }
        if (click == ClickType.DOUBLE_CLICK || click == ClickType.NUMBER_KEY || click == ClickType.SWAP_OFFHAND
                || clicked == null) {
            event.setCancelled(true);
        }
        // Plain clicks in the player's own inventory are allowed: that's how items reach the cursor.
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder(false) instanceof EnchantMenu)) {
            return;
        }
        int topSize = event.getView().getTopInventory().getSize();
        for (int raw : event.getRawSlots()) {
            if (raw < topSize) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder(false) instanceof EnchantMenu menu
                && event.getPlayer() instanceof Player player) {
            menus.finish(player, menu);
        }
    }

    // hand the item back before quit's own close, so it is in the inventory when player data is saved
    @EventHandler(priority = EventPriority.LOWEST)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        menus.menuOf(player).ifPresent(menu -> menus.finish(player, menu));
        menus.forget(player.getUniqueId());
    }

    // MONITOR, once keepInventory is final. Dropped directly rather than via the drop list, which later
    // listeners (graves, keep-inventory perks) may clear.
    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        ItemStack item = menus.takeForDeath(player);
        if (item == null || item.getType().isAir()) {
            return;
        }
        if (event.getKeepInventory()) {
            Map<Integer, ItemStack> left = player.getInventory().addItem(item);
            left.values().forEach(rest -> player.getWorld().dropItemNaturally(player.getLocation(), rest));
        } else {
            player.getWorld().dropItemNaturally(player.getLocation(), item);
        }
    }
}
