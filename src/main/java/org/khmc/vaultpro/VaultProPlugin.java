package org.khmc.vaultpro;

import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.khmc.vaultpro.commands.PayCommand;
import org.khmc.vaultpro.commands.TopCommand;
import org.khmc.vaultpro.commands.VaultProCommand;
import org.khmc.vaultpro.gui.TopGUI;
import org.khmc.vaultpro.listener.TopSearchListener;
import org.khmc.vaultpro.service.VaultService;
import org.khmc.vaultpro.service.VaultServiceImpl;

import java.util.Objects;

public class VaultProPlugin extends JavaPlugin {

    private static VaultProPlugin instance;
    private VaultServiceImpl vaultService;
    private TopGUI topGUI;
    private org.khmc.vaultpro.listener.MoneyPaperListener moneyPaperListener;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        vaultService = new VaultServiceImpl(this);
        topGUI = new TopGUI(this);
        new TopSearchListener(this);
        moneyPaperListener = new org.khmc.vaultpro.listener.MoneyPaperListener(this);

        // Register VaultService
        getServer().getServicesManager().register(
                VaultService.class,
                vaultService,
                this,
                ServicePriority.Highest
        );

        // Register /v command
        VaultProCommand commandExecutor = new VaultProCommand(this, vaultService);
        Objects.requireNonNull(getCommand("v")).setExecutor(commandExecutor);
        Objects.requireNonNull(getCommand("v")).setTabCompleter(commandExecutor);

        // Register /pay command
        PayCommand payCommand = new PayCommand(vaultService);
        Objects.requireNonNull(getCommand("pay")).setExecutor(payCommand);
        Objects.requireNonNull(getCommand("pay")).setTabCompleter(payCommand);

        // Register /top command
        TopCommand topCommand = new TopCommand(this);
        Objects.requireNonNull(getCommand("top")).setExecutor(topCommand);
        Objects.requireNonNull(getCommand("top")).setTabCompleter(topCommand);

        // Register PlaceholderAPI expansion
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new org.khmc.vaultpro.placeholder.VaultProExpansion(this).register();
        }

        getLogger().info("VaultPro economy provider has been enabled successfully.");
    }

    @Override
    public void onDisable() {
        getLogger().info("VaultPro has been disabled.");
    }

    public static VaultProPlugin getInstance() {
        return instance;
    }

    public VaultServiceImpl getVaultService() {
        return vaultService;
    }

    public TopGUI getTopGUI() {
        return topGUI;
    }

    public org.khmc.vaultpro.listener.MoneyPaperListener getMoneyPaperListener() {
        return moneyPaperListener;
    }
}
