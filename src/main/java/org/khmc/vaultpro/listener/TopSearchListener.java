package org.khmc.vaultpro.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.khmc.vaultpro.VaultProPlugin;
import org.khmc.vaultpro.gui.TopGUI;
import org.khmc.vaultpro.service.TopPlayerEntry;
import org.khmc.vaultpro.util.ColorUtil;
import org.khmc.vaultpro.util.SchedulerUtil;
import org.khmc.vaultpro.util.SoundUtil;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TopSearchListener implements Listener {

    private static final Map<UUID, TopGUI> awaitingSearch = new ConcurrentHashMap<>();
    private final VaultProPlugin plugin;

    public TopSearchListener(VaultProPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public static void awaitSearch(Player player, TopGUI gui) {
        if (player == null) return;
        awaitingSearch.put(player.getUniqueId(), gui);
        player.sendMessage(ColorUtil.parse("&a✦ &f" + ColorUtil.toSmallCaps("please type the player name in chat to search...")));
        player.sendMessage(ColorUtil.parse("&7" + ColorUtil.toSmallCaps("type 'cancel' to cancel.")));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        TopGUI gui = awaitingSearch.remove(player.getUniqueId());
        if (gui == null) return;

        event.setCancelled(true);
        String targetName = event.getMessage().trim();

        if (targetName.equalsIgnoreCase("cancel")) {
            SchedulerUtil.runTaskEntity(plugin, player, () -> {
                player.sendMessage(ColorUtil.parse("&c✦ " + ColorUtil.toSmallCaps("search cancelled.")));
                SoundUtil.playError(player);
                gui.open(player, 1);
            });
            return;
        }

        SchedulerUtil.runTaskAsync(plugin, () -> {
            List<TopPlayerEntry> entries = plugin.getVaultService().getTopPlayers();
            int foundRank = -1;

            for (int i = 0; i < entries.size(); i++) {
                if (entries.get(i).name().equalsIgnoreCase(targetName)) {
                    foundRank = i + 1;
                    break;
                }
            }

            final int rank = foundRank;
            SchedulerUtil.runTaskEntity(plugin, player, () -> {
                if (rank != -1) {
                    int targetPage = (int) Math.ceil(rank / 45.0);
                    player.sendMessage(ColorUtil.parse("&a✦ &ffᴏᴜɴᴅ &e" + targetName + " &fᴀᴛ ʀᴀɴᴋ &e#" + rank + " &f(ᴘᴀɢᴇ " + targetPage + ")!"));
                    SoundUtil.playSuccess(player);
                    gui.open(player, targetPage, targetName);
                } else {
                    player.sendMessage(ColorUtil.parse("&c✦ " + ColorUtil.toSmallCaps("player ") + "&e" + targetName + " &c" + ColorUtil.toSmallCaps("not found in economy database!")));
                    SoundUtil.playError(player);
                    gui.open(player, 1);
                }
            });
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        awaitingSearch.remove(event.getPlayer().getUniqueId());
    }
}
