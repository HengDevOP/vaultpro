package org.khmc.vaultpro.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.khmc.vaultpro.VaultProPlugin;
import org.khmc.vaultpro.listener.TopSearchListener;
import org.khmc.vaultpro.service.TopPlayerEntry;
import org.khmc.vaultpro.util.ColorUtil;
import org.khmc.vaultpro.util.SchedulerUtil;
import org.khmc.vaultpro.util.SoundUtil;

import java.util.ArrayList;
import java.util.List;

public class TopGUI implements Listener {

    private final VaultProPlugin plugin;

    public TopGUI(VaultProPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public static class TopHolder implements InventoryHolder {
        private final int page;
        private final int totalPages;
        private final List<TopPlayerEntry> allEntries;

        public TopHolder(int page, int totalPages, List<TopPlayerEntry> allEntries) {
            this.page = page;
            this.totalPages = totalPages;
            this.allEntries = allEntries;
        }

        public int getPage() { return page; }
        public int getTotalPages() { return totalPages; }
        public List<TopPlayerEntry> getAllEntries() { return allEntries; }

        @Override
        public Inventory getInventory() { return null; }
    }

    public void open(Player player, int page) {
        open(player, page, null);
    }

    public void open(Player player, int page, String highlightedPlayerName) {
        SchedulerUtil.runTaskAsync(plugin, () -> {
            List<TopPlayerEntry> entries = plugin.getVaultService().getTopPlayers();
            int total = entries.size();
            int totalPages = Math.max(1, (int) Math.ceil(total / 45.0));
            int validPage = Math.max(1, Math.min(page, totalPages));

            SchedulerUtil.runTaskEntity(plugin, player, () -> {
                String title = "&8ᴛᴏᴘ ʙᴀʟᴀɴᴄᴇs &7(" + validPage + "/" + totalPages + ")";
                Inventory inv = Bukkit.createInventory(new TopHolder(validPage, totalPages, entries), 54, ColorUtil.parse(title));

                populate(inv, entries, validPage, totalPages, highlightedPlayerName);
                player.openInventory(inv);
            });
        });
    }

    private void populate(Inventory inv, List<TopPlayerEntry> entries, int page, int totalPages, String highlightedName) {
        inv.clear();

        int startIndex = (page - 1) * 45;
        int endIndex = Math.min(startIndex + 45, entries.size());

        for (int i = startIndex; i < endIndex; i++) {
            int slot = i - startIndex;
            int rank = i + 1;
            TopPlayerEntry entry = entries.get(i);

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(Bukkit.getOfflinePlayer(entry.uuid()));
                boolean isHighlight = highlightedName != null && entry.name().equalsIgnoreCase(highlightedName);
                String prefix = rank <= 3 ? "&e&l#" + rank : "&a#" + rank;
                if (isHighlight) prefix = "&d&l★ #" + rank;

                meta.displayName(ColorUtil.parse(prefix + " &f" + entry.name()));

                List<Component> lore = new ArrayList<>();
                lore.add(ColorUtil.parse("&7" + ColorUtil.toSmallCaps("balance: ") + "&a" + plugin.getVaultService().formatCurrency(entry.balance())));
                if (isHighlight) {
                    lore.add(ColorUtil.parse("&d• " + ColorUtil.toSmallCaps("searched player")));
                }
                meta.lore(lore);
                if (isHighlight) {
                    meta.setEnchantmentGlintOverride(true);
                }
                head.setItemMeta(meta);
            }
            inv.setItem(slot, head);
        }

        // Bottom row (slots 45 to 53)
        // Filler panes
        ItemStack pane = makeItem(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, pane);
        }

        // Arrow Previous (Slot 45)
        if (page > 1) {
            inv.setItem(45, makeItem(Material.ARROW, "&a&l« " + ColorUtil.toSmallCaps("previous page"), List.of("&7Go to page " + (page - 1))));
        }

        // Hopper Search (Slot 49)
        inv.setItem(49, makeItem(Material.HOPPER, "&e&l" + ColorUtil.toSmallCaps("search player"), List.of("&7Click to search for a specific player.")));

        // Arrow Next (Slot 53)
        if (page < totalPages) {
            inv.setItem(53, makeItem(Material.ARROW, "&a&l" + ColorUtil.toSmallCaps("next page") + " »", List.of("&7Go to page " + (page + 1))));
        }
    }

    private ItemStack makeItem(Material mat, String title, List<String> loreLines) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(ColorUtil.parse(title));
            List<Component> lore = new ArrayList<>();
            for (String l : loreLines) {
                lore.add(ColorUtil.parse(l));
            }
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof TopHolder holder)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;
        event.setCancelled(true);

        int slot = event.getSlot();

        // Clicked Previous Arrow (Slot 45)
        if (slot == 45 && holder.getPage() > 1) {
            SoundUtil.playClick(player);
            open(player, holder.getPage() - 1);
            return;
        }

        // Clicked Next Arrow (Slot 53)
        if (slot == 53 && holder.getPage() < holder.getTotalPages()) {
            SoundUtil.playClick(player);
            open(player, holder.getPage() + 1);
            return;
        }

        // Clicked Hopper Search (Slot 49)
        if (slot == 49) {
            SoundUtil.playClick(player);
            player.closeInventory();
            TopSearchListener.awaitSearch(player, this);
            return;
        }
    }
}
