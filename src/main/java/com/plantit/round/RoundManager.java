package com.plantit.round;

import com.plantit.PlantIt;
import com.plantit.config.GameConfig;
import com.plantit.messaging.GameMessenger;
import com.plantit.team.GameTeam;
import com.plantit.team.TeamManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;

public class RoundManager {

    private final PlantIt plugin;
    private final TeamManager teamManager;
    private final GameConfig config;
    private final GameMessenger messenger;

    private RoundPhase phase = RoundPhase.WAITING;
    private int currentRound = 0;
    private int tScore = 0;
    private int ctScore = 0;
    private int phaseTimeLeft = 0;
    private boolean inOvertime = false;

    private BukkitTask tickTask;

    public RoundManager(PlantIt plugin, TeamManager teamManager, GameConfig config, GameMessenger messenger) {
        this.plugin = plugin;
        this.teamManager = teamManager;
        this.config = config;
        this.messenger = messenger;
    }

    public void tryStartRound() {
        if (phase != RoundPhase.WAITING) return;
        if (teamManager.getTotalActivePlayers() < config.getMinPlayers()) return;
        startRound();
    }

    public void startRound() {
        currentRound++;

        // Halftime swap at round (maxRounds / 2) + 1
        int half = config.getMaxRounds() / 2;
        if (currentRound == half + 1 && !inOvertime) {
            teamManager.swapTeams();
            broadcastTitle("Halftime", "Teams have swapped", NamedTextColor.YELLOW);
        }

        teamManager.resetForRound();
        transitionTo(RoundPhase.FREEZE);
    }

    private void transitionTo(RoundPhase newPhase) {
        cancelTick();
        this.phase = newPhase;

        switch (newPhase) {
            case FREEZE -> startFreezePhase();
            case LIVE -> startLivePhase();
            default -> { }
        }
    }

    private void startFreezePhase() {
        phaseTimeLeft = config.getFreezeDuration();
        broadcastTitle("Round " + currentRound, "Buy your equipment!", NamedTextColor.WHITE);

        tickTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (phaseTimeLeft <= 0) {
                transitionTo(RoundPhase.LIVE);
                return;
            }
            broadcastActionBar(Component.text("Buy Phase  ")
                    .append(Component.text(phaseTimeLeft + "s", NamedTextColor.YELLOW)));
            phaseTimeLeft--;
        }, 0L, 20L);
    }

    private void startLivePhase() {
        phaseTimeLeft = config.getRoundDuration();
        broadcastTitle("", "GO!", NamedTextColor.GREEN);

        tickTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (phaseTimeLeft <= 0) {
                endRound(RoundEndReason.TIME_EXPIRED);
                return;
            }
            broadcastActionBar(Component.text("Round " + currentRound + "  ")
                    .append(Component.text(formatTime(phaseTimeLeft), NamedTextColor.WHITE))
                    .append(Component.text("  T ", NamedTextColor.RED))
                    .append(Component.text(tScore + " : " + ctScore, NamedTextColor.WHITE))
                    .append(Component.text(" CT", NamedTextColor.BLUE)));
            phaseTimeLeft--;
        }, 0L, 20L);
    }

    public void endRound(RoundEndReason reason) {
        cancelTick();
        phase = RoundPhase.ROUND_END;

        if (reason.isTWin()) {
            tScore++;
        } else {
            ctScore++;
        }

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

        boolean tAlive = !teamManager.getAlivePlayers(GameTeam.T).isEmpty();
        boolean ctAlive = !teamManager.getAlivePlayers(GameTeam.CT).isEmpty();

        if (!tAlive) endRound(RoundEndReason.T_ELIMINATED);
        else if (!ctAlive) endRound(RoundEndReason.CT_ELIMINATED);
    }

    private boolean isMatchOver() {
        int winsNeeded = config.getMaxRounds() / 2 + 1;
        if (!inOvertime) {
            return tScore >= winsNeeded || ctScore >= winsNeeded;
        }
        // Overtime: first team to win an OT half by 2 wins
        return Math.abs(tScore - ctScore) >= 2;
    }

    private void endMatch() {
        boolean tWon = tScore > ctScore;
        broadcastTitle(
                tWon ? "Terrorists Win!" : "Counter-Terrorists Win!",
                "Final: T " + tScore + " : " + ctScore + " CT",
                tWon ? NamedTextColor.RED : NamedTextColor.BLUE);

        // Reset and signal the proxy that this server has a slot open
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            phase = RoundPhase.WAITING;
            currentRound = 0;
            tScore = 0;
            ctScore = 0;
            inOvertime = false;
            messenger.signalSlotsOpen(plugin.getGameConfig().getMinPlayers());
        }, 100L);
    }

    public void shutdown() {
        cancelTick();
    }

    // -------------------------------------------------------------------------

    private void cancelTick() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
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

    public RoundPhase getPhase() {
        return phase;
    }

    public int getCurrentRound() {
        return currentRound;
    }

    public int getTScore() {
        return tScore;
    }

    public int getCtScore() {
        return ctScore;
    }

    public int getPhaseTimeLeft() {
        return phaseTimeLeft;
    }
}
