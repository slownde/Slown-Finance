package de.syscall.listener;

import de.syscall.SlownFinance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerMoveListener implements Listener {

    private final SlownFinance plugin;
    private final Map<UUID, Long> lastHologramUpdate;

    public PlayerMoveListener(SlownFinance plugin) {
        this.plugin = plugin;
        this.lastHologramUpdate = new HashMap<>();
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
                event.getFrom().getBlockY() == event.getTo().getBlockY() &&
                event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        long lastUpdate = lastHologramUpdate.getOrDefault(playerId, 0L);

        if (currentTime - lastUpdate < 1000) {
            return;
        }

        lastHologramUpdate.put(playerId, currentTime);

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getHologramManager().updatePlayerHolograms(player);
        });
    }
}