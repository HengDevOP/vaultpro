package org.khmc.vaultpro.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.TimeUnit;

public final class SchedulerUtil {

    private static final boolean IS_FOLIA;

    static {
        boolean folia = false;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            folia = true;
        } catch (ClassNotFoundException e) {
            folia = false;
        }
        IS_FOLIA = folia;
    }

    private SchedulerUtil() {}

    public static boolean isFolia() {
        return IS_FOLIA;
    }

    public interface TaskWrapper {
        void cancel();
    }

    public static void runTaskGlobal(Plugin plugin, Runnable runnable) {
        if (IS_FOLIA) {
            Bukkit.getGlobalRegionScheduler().execute(plugin, runnable);
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }

    public static void runTaskAsync(Plugin plugin, Runnable runnable) {
        if (IS_FOLIA) {
            Bukkit.getAsyncScheduler().runNow(plugin, task -> runnable.run());
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
        }
    }

    public static TaskWrapper runTimerAsync(Plugin plugin, Runnable runnable, long delaySeconds, long periodSeconds) {
        long safeDelay = Math.max(1, delaySeconds);
        long safePeriod = Math.max(1, periodSeconds);

        if (IS_FOLIA) {
            io.papermc.paper.threadedregions.scheduler.ScheduledTask task = Bukkit.getAsyncScheduler().runAtFixedRate(
                    plugin, t -> runnable.run(), safeDelay, safePeriod, TimeUnit.SECONDS
            );
            return task::cancel;
        } else {
            org.bukkit.scheduler.BukkitTask task = Bukkit.getScheduler().runTaskTimerAsynchronously(
                    plugin, runnable, safeDelay * 20L, safePeriod * 20L
            );
            return task::cancel;
        }
    }

    public static void runTaskEntity(Plugin plugin, Entity entity, Runnable runnable) {
        if (IS_FOLIA && entity != null) {
            entity.getScheduler().run(plugin, task -> runnable.run(), null);
        } else {
            runTaskGlobal(plugin, runnable);
        }
    }
}
