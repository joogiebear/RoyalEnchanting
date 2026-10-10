package com.mystipixel.royalenchanting;

import com.mystipixel.royalenchanting.command.RoyalEnchantingCommand;
import com.mystipixel.royalenchanting.config.Settings;
import com.mystipixel.royalenchanting.gui.MenuListener;
import com.mystipixel.royalenchanting.gui.MenuService;
import com.mystipixel.royalenchanting.hook.EnchantDescriptions;
import com.mystipixel.royalenchanting.hook.Hooks;
import com.mystipixel.royalenchanting.message.MessageManager;
import com.mystipixel.royalenchanting.reward.RewardService;
import com.mystipixel.royalenchanting.table.TableService;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class RoyalEnchantingPlugin extends JavaPlugin {

    private Settings settings;
    private MessageManager messages;
    private Hooks hooks;
    private TableService table;
    private RewardService rewards;
    private EnchantDescriptions descriptions;
    private MenuService menus;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (!new File(getDataFolder(), "enchants.yml").exists()) {
            saveResource("enchants.yml", false);
        }
        this.messages = new MessageManager(this);
        this.hooks = new Hooks(getLogger());
        this.table = new TableService(getLogger(), hooks);
        this.rewards = new RewardService(getLogger(), hooks);
        this.descriptions = new EnchantDescriptions();
        this.menus = new MenuService(this);
        loadSettings();
        hooks.detect();

        getServer().getPluginManager().registerEvents(new MenuListener(this, menus), this);
        PluginCommand command = getCommand("royalenchanting");
        if (command != null) {
            RoyalEnchantingCommand executor = new RoyalEnchantingCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }
        getLogger().info("RoyalEnchanting enabled with " + settings.entries().size() + " table enchants.");
    }

    @Override
    public void onDisable() {
        if (menus != null) {
            menus.closeAll();
        }
    }

    public void reload() {
        reloadConfig();
        messages.reload();
        loadSettings();
        hooks.detect();
        table.clearReports();
        rewards.clearReports();
        menus.refreshAll();
    }

    private void loadSettings() {
        YamlConfiguration enchants = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "enchants.yml"));
        this.settings = new Settings(getConfig(), enchants, getLogger());
    }

    public Settings settings() {
        return settings;
    }

    public MessageManager messages() {
        return messages;
    }

    public TableService table() {
        return table;
    }

    public RewardService rewards() {
        return rewards;
    }

    public EnchantDescriptions descriptions() {
        return descriptions;
    }
}
