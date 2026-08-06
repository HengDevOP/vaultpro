package org.khmc.vaultpro.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class ColorUtil {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('&')
            .hexCharacter('#')
            .build();

    private ColorUtil() {}

    public static Component parse(String input) {
        if (input == null) return Component.empty();
        Component comp;
        if (input.contains("&") || input.contains("§")) {
            comp = LEGACY.deserialize(input.replace('§', '&'));
        } else if (input.contains("<") && input.contains(">")) {
            try {
                comp = MINI.deserialize(input);
            } catch (Exception e) {
                comp = LEGACY.deserialize(input);
            }
        } else {
            comp = LEGACY.deserialize(input);
        }
        return Component.empty().decoration(TextDecoration.ITALIC, false).append(comp);
    }

    public static String toSmallCaps(String input) {
        if (input == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            sb.append(switch (Character.toLowerCase(c)) {
                case 'a' -> 'ᴀ';
                case 'b' -> 'ʙ';
                case 'c' -> 'ᴄ';
                case 'd' -> 'ᴅ';
                case 'e' -> 'ᴇ';
                case 'f' -> 'ꜰ';
                case 'g' -> 'ɢ';
                case 'h' -> 'ʜ';
                case 'i' -> 'ɪ';
                case 'j' -> 'ᴊ';
                case 'k' -> 'ᴋ';
                case 'l' -> 'ʟ';
                case 'm' -> 'ᴍ';
                case 'n' -> 'ɴ';
                case 'o' -> 'ᴏ';
                case 'p' -> 'ᴘ';
                case 'q' -> 'ǫ';
                case 'r' -> 'ʀ';
                case 's' -> 'ѕ';
                case 't' -> 'ᴛ';
                case 'u' -> 'ᴜ';
                case 'v' -> 'ᴠ';
                case 'w' -> 'ᴡ';
                case 'x' -> 'x';
                case 'y' -> 'ʏ';
                case 'z' -> 'ᴢ';
                case '0' -> '₀';
                case '1' -> '₁';
                case '2' -> '₂';
                case '3' -> '₃';
                case '4' -> '₄';
                case '5' -> '₅';
                case '6' -> '₆';
                case '7' -> '₇';
                case '8' -> '₈';
                case '9' -> '₉';
                default -> c;
            });
        }
        return sb.toString();
    }
}
