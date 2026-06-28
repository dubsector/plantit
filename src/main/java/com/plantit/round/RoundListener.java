package com.plantit.round;

import com.plantit.PlantIt;
import com.plantit.bomb.BombManager;
import com.plantit.economy.EconomyManager;
import com.plantit.hud.HudManager;
import com.plantit.team.GameTeam;
import com.plantit.team.TeamManager;
import com.plantit.weapon.Weapon;
import com.plantit.weapon.WeaponManager;
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
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class RoundListener implements Listener {

    private final PlantIt plugin;
    private final RoundManager roundManager;
    private final TeamManager teamManager;
    private final EconomyManager economyManager;
    private final BombManager bombManager;
    private final HudManager hudManager;
    private final WeaponManager weaponManager;

    public RoundListener(PlantIt plugin, RoundManager roundManager, TeamManager teamManager,
                         EconomyManager economyManager, BombManager bombManager,
                         HudManager hudManager, WeaponManager weaponManager) {
        this.plugin = plugin;
        this.roundManager = roundManager;
        this.teamManager = teamManager;
        this.economyManager = economyManager;
        this.bombManager = bombManager;
        this.hudManager = hudManager;
        this.weaponManager = weaponManager;
    }

    /** Block XZ movement during freeze; allow looking around. */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (roundManager.getPhase() != RoundPhase.FREEZE) return;
        if (teamManager.getTeam(event.getPlayer()) == GameTeam.SPECTATOR) return;

        Location from = event.getFrom();
        Location to   = event.getTo();

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

        Player dead   = event.getEntity();
        Player killer = dead.getKiller();

        // Keep the bomb in drops if the carrier died; clear everything else
        List<ItemStack> bombDrops = new ArrayList<>();
        for (ItemStack item : event.getDrops()) {
            if (bombManager.isBombItem(item)) bombDrops.add(item);
        }
        event.getDrops().clear();
        event.getDrops().addAll(bombDrops);

        event.setKeepInventory(false);
        event.setKeepLevel(true);

        teamManager.markDead(dead);

        if (killer != null) {
            Weapon w = weaponManager.getWeapon(killer.getInventory().getItemInMainHand());
            int bonus = w != null ? w.getKillBonus() : EconomyManager.KILL_BONUS;
            economyManager.onKill(killer, bonus);
        }

        // Defer by 1 tick so death is fully processed before checking elimination
        plugin.getServer().getScheduler().runTaskLater(plugin,
                roundManager::checkEliminationWin, 1L);
    }

    /** After respawn, keep dead players in spectator mode. */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (!teamManager.isDead(player)) return;

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (teamManager.isDead(player)) player.setGameMode(GameMode.SPECTATOR);
        }, 1L);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        hudManager.assignScoreboard(player);
        economyManager.ensurePlayer(player);

        if (roundManager.getPhase() == RoundPhase.WAITING) {
            teamManager.assignTeam(player);
            roundManager.tryStartRound();
        } else {
            teamManager.setTeam(player, GameTeam.SPECTATOR);
            player.setGameMode(GameMode.SPECTATOR);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        bombManager.cancelPlanting();
        bombManager.cancelDefusing();
        teamManager.removePlayer(player);
        economyManager.removePlayer(player);
        weaponManager.removePlayer(player);
        hudManager.removePlayer(player);
        roundManager.checkEliminationWin();
    }
}
