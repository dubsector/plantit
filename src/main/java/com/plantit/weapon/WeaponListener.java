package com.plantit.weapon;

import com.plantit.PlantIt;
import com.plantit.economy.EconomyManager;
import com.plantit.round.RoundManager;
import com.plantit.round.RoundPhase;
import com.plantit.team.TeamManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

public class WeaponListener implements Listener {

    private final PlantIt plugin;
    private final WeaponManager weaponManager;
    private final BuyMenu buyMenu;
    private final RoundManager roundManager;
    private final EconomyManager economyManager;

    public WeaponListener(PlantIt plugin, WeaponManager weaponManager, BuyMenu buyMenu,
                          RoundManager roundManager, EconomyManager economyManager) {
        this.plugin         = plugin;
        this.weaponManager  = weaponManager;
        this.buyMenu        = buyMenu;
        this.roundManager   = roundManager;
        this.economyManager = economyManager;
    }

    /** Intercept E-key during freeze phase → open buy menu instead. */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (roundManager.getPhase() != RoundPhase.FREEZE) return;
        if (buyMenu.isOpen(player)) return; // already our GUI opening

        // Player's own crafting inventory = they pressed E
        if (event.getInventory().getType() == InventoryType.CRAFTING) {
            event.setCancelled(true);
            // Open on next tick to avoid conflict with the cancelled event
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> buyMenu.open(player), 1L);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!buyMenu.isOpen(player)) return;

        event.setCancelled(true);
        if (event.getClickedInventory() == null) return;
        if (event.getClickedInventory().getType() == InventoryType.PLAYER) return;

        buyMenu.handleClick(player, event.getRawSlot(), event.getInventory());
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        buyMenu.close(player);
    }

    /** Override damage to match weapon stats. */
    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof Player)) return;

        ItemStack held = attacker.getInventory().getItemInMainHand();
        Weapon weapon = weaponManager.getWeapon(held);
        if (weapon != null) event.setDamage(weapon.getDamage());
    }
}
