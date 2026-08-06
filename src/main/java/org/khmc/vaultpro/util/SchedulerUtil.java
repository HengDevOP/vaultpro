package org.khmc.vaultpro.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

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

    public static void runTaskEntity(Plugin plugin, Entity entity, Runnable runnable) {
        if (IS_FOLIA && entity != null) {
            entity.getScheduler().run(plugin, task -> runnable.run(), null);
        } else {
            runTaskGlobal(plugin, runnable);
        }
    }
}
