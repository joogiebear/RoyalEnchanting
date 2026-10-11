package com.mystipixel.royalenchanting.hook;

import me.clip.placeholderapi.PlaceholderAPI;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.logging.Logger;

/**
 * The optional integrations, detected on enable and on reload. Every accessor copes with the plugin
 * being absent; nothing here is required to open the table.
 */
public final class Hooks {

    private final Logger log;
    private EcoSkillsHook skills;
    private EcoJobsHook jobs;
    private EconGuardHook econGuard;
    private boolean papi;
    private boolean vault;

    public Hooks(Logger log) {
        this.log = log;
    }

    public void detect() {
        this.skills = EcoSkillsHook.create(log);
        this.jobs = EcoJobsHook.create(log);
        this.econGuard = EconGuardHook.create(log);
        this.papi = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
        this.vault = Bukkit.getPluginManager().isPluginEnabled("Vault");
        log.info("Hooks: EcoSkills " + state(skills != null) + ", EcoJobs " + state(jobs != null)
                + ", PlaceholderAPI " + state(papi) + ", Vault " + state(vault)
                + ", EconGuard " + state(econGuard != null)
                + ", EcoEnchants " + state(Bukkit.getPluginManager().isPluginEnabled("EcoEnchants")));
    }

    private static String state(boolean on) {
        return on ? "found" : "not found";
    }

    public EcoSkillsHook skills() {
        return skills;
    }

    public EcoJobsHook jobs() {
        return jobs;
    }

    public EconGuardHook econGuard() {
        return econGuard;
    }

    public boolean hasPlaceholderApi() {
        return papi;
    }

    /** Resolves PlaceholderAPI placeholders when it is installed; otherwise returns the text unchanged. */
    public String placeholders(Player player, String text) {
        if (!papi || text == null || text.indexOf('%') < 0) {
            return text;
        }
        try {
            return PlaceholderAPI.setPlaceholders(player, text);
        } catch (RuntimeException | LinkageError e) {
            return text;
        }
    }

    /** Pays through Vault. False when there is no economy or it refused. */
    public boolean deposit(OfflinePlayer player, double amount) {
        if (!vault) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        EconomyResponse response = rsp.getProvider().depositPlayer(player, amount);
        return response != null && response.transactionSuccess();
    }
}
