package com.plantit.messaging;

import com.plantit.PlantIt;
import com.plantit.map.MapManager;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.nio.charset.StandardCharsets;
import java.util.Collection;

/**
 * Handles plugin messages on the {@code plantit:queue} channel.
 *
 * Outgoing (game server → proxy):
 *   SLOT_OPEN:<count>     — notify proxy that <count> slots are available
 *
 * Incoming (proxy → game server):
 *   MAP_SELECTED:<name>   — proxy voted map for the next match; load it
 */
public class GameMessenger implements PluginMessageListener {

    public static final String CHANNEL = "plantit:queue";

    private final PlantIt plugin;
    private final MapManager mapManager;

    public GameMessenger(PlantIt plugin, MapManager mapManager) {
        this.plugin = plugin;
        this.mapManager = mapManager;
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL);
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, CHANNEL, this);
    }

    /** Signals to the proxy that the game is ready to receive {@code slotCount} players. */
    public void signalSlotsOpen(int slotCount) {
        Collection<? extends Player> online = plugin.getServer().getOnlinePlayers();
        if (online.isEmpty()) return;
        Player carrier = online.iterator().next();
        carrier.sendPluginMessage(plugin, CHANNEL, ("SLOT_OPEN:" + slotCount).getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals(CHANNEL)) return;
        String msg = new String(message, StandardCharsets.UTF_8).trim();

        if (msg.startsWith("MAP_SELECTED:")) {
            String mapName = msg.substring("MAP_SELECTED:".length()).trim();
            if (!mapName.isEmpty()) {
                mapManager.loadMap(mapName);
                plugin.getLogger().info("Map selected by vote: " + mapName);
            }
        }
    }

    public void shutdown() {
        plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, CHANNEL);
        plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin, CHANNEL, this);
    }
}
