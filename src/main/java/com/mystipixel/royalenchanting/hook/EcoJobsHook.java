package com.mystipixel.royalenchanting.hook;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.OptionalInt;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * EcoJobs' public API, by reflection (see {@link EcoSkillsHook} for why). Signatures checked with javap
 * against EcoJobs 2026.38:
 * <pre>
 *   com.willfp.ecojobs.jobs.Jobs.getByID(String) -> Job                       (static)
 *   com.willfp.ecojobs.api.EcoJobsAPI.hasJobActive(OfflinePlayer, Job) -> boolean
 *   com.willfp.ecojobs.api.EcoJobsAPI.getJobLevel(OfflinePlayer, Job) -> int
 *   com.willfp.ecojobs.api.EcoJobsAPI.giveJobExperience(Player, Job, double)
 * </pre>
 */
public final class EcoJobsHook {

    private final Logger log;
    private final Method getById;
    private final Method hasJobActive;
    private final Method getJobLevel;
    private final Method giveJobExperience;
    private boolean warned;

    private EcoJobsHook(Logger log, Method getById, Method hasJobActive, Method getJobLevel,
                        Method giveJobExperience) {
        this.log = log;
        this.getById = getById;
        this.hasJobActive = hasJobActive;
        this.getJobLevel = getJobLevel;
        this.giveJobExperience = giveJobExperience;
    }

    public static EcoJobsHook create(Logger log) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("EcoJobs");
        if (plugin == null || !plugin.isEnabled()) {
            return null;
        }
        try {
            ClassLoader loader = plugin.getClass().getClassLoader();
            Class<?> jobs = Class.forName("com.willfp.ecojobs.jobs.Jobs", true, loader);
            Class<?> job = Class.forName("com.willfp.ecojobs.jobs.Job", true, loader);
            Class<?> api = Class.forName("com.willfp.ecojobs.api.EcoJobsAPI", true, loader);
            return new EcoJobsHook(log,
                    jobs.getMethod("getByID", String.class),
                    api.getMethod("hasJobActive", OfflinePlayer.class, job),
                    api.getMethod("getJobLevel", OfflinePlayer.class, job),
                    api.getMethod("giveJobExperience", Player.class, job, double.class));
        } catch (ReflectiveOperationException | LinkageError e) {
            log.log(Level.WARNING, "EcoJobs is installed but its API could not be reached; "
                    + "job rewards from the table are skipped.", e);
            return null;
        }
    }

    private Object job(String id) throws ReflectiveOperationException {
        Object job = getById.invoke(null, id);
        if (job == null) {
            warnOnce("EcoJobs has no job '" + id + "'.", null);
        }
        return job;
    }

    /** Empty when the job doesn't exist or the call failed. */
    public java.util.Optional<Boolean> isActive(OfflinePlayer player, String jobId) {
        try {
            Object job = job(jobId);
            return job == null ? java.util.Optional.empty()
                    : java.util.Optional.of((Boolean) hasJobActive.invoke(null, player, job));
        } catch (ReflectiveOperationException | RuntimeException e) {
            warnOnce("Checking an EcoJobs job failed.", e);
            return java.util.Optional.empty();
        }
    }

    public OptionalInt level(OfflinePlayer player, String jobId) {
        try {
            Object job = job(jobId);
            return job == null ? OptionalInt.empty() : OptionalInt.of((Integer) getJobLevel.invoke(null, player, job));
        } catch (ReflectiveOperationException | RuntimeException e) {
            warnOnce("Reading an EcoJobs level failed.", e);
            return OptionalInt.empty();
        }
    }

    public boolean giveXp(Player player, String jobId, double amount) {
        try {
            Object job = job(jobId);
            if (job == null) {
                return false;
            }
            giveJobExperience.invoke(null, player, job, amount);
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            warnOnce("Giving EcoJobs XP failed.", e);
            return false;
        }
    }

    private void warnOnce(String message, Throwable cause) {
        if (!warned) {
            warned = true;
            log.log(Level.WARNING, message + " (further EcoJobs errors are not logged)", cause);
        }
    }
}
