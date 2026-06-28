package com.plantit.round;

import com.plantit.PlantIt;
import com.plantit.team.CSTeam;
import com.plantit.team.TeamManager;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class RoundListener implements Listener {

    private final PlantIt plugin;
    private final RoundManager roundManager;
    private final TeamManager teamManager;

    public RoundListener(PlantIt plugin, RoundManager roundManager, TeamManager teamManager) {
        this.plugin = plugin;
        this.roundManager = roundManager;
        this.teamManager = teamManager;
    }

    /** Block XZ movement during freeze; allow looking around. */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (roundManager.getPhase() != RoundPhase.FREEZE) return;
        if (teamManager.getTeam(event.getPlayer()) == CSTeam.SPECTATOR) return;

        Location from = event.getFrom();
        Location to = event.getTo();

        if (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ()) {
            Location cancelled = from.clone();
            cancelled.setYaw(to.getYaw());
            cancelled.setPitch(to.getPitch());
            event.setTo(cancelled);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (roundManager.getPhase() != RoundPhase.LIVE) return;

        Player dead = event.getEntity();
        event.setKeepInventory(false);
        event.setKeepLevel(true);
        event.getDrops().clear(); // weapon drop logic added later

        teamManager.markDead(dead);

        // Defer by 1 tick so death is fully processed before we end the round
        plugin.getServer().getScheduler().runTaskLater(plugin,
                roundManager::checkEliminationWin, 1L);
    }

    /** After respawn, keep dead players in spectator mode. */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (!teamManager.isDead(player)) return;

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (teamManager.isDead(player)) {
                player.setGameMode(GameMode.SPECTATOR);
            }
        }, 1L);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (roundManager.getPhase() == RoundPhase.WAITING) {
            teamManager.assignTeam(player);
            roundManager.tryStartRound();
        } else {
            // Mid-round join → spectator until next round
            teamManager.setTeam(player, CSTeam.SPECTATOR);
            player.setGameMode(GameMode.SPECTATOR);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        teamManager.removePlayer(event.getPlayer());
        // Re-check in case removing this player leaves a team empty
        roundManager.checkEliminationWin();
    }
}
