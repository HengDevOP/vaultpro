package org.khmc.vaultpro.service;

import org.bukkit.OfflinePlayer;

public interface VaultService {

    boolean hasAccount(OfflinePlayer player);

    double getBalance(OfflinePlayer player, String currency);

    boolean withdraw(OfflinePlayer player, String currency, double amount);

    boolean deposit(OfflinePlayer player, String currency, double amount);

    String format(String currency, double amount);

    String formatCurrency(double amount);

    String getSymbol(String currency);

    String getSymbol();

    String getColorFormatted();
}
