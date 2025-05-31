package de.syscall.manager;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import de.syscall.SlownFinance;
import de.syscall.data.Shop;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.craftbukkit.v1_21_R3.inventory.CraftItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class HologramManager {

    private final SlownFinance plugin;
    private final ProtocolManager protocolManager;
    private final Map<String, Set<Integer>> shopHolograms;
    private final Map<String, Set<Integer>> shopItems;
    private final Map<Integer, Float> itemRotations;
    private int nextEntityId;

    private double itemHeight;
    private double priceHeight;
    private double ownerHeight;
    private double sellHeight;
    private float rotationSpeed;
    private int rotationInterval;
    private int viewDistanceSquared;

    public HologramManager(SlownFinance plugin) {
        this.plugin = plugin;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
        this.shopHolograms = new ConcurrentHashMap<>();
        this.shopItems = new ConcurrentHashMap<>();
        this.itemRotations = new ConcurrentHashMap<>();
        this.nextEntityId = 100000;
        loadConfig();
        startRotationTask();
    }

    private void loadConfig() {
        plugin.reloadConfig();
        this.itemHeight = plugin.getConfig().getDouble("shop.hologram.item-height", 1.0);
        this.priceHeight = plugin.getConfig().getDouble("shop.hologram.price-height", 0.5);
        this.ownerHeight = plugin.getConfig().getDouble("shop.hologram.owner-height", 0.8);
        this.sellHeight = plugin.getConfig().getDouble("shop.hologram.sell-height", 1.5);
        this.rotationSpeed = (float) plugin.getConfig().getDouble("shop.hologram.rotation-speed", 1.5);
        this.rotationInterval = plugin.getConfig().getInt("shop.hologram.rotation-interval", 2);
        int viewDistance = plugin.getConfig().getInt("shop.hologram.view-distance", 100);
        this.viewDistanceSquared = viewDistance * viewDistance;
    }

    private void startRotationTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                updateItemRotations();
            }
        }.runTaskTimer(plugin, 0L, rotationInterval);
    }

    private void updateItemRotations() {
        for (Map.Entry<Integer, Float> entry : itemRotations.entrySet()) {
            int entityId = entry.getKey();
            float newRotation = entry.getValue() + rotationSpeed;
            if (newRotation >= 360.0f) {
                newRotation = 0.0f;
            }
            itemRotations.put(entityId, newRotation);

            for (Player player : plugin.getServer().getOnlinePlayers()) {
                sendRotationUpdate(player, entityId, newRotation);
            }
        }
    }

    public void createShopHologram(Shop shop) {
        String shopId = shop.getUniqueId();
        Location loc = shop.getHologramLocation();

        Set<Integer> hologramIds = new HashSet<>();
        Set<Integer> itemIds = new HashSet<>();

        int buyHologramId = nextEntityId++;
        int sellHologramId = nextEntityId++;
        int ownerHologramId = nextEntityId++;
        int itemEntityId = nextEntityId++;

        hologramIds.add(buyHologramId);
        if (shop.isSellEnabled()) {
            hologramIds.add(sellHologramId);
        }
        hologramIds.add(ownerHologramId);
        itemIds.add(itemEntityId);

        shopHolograms.put(shopId, hologramIds);
        shopItems.put(shopId, itemIds);
        itemRotations.put(itemEntityId, 0.0f);

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (player.getLocation().distanceSquared(loc) < viewDistanceSquared) {
                sendHologramPackets(player, shop, buyHologramId, sellHologramId, ownerHologramId, itemEntityId);
            }
        }
    }

    public void updateShopHologram(Shop shop) {
        removeShopHologram(shop.getUniqueId());
        createShopHologram(shop);
    }

    private void sendHologramPackets(Player player, Shop shop, int buyId, int sellId, int ownerId, int itemId) {
        Location chestLoc = shop.getChestLocation();
        Location centerLoc = chestLoc.clone().add(0.5, 0, 0.5);

        String buyFormat = plugin.getConfig().getString("shop.display.buy-format", "§aBuy: §6{amount}x §7für §a{price} Coins");
        String sellFormat = plugin.getConfig().getString("shop.display.sell-format", "§cSell: §6{amount}x §7für §c{price} Coins");
        String ownerFormat = plugin.getConfig().getString("shop.display.owner-format", "§7Shop von §6{owner}");
        String currency = plugin.getConfig().getString("shop.display.currency-symbol", "Coins");
        int decimalPlaces = plugin.getConfig().getInt("shop.display.decimal-places", 2);

        String buyText = buyFormat
                .replace("{amount}", String.valueOf(shop.getAmount()))
                .replace("{price}", String.format("%." + decimalPlaces + "f", shop.getBuyPrice()))
                .replace("{currency}", currency);

        String ownerText = ownerFormat
                .replace("{owner}", plugin.getServer().getOfflinePlayer(shop.getOwner()).getName());

        sendTextHologram(player, buyId, centerLoc.clone().add(0, priceHeight, 0), buyText);
        sendTextHologram(player, ownerId, centerLoc.clone().add(0, ownerHeight, 0), ownerText);

        if (shop.isSellEnabled()) {
            String sellText = sellFormat
                    .replace("{amount}", String.valueOf(shop.getAmount()))
                    .replace("{price}", String.format("%." + decimalPlaces + "f", shop.getSellPrice()))
                    .replace("{currency}", currency);
            sendTextHologram(player, sellId, centerLoc.clone().add(0, sellHeight, 0), sellText);
        }

        sendItemHologram(player, itemId, centerLoc.clone().add(0, itemHeight, 0), shop.getItem());
    }

    private void sendTextHologram(Player player, int entityId, Location location, String text) {
        try {
            PacketContainer spawnPacket = protocolManager.createPacket(PacketType.Play.Server.SPAWN_ENTITY);
            spawnPacket.getIntegers().write(0, entityId);
            spawnPacket.getUUIDs().write(0, UUID.randomUUID());
            spawnPacket.getEntityTypeModifier().write(0, EntityType.ARMOR_STAND);
            spawnPacket.getDoubles().write(0, location.getX());
            spawnPacket.getDoubles().write(1, location.getY());
            spawnPacket.getDoubles().write(2, location.getZ());

            protocolManager.sendServerPacket(player, spawnPacket);

            PacketContainer metadataPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_METADATA);
            metadataPacket.getIntegers().write(0, entityId);

            List<WrappedDataValue> dataValues = new ArrayList<>();

            dataValues.add(new WrappedDataValue(0, WrappedDataWatcher.Registry.get(Byte.class), (byte) 0x20));
            dataValues.add(new WrappedDataValue(2, WrappedDataWatcher.Registry.getChatComponentSerializer(true),
                    Optional.of(WrappedChatComponent.fromText(text).getHandle())));
            dataValues.add(new WrappedDataValue(3, WrappedDataWatcher.Registry.get(Boolean.class), true));
            dataValues.add(new WrappedDataValue(5, WrappedDataWatcher.Registry.get(Boolean.class), true));
            dataValues.add(new WrappedDataValue(15, WrappedDataWatcher.Registry.get(Byte.class), (byte) 0x01));

            metadataPacket.getDataValueCollectionModifier().write(0, dataValues);

            protocolManager.sendServerPacket(player, metadataPacket);

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to send text hologram: " + e.getMessage());
        }
    }

    private void sendItemHologram(Player player, int entityId, Location location, ItemStack item) {
        try {
            PacketContainer spawnPacket = protocolManager.createPacket(PacketType.Play.Server.SPAWN_ENTITY);
            spawnPacket.getIntegers().write(0, entityId);
            spawnPacket.getUUIDs().write(0, UUID.randomUUID());
            spawnPacket.getEntityTypeModifier().write(0, EntityType.ITEM);
            spawnPacket.getDoubles().write(0, location.getX());
            spawnPacket.getDoubles().write(1, location.getY());
            spawnPacket.getDoubles().write(2, location.getZ());

            protocolManager.sendServerPacket(player, spawnPacket);

            PacketContainer metadataPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_METADATA);
            metadataPacket.getIntegers().write(0, entityId);

            List<WrappedDataValue> dataValues = new ArrayList<>();

            ItemStack singleItem = item.clone();
            singleItem.setAmount(1);
            net.minecraft.world.item.ItemStack nmsItem = CraftItemStack.asNMSCopy(singleItem);

            dataValues.add(new WrappedDataValue(0, WrappedDataWatcher.Registry.get(Byte.class), (byte) 0x20));
            dataValues.add(new WrappedDataValue(5, WrappedDataWatcher.Registry.get(Boolean.class), true));
            dataValues.add(new WrappedDataValue(8, WrappedDataWatcher.Registry.getItemStackSerializer(false), nmsItem));

            metadataPacket.getDataValueCollectionModifier().write(0, dataValues);

            protocolManager.sendServerPacket(player, metadataPacket);

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to send item hologram: " + e.getMessage());
        }
    }

    private void sendRotationUpdate(Player player, int entityId, float yaw) {
        try {
            PacketContainer rotationPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_LOOK);
            rotationPacket.getIntegers().write(0, entityId);
            rotationPacket.getBytes().write(0, (byte) (yaw * 256.0F / 360.0F));
            rotationPacket.getBytes().write(1, (byte) 0);
            rotationPacket.getBooleans().write(0, false);

            protocolManager.sendServerPacket(player, rotationPacket);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to send rotation update: " + e.getMessage());
        }
    }

    public void removeShopHologram(String shopId) {
        Set<Integer> hologramIds = shopHolograms.remove(shopId);
        Set<Integer> itemIds = shopItems.remove(shopId);

        if (hologramIds != null) {
            for (int entityId : hologramIds) {
                removeEntityForAllPlayers(entityId);
            }
        }

        if (itemIds != null) {
            for (int entityId : itemIds) {
                removeEntityForAllPlayers(entityId);
                itemRotations.remove(entityId);
            }
        }
    }

    private void removeEntityForAllPlayers(int entityId) {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            try {
                PacketContainer destroyPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
                destroyPacket.getIntLists().write(0, List.of(entityId));
                protocolManager.sendServerPacket(player, destroyPacket);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to remove entity: " + e.getMessage());
            }
        }
    }

    public void updatePlayerHolograms(Player player) {
        for (Shop shop : plugin.getShopManager().getAllShops()) {
            if (player.getLocation().distanceSquared(shop.getHologramLocation()) < viewDistanceSquared) {
                String shopId = shop.getUniqueId();
                Set<Integer> hologramIds = shopHolograms.get(shopId);
                Set<Integer> itemIds = shopItems.get(shopId);

                if (hologramIds != null && itemIds != null && !itemIds.isEmpty()) {
                    Integer[] hIds = hologramIds.toArray(new Integer[0]);
                    Integer[] iIds = itemIds.toArray(new Integer[0]);

                    if (hIds.length >= 2) {
                        int sellId = shop.isSellEnabled() && hIds.length >= 3 ? hIds[1] : -1;
                        int ownerId = shop.isSellEnabled() && hIds.length >= 3 ? hIds[2] : hIds[1];
                        sendHologramPackets(player, shop, hIds[0], sellId, ownerId, iIds[0]);
                    }
                }
            }
        }
    }

    public void reload() {
        loadConfig();
    }

    public void cleanup() {
        for (Set<Integer> entityIds : shopHolograms.values()) {
            for (int entityId : entityIds) {
                removeEntityForAllPlayers(entityId);
            }
        }
        for (Set<Integer> entityIds : shopItems.values()) {
            for (int entityId : entityIds) {
                removeEntityForAllPlayers(entityId);
            }
        }
        shopHolograms.clear();
        shopItems.clear();
        itemRotations.clear();
    }
}