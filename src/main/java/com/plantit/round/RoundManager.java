package com.plantit.round;

import com.plantit.PlantIt;
import com.plantit.bomb.BombManager;
import com.plantit.bomb.BombState;
import com.plantit.config.GameConfig;
import com.plantit.economy.EconomyManager;
import com.plantit.map.MapManager;
import com.plantit.messaging.GameMessenger;
import com.plantit.team.GameTeam;
import com.plantit.team.TeamManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.List;

public class RoundManager {

    private final PlantIt plugin;
    private final TeamManager teamManager;
    private final GameConfig config;
    private final GameMessenger messenger;
    private final MapManager mapManager;
    private final EconomyManager economyManager;
    private final BombManager bombManager;

    private RoundPhase phase = RoundPhase.WAITING;
    private int currentRound = 0;
    private int tScore = 0;
    private int ctScore = 0;
    private int phaseTimeLeft = 0;

    private BukkitTask tickTask;

    public RoundManager(PlantIt plugin, TeamManager teamManager, GameConfig config,
                        GameMessenger messenger, MapManager mapManager,
                        EconomyManager economyManager, BombManager bombManager) {
        this.plugin = plugin;
        this.teamManager = teamManager;
        this.config = config;
        this.messenger = messenger;
        this.mapManager = mapManager;
        this.economyManager = economyManager;
        this.bombManager = bombManager;
    }

    public void tryStartRound() {
        if (phase != RoundPhase.WAITING) return;
        if (teamManager.getTotalActivePlayers() < config.getMinPlayers()) return;
        startRound();
    }

    public void startRound() {
        currentRound++;

        // Initialize economy on first round of a new match
        if (currentRound == 1) economyManager.initMatch();

        // Halftime swap
        int half = config.getMaxRounds() / 2;
        if (currentRound == half + 1) {
            teamManager.swapTeams();
            broadcastTitle("Halftime", "Teams have swapped", NamedTextColor.YELLOW);
        }

        teamManager.resetForRound();

        // Teleport players to random positions within their spawn region
        for (Player p : teamManager.getAlivePlayers(GameTeam.T)) {
            Location spawn = mapManager.getRandomTSpawn(p.getWorld());
            if (spawn != null) p.teleport(spawn);
        }
        for (Player p : teamManager.getAlivePlayers(GameTeam.CT)) {
            Location spawn = mapManager.getRandomCtSpawn(p.getWorld());
            if (spawn != null) p.teleport(spawn);
        }

        // Give bomb to a random T player
        List<Player> tPlayers = teamManager.getAlivePlayers(GameTeam.T);
        bombManager.onRoundStart(tPlayers);

        // Ensure new players have starting money
        economyManager.onRoundStart();

        transitionTo(RoundPhase.FREEZE);
    }

    private void transitionTo(RoundPhase newPhase) {
        cancelTick();
        this.phase = newPhase;
        switch (newPhase) {
            case FREEZE -> startFreezePhase();
            case LIVE   -> startLivePhase();
            default     -> { }
        }
    }

    private void startFreezePhase() {
        phaseTimeLeft = config.getFreezeDuration();
        broadcastTitle("Round " + currentRound, "Buy your equipment!", NamedTextColor.WHITE);

        tickTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (phaseTimeLeft <= 0) { transitionTo(RoundPhase.LIVE); return; }
            broadcastActionBar(Component.text("Buy Phase  ")
                    .append(Component.text(phaseTimeLeft + "s", NamedTextColor.YELLOW))
                    .append(Component.text("  | /plantit buy kit ($400)", NamedTextColor.GRAY)));
            phaseTimeLeft--;
        }, 0L, 20L);
    }

    private void startLivePhase() {
        phaseTimeLeft = config.getRoundDuration();
        broadcastTitle("", "GO!", NamedTextColor.GREEN);

        tickTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            BombState bombState = bombManager.getState();
            boolean bombActive = bombState == BombState.PLANTED || bombState == BombState.DEFUSING;

            if (phaseTimeLeft <= 0 && !bombActive) {
                endRound(RoundEndReason.TIME_EXPIRED);
                return;
            }

            Component bar;
            if (bombActive) {
                int bombSecs = bombManager.getBombTimeLeft() / 20;
                bar = Component.text("BOMB " + bombManager.getPlantedSite() + "  ", NamedTextColor.RED)
                        .append(Component.text(formatTime(bombSecs), NamedTextColor.YELLOW))
                        .append(Component.text("  T ", NamedTextColor.RED))
                        .append(Component.text(tScore + " : " + ctScore, NamedTextColor.WHITE))
                        .append(Component.text(" CT", NamedTextColor.BLUE));
            } else {
                bar = Component.text("Round " + currentRound + "  ")
                        .append(Component.text(formatTime(phaseTimeLeft), NamedTextColor.WHITE))
                        .append(Component.text("  T ", NamedTextColor.RED))
                        .append(Component.text(tScore + " : " + ctScore, NamedTextColor.WHITE))
                        .append(Component.text(" CT", NamedTextColor.BLUE));
            }
            broadcastActionBar(bar);
            if (!bombActive) phaseTimeLeft--;
        }, 0L, 20L);
    }

    public void endRound(RoundEndReason reason) {
        if (phase == RoundPhase.ROUND_END || phase == RoundPhase.WAITING) return;
        cancelTick();
        bombManager.reset();
        phase = RoundPhase.ROUND_END;

        GameTeam winner = reason.isTWin() ? GameTeam.T : GameTeam.CT;
        if (reason.isTWin()) tScore++; else ctScore++;

        economyManager.onRoundEnd(winner);

        broadcastTitle(reason.getMessage(),
                "T  " + tScore + " : " + ctScore + "  CT",
                reason.getColor());

        if (isMatchOver()) {
            plugin.getServer().getScheduler().runTaskLater(plugin, this::endMatch,
                    config.getRoundEndDelay() * 20L);
        } else {
            plugin.getServer().getScheduler().runTaskLater(plugin, this::startRound,
                    config.getRoundEndDelay() * 20L);
        }
    }

    /** Called after every death — checks whether either team has been wiped. */
    public void checkEliminationWin() {
        if (phase != RoundPhase.LIVE) return;

        boolean tAlive  = !teamManager.getAlivePlayers(GameTeam.T).isEmpty();
        boolean ctAlive = !teamManager.getAlivePlayers(GameTeam.CT).isEmpty();

        if (!tAlive)  endRound(RoundEndReason.T_ELIMINATED);
        else if (!ctAlive) endRound(RoundEndReason.CT_ELIMINATED);
    }

    private boolean isMatchOver() {
        int winsNeeded = config.getMaxRounds() / 2 + 1;
        return tScore >= winsNeeded || ctScore >= winsNeeded || currentRound >= config.getMaxRounds();
    }

    private void endMatch() {
        boolean draw = tScore == ctScore;
        boolean tWon = tScore > ctScore;
        broadcastTitle(
                draw ? "Draw!" : (tWon ? "Terrorists Win!" : "Counter-Terrorists Win!"),
                "Final: T " + tScore + " : " + ctScore + " CT",
                draw ? NamedTextColor.YELLOW : (tWon ? NamedTextColor.RED : NamedTextColor.BLUE));

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            phase = RoundPhase.WAITING;
            currentRound = 0;
            tScore = 0;
            ctScore = 0;
            messenger.signalSlotsOpen(plugin.getGameConfig().getMinPlayers());
        }, 100L);
    }

    public void shutdown() {
        cancelTick();
    }

    // -------------------------------------------------------------------------

    private void cancelTick() {
        if (tickTask != null) { tickTask.cancel(); tickTask = null; }
    }

    private void broadcastTitle(String title, String subtitle, TextColor color) {
        Title t = Title.title(
                Component.text(title, color),
                Component.text(subtitle, NamedTextColor.GRAY),
                Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(2500), Duration.ofMillis(500)));
        plugin.getServer().getOnlinePlayers().forEach(p -> p.showTitle(t));
    }

    private void broadcastActionBar(Component message) {
        plugin.getServer().getOnlinePlayers().forEach(p -> p.sendActionBar(message));
    }

    private static String formatTime(int seconds) {
        return (seconds / 60) + ":" + String.format("%02d", seconds % 60);
    }

    // -------------------------------------------------------------------------

    public RoundPhase getPhase()     { return phase; }
    public int getCurrentRound()     { return currentRound; }
    public int getTScore()           { return tScore; }
    public int getCtScore()          { return ctScore; }
    public int getPhaseTimeLeft()    { return phaseTimeLeft; }
}
