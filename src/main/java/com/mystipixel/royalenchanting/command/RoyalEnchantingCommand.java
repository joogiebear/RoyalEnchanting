package com.mystipixel.royalenchanting.command;

import com.mystipixel.royalenchanting.RoyalEnchantingPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.util.List;

/** {@code /royalenchanting reload}. */
public final class RoyalEnchantingCommand implements TabExecutor {

    private final RoyalEnchantingPlugin plugin;

    public RoyalEnchantingCommand(RoyalEnchantingPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("royalenchanting.admin")) {
            plugin.messages().send(sender, "no-permission-admin");
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reload();
            plugin.messages().send(sender, "reloaded", "enchants", Integer.toString(plugin.settings().entries().size()));
            return true;
        }
        plugin.messages().send(sender, "usage", "label", label);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission("royalenchanting.admin") && "reload".startsWith(args[0].toLowerCase())) {
            return List.of("reload");
        }
        return List.of();
    }
}
