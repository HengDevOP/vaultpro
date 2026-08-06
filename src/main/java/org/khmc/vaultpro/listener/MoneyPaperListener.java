package org.khmc.vaultpro.listener;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.khmc.vaultpro.VaultProPlugin;
import org.khmc.vaultpro.util.ColorUtil;
import org.khmc.vaultpro.util.SoundUtil;

public class MoneyPaperListener implements Listener {

    private final VaultProPlugin plugin;
    private final NamespacedKey valueKey;

    public MoneyPaperListener(VaultProPlugin plugin) {
        this.plugin = plugin;
        this.valueKey = new NamespacedKey(plugin, "money_paper_value");
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public NamespacedKey getValueKey() {
        return valueKey;
    }

    public boolean isMoneyPaper(ItemStack stack) {
        if (stack == null || stack.getType() != Material.PAPER || !stack.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return false;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (pdc.has(valueKey, PersistentDataType.DOUBLE)) {
            return true;
        }
        if (meta.hasDisplayName()) {
            String plainName = PlainTextComponentSerializer.plainText().serialize(meta.displayName());
            if (plainName.equalsIgnoreCase("Money Paper")) {
                return true;
            }
        }
        return false;
    }

    public double getMoneyPaperValue(ItemStack stack) {
        if (stack == null || stack.getType() != Material.PAPER || !stack.hasItemMeta()) {
            return 0.0;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return 0.0;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (pdc.has(valueKey, PersistentDataType.DOUBLE)) {
            Double val = pdc.get(valueKey, PersistentDataType.DOUBLE);
            return val != null ? val : 0.0;
        }
        return 0.0;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAnvilPrepare(PrepareAnvilEvent event) {
        ItemStack first = event.getInventory().getItem(0);
        if (isMoneyPaper(first)) {
            event.setResult(null);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!isMoneyPaper(item)) return;

        event.setCancelled(true);
        double value = getMoneyPaperValue(item);
        if (value <= 0.0) {
            player.sendMessage(ColorUtil.parse("&c✦ " + ColorUtil.toSmallCaps("invalid money paper value.")));
            SoundUtil.playError(player);
            return;
        }

        // Consume 1 paper
        item.setAmount(item.getAmount() - 1);

        // Deposit into player account
        plugin.getVaultService().deposit(player, "money", value);

        // Play Map Art Sound (UI_CARTOGRAPHY_TABLE_TAKE_RESULT)
        player.playSound(player.getLocation(), Sound.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, 1.0f, 1.0f);

        player.sendMessage(ColorUtil.parse("&a✦ &f" + ColorUtil.toSmallCaps("deposited ") + "&e" + plugin.getVaultService().formatCurrency(value) + " &f" + ColorUtil.toSmallCaps("into your account!")));
    }
}
