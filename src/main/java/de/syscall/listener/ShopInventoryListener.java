package de.syscall.listener;

import de.syscall.SlownFinance;
import de.syscall.data.Shop;
import de.syscall.util.ColorUtil;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ShopInventoryListener implements Listener {

    private final SlownFinance plugin;
    private final Map<UUID, Shop> editingShops;

    public ShopInventoryListener(SlownFinance plugin) {
        this.plugin = plugin;
        this.editingShops = new HashMap<>();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!event.getPlayer().isSneaking()) return;

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;

        Player player = event.getPlayer();
        Shop shop = plugin.getShopManager().getShopAtLocation(clickedBlock.getLocation());

        if (shop == null) return;

        if (!shop.getOwner().equals(player.getUniqueId()) &&
                !player.hasPermission("slownfinance.shop.admin")) {
            return;
        }

        event.setCancelled(true);
        editingShops.put(player.getUniqueId(), shop);

        player.openInventory(getChestInventory(clickedBlock));
        player.sendMessage(ColorUtil.component("&7Du kannst nur &6" + shop.getMaterial().name() + " &7hinzufügen!"));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Shop shop = editingShops.get(player.getUniqueId());
        if (shop == null) return;

        ItemStack clickedItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();

        if (isValidShopInteraction(event, shop, clickedItem, cursorItem)) {
            return;
        }

        event.setCancelled(true);
        player.sendMessage(ColorUtil.component("&cDu kannst nur &6" + shop.getMaterial().name() + " &cin diese Shop-Kiste legen!"));
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        Shop shop = editingShops.remove(player.getUniqueId());
        if (shop == null) return;

        player.sendMessage(ColorUtil.component("&aShop-Inventar geschlossen!"));
    }

    private boolean isValidShopInteraction(InventoryClickEvent event, Shop shop, ItemStack clickedItem, ItemStack cursorItem) {
        Material shopMaterial = shop.getMaterial();

        if (event.isShiftClick()) {
            return clickedItem != null && clickedItem.getType() == shopMaterial;
        } else {
            if (cursorItem != null && cursorItem.getType() != Material.AIR) {
                return cursorItem.getType() == shopMaterial;
            } else if (clickedItem != null && clickedItem.getType() == shopMaterial) {
                return true;
            } else return cursorItem == null || cursorItem.getType() == Material.AIR;
        }
    }

    private Inventory getChestInventory(Block chestBlock) {
        if (chestBlock.getState() instanceof org.bukkit.block.Chest chest) {
            return chest.getInventory();
        }
        return null;
    }
}