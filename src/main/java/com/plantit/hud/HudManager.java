package com.plantit.hud;

import com.plantit.PlantIt;
import com.plantit.round.RoundManager;
import com.plantit.round.RoundPhase;
import com.plantit.team.GameTeam;
import com.plantit.team.TeamManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class HudManager {

    // Each slot is a unique 2-char color code string used as the scoreboard entry name.
    // Color codes with no following text render as blank, so only the team prefix shows.
    private static final String[] SLOTS = {"§0","§1","§2","§3","§4","§5","§6","§7"};

    private final PlantIt plugin;
    private final RoundManager roundManager;
    private final TeamManager teamManager;
    private final Scoreboard scoreboard;
    private final Objective objective;

    public HudManager(PlantIt plugin, RoundManager roundManager, TeamManager teamManager) {
        this.plugin = plugin;
        this.roundManager = roundManager;
        this.teamManager = teamManager;

        scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        objective = scoreboard.registerNewObjective("pi_hud", Criteria.DUMMY,
                Component.text("  PlantIt  ", NamedTextColor.RED));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        for (int i = 0; i < SLOTS.length; i++) {
            Team t = scoreboard.registerNewTeam("pi_line_" + i);
            t.addEntry(SLOTS[i]);
            objective.getScore(SLOTS[i]).setScore(SLOTS.length - i);
        }

        plugin.getServer().getScheduler().runTaskTimer(plugin, this::update, 0L, 10L);
    }

    public void assignScoreboard(Player player) {
        player.setScoreboard(scoreboard);
    }

    public void removePlayer(Player player) {
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    public void update() {
        int tAlive  = teamManager.getAlivePlayers(GameTeam.T).size();
        int ctAlive = teamManager.getAlivePlayers(GameTeam.CT).size();
        int round   = roundManager.getCurrentRound();
        int max     = plugin.getGameConfig().getMaxRounds();
        int tScore  = roundManager.getTScore();
        int ctScore = roundManager.getCtScore();
        RoundPhase phase = roundManager.getPhase();

        setLine(0, Component.text(" Terrorists", NamedTextColor.RED)
                .append(Component.text("  " + tAlive + " alive", NamedTextColor.GRAY)));
        setLine(1, Component.empty());
        setLine(2, Component.text("  " + tScore, NamedTextColor.RED)
                .append(Component.text(" : ", NamedTextColor.GRAY))
                .append(Component.text(ctScore + "  ", NamedTextColor.BLUE)));
        setLine(3, Component.empty());
        setLine(4, Component.text(" Counter-Terrorists", NamedTextColor.BLUE)
                .append(Component.text("  " + ctAlive + " alive", NamedTextColor.GRAY)));
        setLine(5, Component.empty());
        setLine(6, Component.text(" Round ", NamedTextColor.GRAY)
                .append(Component.text(round + "/" + max, NamedTextColor.WHITE)));
        setLine(7, Component.text(" " + phaseLabel(phase), NamedTextColor.YELLOW));
    }

    private void setLine(int index, Component content) {
        scoreboard.getTeam("pi_line_" + index).prefix(content);
    }

    private static String phaseLabel(RoundPhase phase) {
        return switch (phase) {
            case WAITING   -> "Waiting";
            case FREEZE    -> "Buy Phase";
            case LIVE      -> "Live";
            case ROUND_END -> "Round End";
        };
    }
}
