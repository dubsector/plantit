package com.plantit.economy;

import com.plantit.PlantIt;
import com.plantit.team.GameTeam;
import com.plantit.team.TeamManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EconomyManager {

    public static final int MAX_MONEY        = 16000;
    public static final int STARTING_MONEY   = 800;
    public static final int OT_STARTING_MONEY = 10000;
    public static final int WIN_BONUS        = 3250;
    public static final int LOSS_BONUS_BASE  = 1400;
    public static final int LOSS_INCREMENT   = 500;
    public static final int LOSS_BONUS_MAX   = 3400;
    public static final int PLANT_BONUS      = 300;
    public static final int DEFUSE_BONUS     = 300;
    public static final int KILL_BONUS       = 300;
    public static final int DEFUSE_KIT_COST  = 400;

    private final PlantIt plugin;
    private final TeamManager teamManager;

    private final Map<UUID, Integer> money   = new HashMap<>();
    private final Map<UUID, Boolean> hasKit  = new HashMap<>();
    private int tLossStreak  = 0;
    private int ctLossStreak = 0;

    public EconomyManager(PlantIt plugin, TeamManager teamManager) {
        this.plugin = plugin;
        this.teamManager = teamManager;
    }

    public void initMatch() {
        money.clear();
        hasKit.clear();
        tLossStreak = 0;
        ctLossStreak = 0;
        plugin.getServer().getOnlinePlayers().forEach(p -> setMoney(p, STARTING_MONEY));
    }

    public void initOvertimeHalf() {
        plugin.getServer().getOnlinePlayers().forEach(p -> setMoney(p, OT_STARTING_MONEY));
        hasKit.clear();
    }

    public void ensurePlayer(Player player) {
        if (!money.containsKey(player.getUniqueId())) setMoney(player, STARTING_MONEY);
    }

    public void onRoundEnd(GameTeam winner) {
        if (winner == GameTeam.T) { tLossStreak = 0; ctLossStreak++; }
        else                      { ctLossStreak = 0; tLossStreak++;  }

        plugin.getServer().getOnlinePlayers().forEach(p -> {
            GameTeam team = teamManager.getTeam(p);
            if (team == GameTeam.SPECTATOR) return;
            if (team == winner) addMoney(p, WIN_BONUS);
            else                addMoney(p, lossBonus(team == GameTeam.T ? tLossStreak : ctLossStreak));
        });

        // Reset kits at round end (kit is per-round)
        hasKit.clear();
    }

    public void onBombPlanted() {
        teamManager.getAlivePlayers(GameTeam.T).forEach(p -> addMoney(p, PLANT_BONUS));
    }

    public void onBombDefused() {
        teamManager.getAlivePlayers(GameTeam.CT).forEach(p -> addMoney(p, DEFUSE_BONUS));
    }

    public void onKill(Player killer, int bonus) {
        addMoney(killer, bonus);
    }

    public boolean buyKit(Player player) {
        if (teamManager.getTeam(player) != GameTeam.CT) return false;
        if (hasKit(player)) { player.sendMessage(Component.text("You already have a defuse kit.", NamedTextColor.YELLOW)); return false; }
        if (!spend(player, DEFUSE_KIT_COST)) { player.sendMessage(Component.text("Not enough money.", NamedTextColor.RED)); return false; }
        hasKit.put(player.getUniqueId(), true);
        player.sendMessage(Component.text("Defuse kit purchased.", NamedTextColor.AQUA));
        return true;
    }

    public boolean hasKit(Player player) {
        return hasKit.getOrDefault(player.getUniqueId(), false);
    }

    public boolean spend(Player player, int cost) {
        if (getMoney(player) < cost) return false;
        setMoney(player, getMoney(player) - cost);
        return true;
    }

    public void addMoney(Player player, int amount) {
        setMoney(player, Math.min(getMoney(player) + amount, MAX_MONEY));
        if (amount > 0) player.sendMessage(Component.text("+ $" + amount, NamedTextColor.GREEN));
    }

    public void setMoney(Player player, int amount) {
        int clamped = Math.max(0, Math.min(amount, MAX_MONEY));
        money.put(player.getUniqueId(), clamped);
        player.setLevel(clamped);
        player.setExp(0f);
    }

    public int getMoney(Player player) {
        return money.getOrDefault(player.getUniqueId(), STARTING_MONEY);
    }

    public void removePlayer(Player player) {
        money.remove(player.getUniqueId());
        hasKit.remove(player.getUniqueId());
    }

    private static int lossBonus(int streak) {
        return Math.min(LOSS_BONUS_BASE + Math.max(0, streak - 1) * LOSS_INCREMENT, LOSS_BONUS_MAX);
    }

    public int getTLossStreak()  { return tLossStreak; }
    public int getCtLossStreak() { return ctLossStreak; }
}
