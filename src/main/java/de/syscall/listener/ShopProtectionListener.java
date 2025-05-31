package de.syscall.listener;

import de.syscall.SlownFinance;
import de.syscall.data.Shop;
import de.syscall.util.ColorUtil;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

public class ShopProtectionListener implements Listener {

    private final SlownFinance plugin;

    public ShopProtectionListener(SlownFinance plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        Shop shop = plugin.getShopManager().getShopAtLocation(block.getLocation());
        if (shop == null) return;

        if (shop.getOwner().equals(player.getUniqueId())) {
            if (player.isSneaking()) {
                plugin.getShopManager().deleteShop(shop.getUniqueId());
                player.sendMessage(ColorUtil.component("&7Shop &6" + shop.getName() + " &7wurde gelöscht!"));
                return;
            } else {
                event.setCancelled(true);
                player.sendMessage(ColorUtil.component("&cHalte Shift und klicke, um den Shop zu löschen!"));
                return;
            }
        }

        if (player.hasPermission("slownfinance.shop.admin")) {
            if (player.isSneaking()) {
                plugin.getShopManager().deleteShop(shop.getUniqueId());
                player.sendMessage(ColorUtil.component("&7Shop &6" + shop.getName() + " &7wurde als Admin gelöscht!"));
                return;
            }
        }

        event.setCancelled(true);
        player.sendMessage(ColorUtil.component("&cDu kannst fremde Shops nicht zerstören!"));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(block -> {
            Shop shop = plugin.getShopManager().getShopAtLocation(block.getLocation());
            return shop != null;
        });
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> {
            Shop shop = plugin.getShopManager().getShopAtLocation(block.getLocation());
            return shop != null;
        });
    }
}