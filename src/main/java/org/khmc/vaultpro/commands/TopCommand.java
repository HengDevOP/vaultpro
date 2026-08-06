package org.khmc.vaultpro.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.khmc.vaultpro.VaultProPlugin;
import org.khmc.vaultpro.util.ColorUtil;
import org.khmc.vaultpro.util.SoundUtil;

import java.util.Collections;
import java.util.List;

public class TopCommand implements CommandExecutor, TabCompleter {

    private final VaultProPlugin plugin;

    public TopCommand(VaultProPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtil.parse("<red>Only players can open the top balance GUI."));
            return true;
        }

        if (!player.hasPermission("vaultpro.top")) {
            player.sendMessage(ColorUtil.parse("&c✦ ʏᴏᴜ ᴅᴏ ɴᴏᴛ ʜᴀᴠᴇ ᴘᴇʀᴍɪssɪᴏɴ."));
            SoundUtil.playError(player);
            return true;
        }

        int page = 1;
        if (args.length >= 1) {
            try {
                page = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {}
        }

        plugin.getTopGUI().open(player, page);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("1", "2", "3", "4", "5");
        }
        return Collections.emptyList();
    }
}
