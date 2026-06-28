package com.plantit.messaging;

import com.plantit.PlantIt;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageRecipient;

import java.nio.charset.StandardCharsets;
import java.util.Collection;

/**
 * Sends plugin messages to the Velocity proxy over the {@code plantit:queue} channel.
 *
 * Protocol (UTF-8 string):
 *   SLOT_OPEN:<count>   — notify the proxy that <count> player slots are available
 */
public class GameMessenger {

    public static final String CHANNEL = "plantit:queue";

    private final PlantIt plugin;

    public GameMessenger(PlantIt plugin) {
        this.plugin = plugin;
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL);
    }

    /**
     * Signals to the proxy that the game is ready to receive {@code slotCount} players.
     * Plugin messages must be sent via a carrier player; picks the first online player.
     */
    public void signalSlotsOpen(int slotCount) {
        Collection<? extends Player> online = plugin.getServer().getOnlinePlayers();
        if (online.isEmpty()) return;

        Player carrier = online.iterator().next();
        byte[] data = ("SLOT_OPEN:" + slotCount).getBytes(StandardCharsets.UTF_8);
        carrier.sendPluginMessage(plugin, CHANNEL, data);
    }

    public void shutdown() {
        plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, CHANNEL);
    }
}
