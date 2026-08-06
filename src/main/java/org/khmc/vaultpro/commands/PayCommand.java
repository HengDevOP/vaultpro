package org.khmc.vaultpro.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.khmc.vaultpro.service.VaultServiceImpl;
import org.khmc.vaultpro.util.ColorUtil;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class PayCommand implements CommandExecutor, TabCompleter {

    private final VaultServiceImpl vaultService;

    public PayCommand(VaultServiceImpl vaultService) {
        this.vaultService = vaultService;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtil.parse("&cOnly players can use this command."));
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(ColorUtil.parse("&cUsage: /pay <player> <amount>"));
            return true;
        }

        String targetName = args[0];
        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (target == null || (!target.hasPlayedBefore() && !target.isOnline())) {
            player.sendMessage(ColorUtil.parse("&cPlayer '" + targetName + "' was not found or has never played before."));
            return true;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(ColorUtil.parse("&cYou cannot pay yourself."));
            return true;
        }

        // Check if target player has payment toggle enabled in Settings
        if (Bukkit.getPluginManager().isPluginEnabled("Settings")) {
            try {
                org.khmc.settings.SettingsPlugin settingsPlugin = org.khmc.settings.SettingsPlugin.getInstance();
                if (settingsPlugin != null && settingsPlugin.getSettingsManager() != null) {
                    if (!settingsPlugin.getSettingsManager().isPaymentEnabled(target.getUniqueId())) {
                        player.sendMessage(ColorUtil.parse("&c✦ " + ColorUtil.toSmallCaps("this player has payment disabled")));
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.5f);
                        return true;
                    }
                }
            } catch (Throwable ignored) {}
        }

        double amount;
        try {
            amount = parseCompactNumber(args[1]);
            if (amount <= 0) {
                player.sendMessage(ColorUtil.parse("&cAmount must be greater than 0."));
                return true;
            }
        } catch (NumberFormatException e) {
            player.sendMessage(ColorUtil.parse("&cInvalid number format for amount: " + args[1]));
            return true;
        }

        double balance = vaultService.getBalance(player, "money");
        if (balance < amount) {
            player.sendMessage(ColorUtil.parse("&cYou do not have enough money. (Balance: " + vaultService.format("money", balance) + ")"));
            return true;
        }

        if (vaultService.withdraw(player, "money", amount)) {
            if (vaultService.deposit(target, "money", amount)) {
                String formattedAmount = vaultService.format("money", amount);
                String displayName = target.getName() != null ? target.getName() : targetName;

                // Sender message: "You have paid &b{player} for {amount}" (only placeholders colored)
                player.sendMessage(ColorUtil.parse("You have paid &b" + displayName + " &rfor " + formattedAmount));
                // Pitched 1.8 orb pickup sound
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.8f);

                if (target.isOnline() && target.getPlayer() != null) {
                    Player targetPlayer = target.getPlayer();

                    // Receiver message always sends as normal: "&b{player} &rhas paid you {amount}"
                    targetPlayer.sendMessage(ColorUtil.parse("&b" + player.getName() + " &rhas paid you " + formattedAmount));

                    // Check if receiver has payment_alerts enabled in Settings to play sound
                    boolean playSound = true;
                    if (Bukkit.getPluginManager().isPluginEnabled("Settings")) {
                        try {
                            org.khmc.settings.SettingsPlugin settingsPlugin = org.khmc.settings.SettingsPlugin.getInstance();
                            if (settingsPlugin != null && settingsPlugin.getSettingsManager() != null) {
                                playSound = settingsPlugin.getSettingsManager().isPaymentAlertsEnabled(target.getUniqueId());
                            }
                        } catch (Throwable ignored) {}
                    }

                    if (playSound) {
                        targetPlayer.playSound(targetPlayer.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.8f);
                    }
                }
            } else {
                vaultService.deposit(player, "money", amount);
                player.sendMessage(ColorUtil.parse("&cAn error occurred while processing the payment."));
            }
        } else {
            player.sendMessage(ColorUtil.parse("&cAn error occurred while processing the payment."));
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> !name.equalsIgnoreCase(sender.getName()) && name.toLowerCase(Locale.ROOT).startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    private double parseCompactNumber(String input) throws NumberFormatException {
        String clean = input.trim().toUpperCase(Locale.ROOT);
        if (clean.isEmpty()) {
            throw new NumberFormatException("Empty input");
        }

        double multiplier = 1.0;
        int suffixIndex = -1;

        if (clean.endsWith("K")) {
            multiplier = 1_000.0;
            suffixIndex = clean.lastIndexOf('K');
        } else if (clean.endsWith("M")) {
            multiplier = 1_000_000.0;
            suffixIndex = clean.lastIndexOf('M');
        } else if (clean.endsWith("B")) {
            multiplier = 1_000_000_000.0;
            suffixIndex = clean.lastIndexOf('B');
        } else if (clean.endsWith("T")) {
            multiplier = 1_000_000_000_000.0;
            suffixIndex = clean.lastIndexOf('T');
        }

        String numberPart = suffixIndex != -1 ? clean.substring(0, suffixIndex) : clean;
        return Double.parseDouble(numberPart) * multiplier;
    }
}
