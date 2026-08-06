package org.khmc.vaultpro.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.khmc.vaultpro.service.VaultServiceImpl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class VaultProCommand implements CommandExecutor, TabCompleter {

    private final org.khmc.vaultpro.VaultProPlugin plugin;
    private final VaultServiceImpl vaultService;

    public VaultProCommand(org.khmc.vaultpro.VaultProPlugin plugin, VaultServiceImpl vaultService) {
        this.plugin = plugin;
        this.vaultService = vaultService;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("vaultpro.admin") && !sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to execute this command.");
            return true;
        }

        if (args.length < 1) {
            sendUsage(sender);
            return true;
        }

        String action = args[0].toLowerCase(Locale.ROOT);

        // Handle /v wit <amount>
        if (action.equals("wit") || action.equals("withdraw")) {
            if (!(sender instanceof org.bukkit.entity.Player player)) {
                sender.sendMessage(ChatColor.RED + "Only players can withdraw money paper.");
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /v wit <amount>");
                return true;
            }
            double amount;
            try {
                amount = parseAmount(args[1]);
                if (amount <= 0) {
                    sender.sendMessage(ChatColor.RED + "Amount must be greater than zero.");
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid number format for amount: " + args[1]);
                return true;
            }

            // Create Money Paper item
            org.bukkit.inventory.ItemStack paper = new org.bukkit.inventory.ItemStack(org.bukkit.Material.PAPER);
            org.bukkit.inventory.meta.ItemMeta meta = paper.getItemMeta();
            if (meta != null) {
                meta.displayName(org.khmc.vaultpro.util.ColorUtil.parse("&aMoney Paper"));
                String symbol = vaultService.getSymbol();
                String symbolColor = vaultService.getColorFormatted();
                String numberOnly = vaultService.formatNumberOnly(amount);
                // Lore: &7Value <color><symbol> &f<amount>
                String loreLine = "&7Value " + symbolColor + symbol + "&f " + numberOnly;
                meta.lore(java.util.List.of(org.khmc.vaultpro.util.ColorUtil.parse(loreLine)));

                // Tag PDC value
                meta.getPersistentDataContainer().set(
                        plugin.getMoneyPaperListener().getValueKey(),
                        org.bukkit.persistence.PersistentDataType.DOUBLE,
                        amount
                );
                paper.setItemMeta(meta);
            }

            // Give to player
            java.util.HashMap<Integer, org.bukkit.inventory.ItemStack> leftover = player.getInventory().addItem(paper);
            if (!leftover.isEmpty()) {
                for (org.bukkit.inventory.ItemStack drop : leftover.values()) {
                    player.getWorld().dropItem(player.getLocation(), drop);
                }
            }

            // Play Map Art Sound (UI_CARTOGRAPHY_TABLE_TAKE_RESULT)
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, 1.0f, 1.0f);

            player.sendMessage(ChatColor.GREEN + "Withdrew " + vaultService.formatCurrency(amount) + " into a Money Paper.");
            return true;
        }

        if (args.length < 2) {
            sendUsage(sender);
            return true;
        }

        String targetName = args[1];
        String currency = "money";

        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (target == null || (!target.hasPlayedBefore() && !target.isOnline())) {
            sender.sendMessage(ChatColor.RED + "Player '" + targetName + "' was not found or has never played before.");
            return true;
        }

        double amount = 0.0;
        if (args.length >= 3) {
            try {
                amount = parseAmount(args[2]);
                if (amount < 0) {
                    sender.sendMessage(ChatColor.RED + "Amount must be positive.");
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid number format for amount: " + args[2]);
                return true;
            }
        } else if (!action.equals("reset")) {
            sendUsage(sender);
            return true;
        }

        switch (action) {
            case "add": {
                boolean success = vaultService.deposit(target, currency, amount);
                if (success) {
                    double current = vaultService.getBalance(target, currency);
                    sender.sendMessage(ChatColor.GREEN + "Added " + vaultService.format(currency, amount) + " to " + target.getName() + "'s balance. (New Balance: " + vaultService.format(currency, current) + ")");
                } else {
                    sender.sendMessage(ChatColor.RED + "Failed to add currency to player.");
                }
                break;
            }
            case "take":
            case "remove": {
                boolean success = vaultService.withdraw(target, currency, amount);
                if (success) {
                    double current = vaultService.getBalance(target, currency);
                    sender.sendMessage(ChatColor.GREEN + "Took " + vaultService.format(currency, amount) + " from " + target.getName() + "'s balance. (New Balance: " + vaultService.format(currency, current) + ")");
                } else {
                    sender.sendMessage(ChatColor.RED + "Player does not have enough money to withdraw " + amount + ".");
                }
                break;
            }
            case "reset": {
                boolean success = vaultService.setBalance(target, currency, amount);
                if (success) {
                    sender.sendMessage(ChatColor.GREEN + "Reset " + target.getName() + "'s balance to " + vaultService.format(currency, amount) + ".");
                } else {
                    sender.sendMessage(ChatColor.RED + "Failed to reset currency.");
                }
                break;
            }
            default: {
                sendUsage(sender);
                break;
            }
        }

        return true;
    }

    private double parseAmount(String input) throws NumberFormatException {
        if (input == null || input.isEmpty()) throw new NumberFormatException("Empty input");
        String clean = input.trim().toLowerCase(Locale.ROOT);
        double multiplier = 1.0;
        if (clean.endsWith("k")) {
            multiplier = 1_000.0;
            clean = clean.substring(0, clean.length() - 1);
        } else if (clean.endsWith("m")) {
            multiplier = 1_000_000.0;
            clean = clean.substring(0, clean.length() - 1);
        } else if (clean.endsWith("b")) {
            multiplier = 1_000_000_000.0;
            clean = clean.substring(0, clean.length() - 1);
        } else if (clean.endsWith("t")) {
            multiplier = 1_000_000_000_000.0;
            clean = clean.substring(0, clean.length() - 1);
        }
        double val = Double.parseDouble(clean);
        return val * multiplier;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== VaultPro Economy Management ===");
        sender.sendMessage(ChatColor.YELLOW + "/v wit <amount>");
        sender.sendMessage(ChatColor.YELLOW + "/v add <player> <amount>");
        sender.sendMessage(ChatColor.YELLOW + "/v take <player> <amount>");
        sender.sendMessage(ChatColor.YELLOW + "/v reset <player> [amount]");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("vaultpro.admin") && !sender.isOp()) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return Arrays.asList("wit", "add", "reset", "take").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("wit") || args[0].equalsIgnoreCase("withdraw")) {
                return Arrays.asList("100", "500", "1000", "5000", "10k", "1m").stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase(Locale.ROOT)))
                        .collect(Collectors.toList());
            }
            return Arrays.stream(Bukkit.getOfflinePlayers())
                    .map(OfflinePlayer::getName)
                    .filter(name -> name != null && name.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }

        if (args.length == 3) {
            List<String> list = Arrays.asList("100", "500", "1000", "5000", "10k", "1m");
            return list.stream()
                    .filter(s -> s.startsWith(args[2].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}
