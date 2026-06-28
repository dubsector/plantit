package com.plantit.bomb;

import com.plantit.round.RoundManager;
import com.plantit.round.RoundPhase;
import com.plantit.team.GameTeam;
import com.plantit.team.TeamManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;

public class BombListener implements Listener {

    private final BombManager bombManager;
    private final RoundManager roundManager;
    private final TeamManager teamManager;

    public BombListener(BombManager bombManager, RoundManager roundManager, TeamManager teamManager) {
        this.bombManager = bombManager;
        this.roundManager = roundManager;
        this.teamManager = teamManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (roundManager.getPhase() != RoundPhase.LIVE) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) return;

        Player player = event.getPlayer();
        GameTeam team = teamManager.getTeam(player);

        if (team == GameTeam.T && bombManager.isBombItem(player.getInventory().getItemInMainHand())) {
            event.setCancelled(true);
            BombState state = bombManager.getState();
            if ((state == BombState.CARRIED) && !bombManager.isPlanting(player)) {
                bombManager.tryStartPlanting(player);
            }
            return;
        }

        if (team == GameTeam.CT && bombManager.isNearBomb(player)) {
            event.setCancelled(true);
            if (bombManager.getState() == BombState.PLANTED && !bombManager.isDefusing(player)) {
                bombManager.tryStartDefusing(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMove(PlayerMoveEvent event) {
        if (roundManager.getPhase() != RoundPhase.LIVE) return;
        Player player = event.getPlayer();

        if (event.getFrom().getX() == event.getTo().getX()
                && event.getFrom().getY() == event.getTo().getY()
                && event.getFrom().getZ() == event.getTo().getZ()) return;

        if (bombManager.isPlanting(player)) bombManager.cancelPlanting();
        else if (bombManager.isDefusing(player)) bombManager.cancelDefusing();
    }
}
