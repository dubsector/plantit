package com.plantit.bomb;

import com.plantit.PlantIt;
import com.plantit.economy.EconomyManager;
import com.plantit.map.MapManager;
import com.plantit.round.RoundEndReason;
import com.plantit.round.RoundManager;
import com.plantit.team.GameTeam;
import com.plantit.team.TeamManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.UUID;

public class BombManager {

    // Tick constants
    public static final int PLANT_TICKS       = 64;   // 3.2 s
    public static final int DEFUSE_TICKS      = 200;  // 10 s
    public static final int DEFUSE_KIT_TICKS  = 100;  // 5 s
    public static final int BOMB_TIMER_TICKS  = 800;  // 40 s

    private final PlantIt plugin;
    private final TeamManager teamManager;
    private final EconomyManager economyManager;
    private final MapManager mapManager;
    private final NamespacedKey bombKey;

    private RoundManager roundManager;

    private BombState state = BombState.INACTIVE;

    private UUID planting;
    private int plantProgress;
    private BukkitTask plantTask;

    private Location bombLocation;
    private String plantedSite;
    private int bombTimeLeft;
    private BukkitTask bombCountdownTask;

    private UUID defusing;
    private int defuseProgress;
    private int defuseTotal;
    private BukkitTask defuseTask;

    public BombManager(PlantIt plugin, TeamManager teamManager,
                       EconomyManager economyManager, MapManager mapManager) {
        this.plugin = plugin;
        this.teamManager = teamManager;
        this.economyManager = economyManager;
        this.mapManager = mapManager;
        this.bombKey = new NamespacedKey(plugin, "c4_bomb");
    }

    public void setRoundManager(RoundManager rm) { this.roundManager = rm; }

    // -------------------------------------------------------------------------
    // Round lifecycle

    public void onRoundStart(List<Player> tPlayers) {
        reset();
        if (tPlayers.isEmpty()) return;
        Player carrier = tPlayers.get((int) (Math.random() * tPlayers.size()));
        giveBomb(carrier);
        state = BombState.CARRIED;
    }

    public void reset() {
        cancelPlantTask();
        cancelDefuseTask();
        cancelBombCountdown();
        removeBombBlock();
        state = BombState.INACTIVE;
        planting = null;
        defusing = null;
        bombLocation = null;
        plantedSite = null;
        plantProgress = 0;
        defuseProgress = 0;
    }

    // -------------------------------------------------------------------------
    // Planting

    public boolean tryStartPlanting(Player player) {
        if (state != BombState.CARRIED) return false;
        if (!hasBomb(player)) return false;
        if (planting != null) return false;

        String site = mapManager.getBombSiteAt(player.getLocation());
        if (site == null) {
            player.sendActionBar(Component.text("Not in a bomb site!", NamedTextColor.RED));
            return false;
        }

        state = BombState.PLANTING;
        planting = player.getUniqueId();
        plantProgress = 0;
        plantedSite = site;

        plantTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            Player p = plugin.getServer().getPlayer(planting);
            if (p == null || !p.isOnline()) { cancelPlanting(); return; }

            plantProgress++;
            float pct = (float) plantProgress / PLANT_TICKS;
            p.sendActionBar(progressBar(pct, "Planting...", NamedTextColor.RED));

            if (plantProgress >= PLANT_TICKS) completePlant(p);
        }, 0L, 1L);

        return true;
    }

    public void cancelPlanting() {
        cancelPlantTask();
        if (planting != null) {
            Player p = plugin.getServer().getPlayer(planting);
            if (p != null) p.sendActionBar(Component.text("Planting cancelled.", NamedTextColor.RED));
        }
        planting = null;
        plantProgress = 0;
        state = BombState.CARRIED;
    }

    private void completePlant(Player player) {
        cancelPlantTask();
        planting = null;

        removeBombFromInventory(player);
        bombLocation = player.getLocation().getBlock().getLocation().add(0.5, 0, 0.5);
        bombLocation.getBlock().setType(Material.REDSTONE_BLOCK);

        state = BombState.PLANTED;
        economyManager.onBombPlanted();
        broadcast(Component.text("Bomb planted at " + plantedSite + "!", NamedTextColor.RED));
        startBombCountdown();
    }

    // -------------------------------------------------------------------------
    // Defusing

    public boolean tryStartDefusing(Player player) {
        if (state != BombState.PLANTED) return false;
        if (teamManager.getTeam(player) != GameTeam.CT) return false;
        if (defusing != null) return false;
        if (!isNearBomb(player)) return false;

        state = BombState.DEFUSING;
        defusing = player.getUniqueId();
        defuseProgress = 0;
        defuseTotal = economyManager.hasKit(player) ? DEFUSE_KIT_TICKS : DEFUSE_TICKS;

        broadcast(Component.text(player.getName() + " is defusing the bomb...", NamedTextColor.BLUE));

        defuseTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            Player p = plugin.getServer().getPlayer(defusing);
            if (p == null || !p.isOnline()) { cancelDefusing(); return; }

            defuseProgress++;
            float pct = (float) defuseProgress / defuseTotal;
            p.sendActionBar(progressBar(pct, "Defusing...", NamedTextColor.BLUE));

            if (defuseProgress >= defuseTotal) completeDefuse(p);
        }, 0L, 1L);

        return true;
    }

    public void cancelDefusing() {
        cancelDefuseTask();
        if (defusing != null) {
            Player p = plugin.getServer().getPlayer(defusing);
            if (p != null) {
                p.sendActionBar(Component.text("Defuse cancelled.", NamedTextColor.RED));
                broadcast(Component.text(p.getName() + " stopped defusing.", NamedTextColor.YELLOW));
            }
        }
        defusing = null;
        defuseProgress = 0;
        state = BombState.PLANTED;
    }

    private void completeDefuse(Player player) {
        cancelDefuseTask();
        cancelBombCountdown();
        removeBombBlock();
        economyManager.onBombDefused();
        state = BombState.INACTIVE;
        broadcast(Component.text(player.getName() + " defused the bomb!", NamedTextColor.BLUE));
        if (roundManager != null) roundManager.endRound(RoundEndReason.BOMB_DEFUSED);
    }

    // -------------------------------------------------------------------------
    // Bomb countdown

    private void startBombCountdown() {
        bombTimeLeft = BOMB_TIMER_TICKS;
        bombCountdownTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (bombTimeLeft <= 0) { explode(); return; }
            if (shouldBeep(bombTimeLeft)) playBeep();
            bombTimeLeft--;
        }, 0L, 1L);
    }

    private void explode() {
        cancelBombCountdown();
        cancelDefuseTask();
        if (bombLocation != null) {
            bombLocation.getWorld().createExplosion(bombLocation, 3.0f, false, false);
        }
        removeBombBlock();
        state = BombState.INACTIVE;
        if (roundManager != null) roundManager.endRound(RoundEndReason.BOMB_EXPLODED);
    }

    private boolean shouldBeep(int ticks) {
        if (ticks > 600) return ticks % 80 == 0;
        if (ticks > 400) return ticks % 60 == 0;
        if (ticks > 200) return ticks % 40 == 0;
        if (ticks > 100) return ticks % 20 == 0;
        if (ticks > 40)  return ticks % 10 == 0;
        return ticks % 5 == 0;
    }

    private void playBeep() {
        if (bombLocation == null) return;
        bombLocation.getWorld().playSound(bombLocation, Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 2.0f);
    }

    // -------------------------------------------------------------------------
    // Bomb item helpers

    public void giveBomb(Player player) {
        ItemStack item = new ItemStack(Material.CLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("C4 Explosive", NamedTextColor.RED));
        meta.getPersistentDataContainer().set(bombKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        player.getInventory().addItem(item);
        player.sendMessage(Component.text("You are the bomb carrier!", NamedTextColor.RED));
    }

    public boolean isBombItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(bombKey, PersistentDataType.BYTE);
    }

    public boolean hasBomb(Player player) {
        for (ItemStack item : player.getInventory()) {
            if (isBombItem(item)) return true;
        }
        return false;
    }

    private void removeBombFromInventory(Player player) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            if (isBombItem(player.getInventory().getItem(i))) {
                player.getInventory().setItem(i, null);
                return;
            }
        }
    }

    public boolean isNearBomb(Player player) {
        if (bombLocation == null) return false;
        if (!player.getWorld().equals(bombLocation.getWorld())) return false;
        return player.getLocation().distance(bombLocation) <= 3.0;
    }

    public boolean isBombBlock(Location loc) {
        if (bombLocation == null) return false;
        return bombLocation.getBlock().equals(loc.getBlock());
    }

    private void removeBombBlock() {
        if (bombLocation != null && bombLocation.getBlock().getType() == Material.REDSTONE_BLOCK) {
            bombLocation.getBlock().setType(Material.AIR);
        }
        bombLocation = null;
    }

    // -------------------------------------------------------------------------
    // Task cancellation

    private void cancelPlantTask() {
        if (plantTask != null) { plantTask.cancel(); plantTask = null; }
    }

    private void cancelDefuseTask() {
        if (defuseTask != null) { defuseTask.cancel(); defuseTask = null; }
    }

    private void cancelBombCountdown() {
        if (bombCountdownTask != null) { bombCountdownTask.cancel(); bombCountdownTask = null; }
    }

    // -------------------------------------------------------------------------

    private void broadcast(Component msg) {
        plugin.getServer().getOnlinePlayers().forEach(p -> p.sendMessage(msg));
    }

    private static Component progressBar(float pct, String label, NamedTextColor color) {
        int filled = (int) (pct * 20);
        return Component.text("[", NamedTextColor.GRAY)
                .append(Component.text("█".repeat(filled), color))
                .append(Component.text("░".repeat(20 - filled), NamedTextColor.DARK_GRAY))
                .append(Component.text("] " + label, NamedTextColor.GRAY));
    }

    // -------------------------------------------------------------------------
    // Getters

    public BombState getState()       { return state; }
    public boolean isPlanting(Player p) { return planting != null && planting.equals(p.getUniqueId()); }
    public boolean isDefusing(Player p) { return defusing != null && defusing.equals(p.getUniqueId()); }
    public Location getBombLocation() { return bombLocation; }
    public int getBombTimeLeft()      { return bombTimeLeft; }
    public String getPlantedSite()    { return plantedSite; }
}
