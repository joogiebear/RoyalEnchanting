package com.mystipixel.royalenchanting.hook;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * EconGuard's flat {@code EconGuard.record(...)} bridge, by reflection so there is no build-time
 * dependency, the same way RoyalBazaar and RoyalAuctions report. Reports never throw.
 */
public final class EconGuardHook {

    static final String SOURCE = "other";
    static final String ACTION_JOB_PAY = "enchanting_job";

    private final Logger log;
    private final Method record;
    private boolean warned;

    private EconGuardHook(Logger log, Method record) {
        this.log = log;
        this.record = record;
    }

    public static EconGuardHook create(Logger log) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("EconGuard");
        if (plugin == null || !plugin.isEnabled()) {
            return null;
        }
        try {
            return from(log, Class.forName("com.mystipixel.econguard.api.EconGuard", true,
                    plugin.getClass().getClassLoader()));
        } catch (ReflectiveOperationException | LinkageError e) {
            log.log(Level.WARNING, "EconGuard is installed but its record bridge could not be reached; "
                    + "enchanter job pay is not reported to it.", e);
            return null;
        }
    }

    static EconGuardHook from(Logger log, Class<?> econGuard) throws NoSuchMethodException {
        return new EconGuardHook(log, econGuard.getMethod("record",
                UUID.class, String.class, String.class, String.class,
                double.class, boolean.class, double.class,
                UUID.class, String.class, String.class, String.class));
    }

    /** Reports enchanter job money paid to the player. Called after the deposit went through. */
    public void reportJobPay(UUID player, String playerName, double amount) {
        try {
            record.invoke(null, player, playerName, SOURCE, ACTION_JOB_PAY, amount, true,
                    Double.NaN, null, null, null, null);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            if (!warned) {
                warned = true;
                log.log(Level.WARNING, "Reporting enchanter job pay to EconGuard failed; the pay itself went "
                        + "through. Further failures are not logged.", e);
            }
        }
    }
}
