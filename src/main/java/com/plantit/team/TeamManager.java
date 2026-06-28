package com.plantit.team;

import com.plantit.PlantIt;
import net.kyori.adventure.text.Component;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class TeamManager {

    private final PlantIt plugin;
    private final Map<UUID, CSTeam> playerTeams = new HashMap<>();
    private final Set<UUID> deadThisRound = new HashSet<>();

    public TeamManager(PlantIt plugin) {
        this.plugin = plugin;
    }

    public void assignTeam(Player player) {
        long tCount = playerTeams.values().stream().filter(t -> t == CSTeam.T).count();
        long ctCount = playerTeams.values().stream().filter(t -> t == CSTeam.CT).count();
        CSTeam team = tCount <= ctCount ? CSTeam.T : CSTeam.CT;
        setTeam(player, team);
    }

    public void setTeam(Player player, CSTeam team) {
        playerTeams.put(player.getUniqueId(), team);
        player.sendMessage(Component.text("Team: ")
                .append(Component.text(team.getDisplayName(), team.getColor())));
    }

    public CSTeam getTeam(Player player) {
        return playerTeams.getOrDefault(player.getUniqueId(), CSTeam.SPECTATOR);
    }

    public void markDead(Player player) {
        deadThisRound.add(player.getUniqueId());
        player.setGameMode(GameMode.SPECTATOR);
    }

    public boolean isDead(Player player) {
        return deadThisRound.contains(player.getUniqueId());
    }

    public List<Player> getAlivePlayers(CSTeam team) {
        return playerTeams.entrySet().stream()
                .filter(e -> e.getValue() == team && !deadThisRound.contains(e.getKey()))
                .map(e -> plugin.getServer().getPlayer(e.getKey()))
                .filter(Objects::nonNull)
                .toList();
    }

    public int getTotalActivePlayers() {
        return (int) playerTeams.values().stream()
                .filter(t -> t != CSTeam.SPECTATOR)
                .count();
    }

    /** Clears dead set and restores alive players to SURVIVAL for the new round. */
    public void resetForRound() {
        deadThisRound.clear();
        playerTeams.forEach((uuid, team) -> {
            if (team == CSTeam.SPECTATOR) return;
            Player p = plugin.getServer().getPlayer(uuid);
            if (p != null) p.setGameMode(GameMode.SURVIVAL);
        });
    }

    /** Swaps T/CT assignments at halftime. */
    public void swapTeams() {
        playerTeams.replaceAll((uuid, team) -> switch (team) {
            case T -> CSTeam.CT;
            case CT -> CSTeam.T;
            case SPECTATOR -> CSTeam.SPECTATOR;
        });
    }

    public void removePlayer(Player player) {
        playerTeams.remove(player.getUniqueId());
        deadThisRound.remove(player.getUniqueId());
    }
}
