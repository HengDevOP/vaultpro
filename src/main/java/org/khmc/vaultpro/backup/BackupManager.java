package org.khmc.vaultpro.backup;

import org.khmc.vaultpro.VaultProPlugin;
import org.khmc.vaultpro.util.SchedulerUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;

public class BackupManager {

    private final VaultProPlugin plugin;
    private SchedulerUtil.TaskWrapper backupTask;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");

    public BackupManager(VaultProPlugin plugin) {
        this.plugin = plugin;
        startBackupTask();
    }

    public void startBackupTask() {
        stop();

        boolean enabled = plugin.getConfig().getBoolean("backup.enabled", true);
        if (!enabled) {
            plugin.getLogger().info("Database backup system is disabled in config.yml.");
            return;
        }

        long intervalSeconds = plugin.getConfig().getLong("backup.backupInterval", 21600L);
        plugin.getLogger().info("Database backup system active. Interval: " + intervalSeconds + "s (" + (intervalSeconds / 3600.0) + " hours).");

        // Schedule async timer task
        backupTask = SchedulerUtil.runTimerAsync(plugin, () -> {
            try {
                performBackup();
            } catch (Exception e) {
                plugin.getLogger().warning("Error during scheduled backup: " + e.getMessage());
            }
        }, intervalSeconds, intervalSeconds);
    }

    public synchronized boolean performBackup() {
        File dbFolder = new File(plugin.getDataFolder(), "database");
        File dbFile = new File(dbFolder, "users.db");

        if (!dbFile.exists()) {
            plugin.getLogger().warning("Backup skipped: users.db does not exist yet.");
            return false;
        }

        String backupDirName = plugin.getConfig().getString("backup.backupDirectory", "backups");
        File backupFolder = new File(plugin.getDataFolder(), backupDirName);
        if (!backupFolder.exists()) {
            backupFolder.mkdirs();
        }

        String timeStamp = dateFormat.format(new Date());
        File backupFile = new File(backupFolder, "users_backup_" + timeStamp + ".db");

        try (FileChannel sourceChannel = new FileInputStream(dbFile).getChannel();
             FileChannel destChannel = new FileOutputStream(backupFile).getChannel()) {
            destChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
            plugin.getLogger().info("Successfully created database backup: " + backupFile.getName() + " (" + (backupFile.length() / 1024) + " KB)");
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to create database backup: " + e.getMessage());
            return false;
        }

        purgeOldBackups(backupFolder);
        return true;
    }

    private void purgeOldBackups(File backupFolder) {
        int maxBackups = plugin.getConfig().getInt("backup.maxBackups", 10);
        if (maxBackups <= 0) return;

        File[] files = backupFolder.listFiles((dir, name) -> name.startsWith("users_backup_") && name.endsWith(".db"));
        if (files == null || files.length <= maxBackups) return;

        // Sort files by last modified timestamp (oldest first)
        Arrays.sort(files, Comparator.comparingLong(File::lastModified));

        int filesToDelete = files.length - maxBackups;
        for (int i = 0; i < filesToDelete; i++) {
            File toDelete = files[i];
            if (toDelete.delete()) {
                plugin.getLogger().info("Purged old backup file: " + toDelete.getName());
            }
        }
    }

    public void stop() {
        if (backupTask != null) {
            backupTask.cancel();
            backupTask = null;
        }
    }
}
