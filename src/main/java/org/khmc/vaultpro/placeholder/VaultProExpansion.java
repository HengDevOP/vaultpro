package org.khmc.vaultpro.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.khmc.vaultpro.VaultProPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VaultProExpansion extends PlaceholderExpansion {

    private final VaultProPlugin plugin;

    public VaultProExpansion(VaultProPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "vaultpro";
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        String lower = params.toLowerCase(java.util.Locale.ROOT);

        // Self player placeholders
        if (lower.equals("top_me")) {
            return player != null && player.getName() != null ? player.getName() : "None";
        }
        if (lower.equals("top_money")) {
            String color = plugin.getVaultService().getColorFormatted();
            String symbol = plugin.getVaultService().getSymbol();
            if (player == null) return color + symbol + " &f0.00";
            double balance = plugin.getVaultService().getBalance(player, "money");
            String numberStr = plugin.getVaultService().formatNumberOnly(balance);
            return color + symbol + " &f" + numberStr;
        }
        if (lower.equals("top_rank")) {
            if (player == null) return "N/A";
            var topList = plugin.getVaultService().getTopPlayers();
            for (int i = 0; i < topList.size(); i++) {
                if (topList.get(i).uuid().equals(player.getUniqueId())) {
                    return String.valueOf(i + 1);
                }
            }
            return "N/A";
        }

        // Top N placeholders: top_<n>_player or top_<n>_money
        if (lower.startsWith("top_")) {
            String sub = lower.substring(4);
            if (sub.endsWith("_player")) {
                String numStr = sub.substring(0, sub.length() - 7);
                try {
                    int rank = Integer.parseInt(numStr);
                    var topList = plugin.getVaultService().getTopPlayers();
                    if (rank >= 1 && rank <= topList.size()) {
                        return topList.get(rank - 1).name();
                    }
                    return "None";
                } catch (NumberFormatException ignored) {}
            } else if (sub.endsWith("_money")) {
                String numStr = sub.substring(0, sub.length() - 6);
                try {
                    int rank = Integer.parseInt(numStr);
                    String color = plugin.getVaultService().getColorFormatted();
                    String symbol = plugin.getVaultService().getSymbol();
                    var topList = plugin.getVaultService().getTopPlayers();
                    if (rank >= 1 && rank <= topList.size()) {
                        double bal = topList.get(rank - 1).balance();
                        return plugin.getVaultService().formatNumberOnly(bal);
                    }
                    return "0.00";
                } catch (NumberFormatException ignored) {}
            }
        }

        if (player == null) {
            return "0";
        }

        double balance = plugin.getVaultService().getBalance(player, "money");
        return plugin.getVaultService().formatNumberOnly(balance);
    }
}
