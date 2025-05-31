package de.syscall.listener;

import de.syscall.SlownFinance;
import de.syscall.data.Shop;
import de.syscall.util.ColorUtil;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class ShopClickListener implements Listener {

    private final SlownFinance plugin;

    public ShopClickListener(SlownFinance plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;

        Player player = event.getPlayer();
        Shop shop = plugin.getShopManager().getShopAtLocation(clickedBlock.getLocation());

        if (shop == null) return;

        if (shop.getOwner().equals(player.getUniqueId())) {
            if (player.isSneaking()) {
                return;
            }

            event.setCancelled(true);
            showShopOwnerInfo(player, shop);
            return;
        }

        if (player.hasPermission("slownfinance.shop.admin") && player.isSneaking()) {
            return;
        }

        event.setCancelled(true);

        if (!shop.isActive()) {
            player.sendMessage(ColorUtil.component("&cDieser Shop ist nicht aktiv!"));
            return;
        }

        if (player.isSneaking()) {
            showShopBuyerInfo(player, shop);
            return;
        }

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            if (plugin.getShopManager().buyFromShop(player, shop)) {
                player.sendMessage(ColorUtil.component("&aKauf erfolgreich abgeschlossen!"));
            }
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (plugin.getShopManager().sellToShop(player, shop)) {
                player.sendMessage(ColorUtil.component("&aVerkauf erfolgreich abgeschlossen!"));
            }
        }
    }

    private void showShopOwnerInfo(Player player, Shop shop) {
        player.sendMessage(ColorUtil.component("&6&l◆ Dein Shop: " + shop.getName() + " ◆"));
        player.sendMessage(ColorUtil.component("&7&m──────────────────────────"));
        player.sendMessage(ColorUtil.component("&7Item: &6" + shop.getMaterial().name()));
        player.sendMessage(ColorUtil.component("&7Anzahl pro Kauf: &6" + shop.getAmount()));
        player.sendMessage(ColorUtil.component("&7Kaufpreis: &a" + String.format("%.2f", shop.getBuyPrice()) + " Coins"));

        if (shop.isSellEnabled()) {
            player.sendMessage(ColorUtil.component("&7Verkaufspreis: &c" + String.format("%.2f", shop.getSellPrice()) + " Coins"));
        } else {
            player.sendMessage(ColorUtil.component("&7Verkauf: &cDeaktiviert"));
        }

        player.sendMessage(ColorUtil.component("&7Status: " + (shop.isActive() ? "&aAktiv" : "&cInaktiv")));
        player.sendMessage(ColorUtil.component("&7&m──────────────────────────"));
        player.sendMessage(ColorUtil.component("&7Verwende &6/shop toggle " + shop.getName() + " &7zum Aktivieren/Deaktivieren"));
        player.sendMessage(ColorUtil.component("&7Verwende &6Shift+Rechtsklick &7zum Nachfüllen"));
    }

    private void showShopBuyerInfo(Player player, Shop shop) {
        String ownerName = plugin.getServer().getOfflinePlayer(shop.getOwner()).getName();

        player.sendMessage(ColorUtil.component("&6&l◆ Shop: " + shop.getName() + " ◆"));
        player.sendMessage(ColorUtil.component("&7&m──────────────────────────"));
        player.sendMessage(ColorUtil.component("&7Besitzer: &6" + ownerName));
        player.sendMessage(ColorUtil.component("&7Item: &6" + shop.getMaterial().name()));
        player.sendMessage(ColorUtil.component("&7Anzahl: &6" + shop.getAmount() + "x"));
        player.sendMessage(ColorUtil.component("&7Kaufpreis: &a" + String.format("%.2f", shop.getBuyPrice()) + " Coins"));

        if (shop.isSellEnabled()) {
            player.sendMessage(ColorUtil.component("&7Verkaufspreis: &c" + String.format("%.2f", shop.getSellPrice()) + " Coins"));
        }

        player.sendMessage(ColorUtil.component("&7&m──────────────────────────"));
        player.sendMessage(ColorUtil.component("&7Links klick = Kaufen"));
        if (shop.isSellEnabled()) {
            player.sendMessage(ColorUtil.component("&7Rechts klick = Verkaufen"));
        }
        player.sendMessage(ColorUtil.component("&7Shift+Klick für Details"));
    }
}