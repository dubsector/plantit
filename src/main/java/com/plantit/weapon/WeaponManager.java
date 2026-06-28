package com.plantit.weapon;

import com.plantit.PlantIt;
import com.plantit.economy.EconomyManager;
import com.plantit.team.GameTeam;
import com.plantit.team.TeamManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class WeaponManager {

    private final PlantIt plugin;
    private final EconomyManager economyManager;
    private final TeamManager teamManager;
    private final NamespacedKey weaponKey;

    // Stash: weapons carried over between rounds (not cleared on death)
    private final Map<UUID, List<ItemStack>> stash = new HashMap<>();

    public WeaponManager(PlantIt plugin, EconomyManager economyManager, TeamManager teamManager) {
        this.plugin = plugin;
        this.economyManager = economyManager;
        this.teamManager = teamManager;
        this.weaponKey = new NamespacedKey(plugin, "weapon_type");
    }

    // -------------------------------------------------------------------------
    // Round lifecycle

    /** Called at match start — clear all weapon stashes. */
    public void onMatchStart() {
        stash.clear();
    }

    /** Called at round start — restore stashed weapons; give knife+default pistol if nothing stashed. */
    public void onRoundStart(Player player) {
        player.getInventory().clear();

        List<ItemStack> saved = stash.get(player.getUniqueId());
        if (saved != null && !saved.isEmpty()) {
            saved.forEach(item -> player.getInventory().addItem(item));
        } else {
            // First round / fresh player: knife + default pistol
            player.getInventory().setItem(0, createItem(defaultPistol(player)));
            player.getInventory().setItem(8, createItem(Weapon.KNIFE));
        }
    }

    /** Called at round end — save current inventory to stash (weapons carry over). */
    public void onRoundEnd(Player player) {
        List<ItemStack> weapons = new ArrayList<>();
        for (ItemStack item : player.getInventory()) {
            if (item != null && item.getType() != Material.AIR && isWeaponItem(item)) {
                weapons.add(item.clone());
            }
        }
        if (!weapons.isEmpty()) stash.put(player.getUniqueId(), weapons);
    }

    /** Remove player from stash tracking on quit. */
    public void removePlayer(Player player) {
        stash.remove(player.getUniqueId());
    }

    // -------------------------------------------------------------------------
    // Buying

    public boolean buy(Player player, Weapon weapon) {
        GameTeam team = teamManager.getTeam(player);
        if (!weapon.getSide().allows(team)) {
            player.sendMessage(Component.text("That weapon is not available for your team.", NamedTextColor.RED));
            return false;
        }
        if (!economyManager.spend(player, weapon.getCost())) {
            player.sendMessage(Component.text("Not enough money. ($" + weapon.getCost() + " needed)", NamedTextColor.RED));
            return false;
        }
        player.getInventory().addItem(createItem(weapon));
        player.sendMessage(Component.text("Purchased " + weapon.getDisplayName() + ".", NamedTextColor.GREEN));
        return true;
    }

    // -------------------------------------------------------------------------
    // Item creation and identification

    public ItemStack createItem(Weapon weapon) {
        ItemStack item = new ItemStack(weapon.getMaterial());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(weapon.getDisplayName(),
                weapon.isFree() ? NamedTextColor.GRAY : NamedTextColor.WHITE));
        meta.lore(List.of(
                Component.text("$" + weapon.getCost(), NamedTextColor.GOLD),
                Component.text(weapon.getDamage() + " dmg  •  $" + weapon.getKillBonus() + " kill bonus",
                        NamedTextColor.DARK_GRAY)
        ));
        meta.getPersistentDataContainer().set(weaponKey, PersistentDataType.STRING, weapon.name());
        item.setItemMeta(meta);
        return item;
    }

    public Weapon getWeapon(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) return null;
        String tag = item.getItemMeta().getPersistentDataContainer()
                .get(weaponKey, PersistentDataType.STRING);
        if (tag == null) return null;
        try { return Weapon.valueOf(tag); } catch (IllegalArgumentException e) { return null; }
    }

    public boolean isWeaponItem(ItemStack item) {
        return getWeapon(item) != null;
    }

    // -------------------------------------------------------------------------

    private Weapon defaultPistol(Player player) {
        return teamManager.getTeam(player) == GameTeam.CT ? Weapon.USP : Weapon.GLOCK;
    }

    public NamespacedKey getWeaponKey() { return weaponKey; }
}
