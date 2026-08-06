package org.khmc.vaultpro.util;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

public final class SoundUtil {
    private SoundUtil() {}

    public static void playClick(Player p) {
        if (p != null) p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.2f);
    }

    public static void playError(Player p) {
        if (p != null) p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
    }

    public static void playSuccess(Player p) {
        if (p != null) p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
    }
}
