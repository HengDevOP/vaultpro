package org.khmc.vaultpro.service;

import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;

public class VaultServiceImpl implements VaultService {

    private static final String CURRENCY_MONEY = "money";
    private final JavaPlugin plugin;
    private final Object lock = new Object();

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException ignored) {}
    }

    public VaultServiceImpl(JavaPlugin plugin) {
        this.plugin = plugin;
        initDatabase();
    }

    private Connection getConnection() throws SQLException {
        File dbFolder = new File(plugin.getDataFolder(), "database");
        if (!dbFolder.exists()) {
            dbFolder.mkdirs();
        }
        File dbFile = new File(dbFolder, "users.db");
        String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
        return DriverManager.getConnection(url);
    }

    private void initDatabase() {
        synchronized (lock) {
            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement()) {
                String sql = "CREATE TABLE IF NOT EXISTS vault_balances (" +
                             "uuid VARCHAR(36) NOT NULL, " +
                             "currency VARCHAR(64) NOT NULL, " +
                             "balance DOUBLE NOT NULL, " +
                             "PRIMARY KEY (uuid, currency)" +
                             ");";
                stmt.execute(sql);
            } catch (SQLException e) {
                plugin.getLogger().severe("Could not initialize SQLite database: " + e.getMessage());
            }
        }
    }

    @Override
    public boolean hasAccount(OfflinePlayer player) {
        if (player == null) return false;
        synchronized (lock) {
            String sql = "SELECT 1 FROM vault_balances WHERE uuid = ? LIMIT 1;";
            try (Connection conn = getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, player.getUniqueId().toString());
                try (ResultSet rs = pstmt.executeQuery()) {
                    return rs.next();
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Could not query account existence: " + e.getMessage());
            }
        }
        return false;
    }

    public double getDefaultBalance(String currency) {
        Object obj = plugin.getConfig().get("vaultpro.money");
        if (obj instanceof java.util.List) {
            java.util.List<?> list = (java.util.List<?>) obj;
            for (Object entry : list) {
                if (entry instanceof java.util.Map) {
                    java.util.Map<?, ?> map = (java.util.Map<?, ?>) entry;
                    if (map.containsKey("default")) {
                        Object val = map.get("default");
                        if (val instanceof Number) {
                            return ((Number) val).doubleValue();
                        }
                    }
                }
            }
        } else if (obj instanceof org.bukkit.configuration.ConfigurationSection) {
            org.bukkit.configuration.ConfigurationSection section = (org.bukkit.configuration.ConfigurationSection) obj;
            return section.getDouble("default", 0.0);
        }
        return 0.0;
    }

    public String getFormatColor(String currency) {
        return getColorFormatted();
    }

    @Override
    public String getColorFormatted() {
        if (plugin.getConfig().contains("prefix.colorFomatted")) {
            return plugin.getConfig().getString("prefix.colorFomatted", "&a");
        }
        if (plugin.getConfig().contains("prefix.colorFormatted")) {
            return plugin.getConfig().getString("prefix.colorFormatted", "&a");
        }
        return plugin.getConfig().getString("prefix.colorFormateed", "&a");
    }

    @Override
    public String getSymbol() {
        return plugin.getConfig().getString("prefix.symbol", "$");
    }

    @Override
    public String getSymbol(String currency) {
        return getSymbol();
    }

    @Override
    public double getBalance(OfflinePlayer player, String currency) {
        if (player == null) return 0.0;
        synchronized (lock) {
            String sql = "SELECT balance FROM vault_balances WHERE uuid = ? AND currency = ?;";
            try (Connection conn = getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, player.getUniqueId().toString());
                pstmt.setString(2, CURRENCY_MONEY);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getDouble("balance");
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Could not query balance: " + e.getMessage());
            }
        }
        return getDefaultBalance(CURRENCY_MONEY);
    }

    @Override
    public boolean withdraw(OfflinePlayer player, String currency, double amount) {
        if (player == null || amount < 0) return false;
        synchronized (lock) {
            double current = getBalance(player, CURRENCY_MONEY);
            if (current < amount) {
                return false;
            }
            return setBalance(player, CURRENCY_MONEY, current - amount);
        }
    }

    @Override
    public boolean deposit(OfflinePlayer player, String currency, double amount) {
        if (player == null || amount < 0) return false;
        synchronized (lock) {
            double current = getBalance(player, CURRENCY_MONEY);
            return setBalance(player, CURRENCY_MONEY, current + amount);
        }
    }

    public boolean setBalance(OfflinePlayer player, String currency, double amount) {
        if (player == null || amount < 0) return false;
        synchronized (lock) {
            String sql = "INSERT OR REPLACE INTO vault_balances (uuid, currency, balance) VALUES (?, ?, ?);";
            try (Connection conn = getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, player.getUniqueId().toString());
                pstmt.setString(2, CURRENCY_MONEY);
                pstmt.setDouble(3, amount);
                pstmt.executeUpdate();
                return true;
            } catch (SQLException e) {
                plugin.getLogger().severe("Could not set balance: " + e.getMessage());
            }
        }
        return false;
    }

    @Override
    public String formatCurrency(double amount) {
        String formatType = plugin.getConfig().getString("formatted", "COMPACT").toUpperCase(Locale.ROOT);
        String formattedValue;
        if ("GROUPED".equals(formatType)) {
            formattedValue = formatNumberGrouped(amount);
        } else {
            formattedValue = formatNumberCompact(amount);
        }
        String symbol = getSymbol();
        String color = getColorFormatted();
        return color + symbol + " " + formattedValue;
    }

    @Override
    public String format(String currency, double amount) {
        return formatCurrency(amount);
    }

    public String formatPlaceholder(String currency, double amount) {
        return formatNumberOnly(amount);
    }

    public String formatNumberOnly(double amount) {
        String formatType = plugin.getConfig().getString("number-format", "compact").toLowerCase(Locale.ROOT);
        if ("grouped".equals(formatType)) {
            return formatNumberGrouped(amount);
        } else if ("plain".equals(formatType) || "raw".equals(formatType)) {
            if (amount == (long) amount) {
                return String.format(Locale.ROOT, "%d", (long) amount);
            } else {
                return String.format(Locale.ROOT, "%.2f", amount);
            }
        } else {
            return formatNumberCompact(amount);
        }
    }

    private String formatNumberCompact(double amount) {
        if (amount >= 1_000_000_000_000.0) {
            return String.format(Locale.ROOT, "%.2fT", amount / 1_000_000_000_000.0);
        } else if (amount >= 1_000_000_000.0) {
            return String.format(Locale.ROOT, "%.2fB", amount / 1_000_000_000.0);
        } else if (amount >= 1_000_000.0) {
            return String.format(Locale.ROOT, "%.2fM", amount / 1_000_000.0);
        } else if (amount >= 1_000.0) {
            return String.format(Locale.ROOT, "%.2fK", amount / 1_000.0);
        } else {
            if (amount == (long) amount) {
                return String.format(Locale.ROOT, "%d", (long) amount);
            } else {
                return String.format(Locale.ROOT, "%.2f", amount);
            }
        }
    }

    private String formatNumberGrouped(double amount) {
        if (amount == (long) amount) {
            return String.format(Locale.ROOT, "%,d", (long) amount);
        } else {
            return String.format(Locale.ROOT, "%,.2f", amount);
        }
    }

    private volatile java.util.List<TopPlayerEntry> cachedTopPlayers = null;
    private volatile long lastTopQueryTime = 0L;
    private static final long CACHE_TTL_MS = 3000L;

    public java.util.List<TopPlayerEntry> getTopPlayers() {
        long now = System.currentTimeMillis();
        java.util.List<TopPlayerEntry> currentCache = cachedTopPlayers;
        if (currentCache != null && (now - lastTopQueryTime) < CACHE_TTL_MS) {
            return currentCache;
        }

        java.util.List<TopPlayerEntry> list = new java.util.ArrayList<>();
        synchronized (lock) {
            String sql = "SELECT uuid, balance FROM vault_balances WHERE currency = 'money' ORDER BY balance DESC LIMIT 10;";
            try (Connection conn = getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql);
                 ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    try {
                        org.bukkit.OfflinePlayer op = org.bukkit.Bukkit.getOfflinePlayer(java.util.UUID.fromString(rs.getString("uuid")));
                        String name = op.getName();
                        if (name != null) {
                            list.add(new TopPlayerEntry(op.getUniqueId(), name, rs.getDouble("balance")));
                        }
                    } catch (Throwable ignored) {}
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Could not query top balances: " + e.getMessage());
            }
        }
        cachedTopPlayers = list;
        lastTopQueryTime = now;
        return list;
    }
}
