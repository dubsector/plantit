package com.plantit.weapon;

import com.plantit.PlantIt;
import com.plantit.economy.EconomyManager;
import com.plantit.team.GameTeam;
import com.plantit.team.TeamManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class BuyMenu {

    // Slot layout: each row is a weapon category
    private static final Weapon[][] LAYOUT = {
        // Row 0 — Pistols
        { Weapon.GLOCK, Weapon.USP, Weapon.TEC9, Weapon.FIVE_SEVEN, Weapon.DEAGLE, null, null, null, null },
        // Row 1 — SMGs
        { Weapon.MAC10, Weapon.MP9, Weapon.UMP, Weapon.P90, null, null, null, null, null },
        // Row 2 — Rifles
        { Weapon.GALIL, Weapon.FAMAS, Weapon.AK47, Weapon.M4A4, Weapon.SG553, Weapon.AUG, null, null, null },
        // Row 3 — Snipers / Heavy
        { Weapon.SSG08, Weapon.AWP, null, Weapon.NOVA, Weapon.XM1014, null, null, null, null },
        // Row 4 — Equipment
        { null, null, null, null, null, null, null, null, null }, // filled dynamically
        // Row 5 — Info row (not clickable)
        { null, null, null, null, null, null, null, null, null },
    };

    private final PlantIt plugin;
    private final WeaponManager weaponManager;
    private final EconomyManager economyManager;
    private final TeamManager teamManager;

    // Track which inventories are our buy menus to avoid re-intercept
    private final Set<UUID> open = new HashSet<>();

    public BuyMenu(PlantIt plugin, WeaponManager weaponManager,
                   EconomyManager economyManager, TeamManager teamManager) {
        this.plugin = plugin;
        this.weaponManager = weaponManager;
        this.economyManager = economyManager;
        this.teamManager = teamManager;
    }

    public void open(Player player) {
        GameTeam team = teamManager.getTeam(player);
        Inventory inv = Bukkit.createInventory(null, 54,
                Component.text("Buy Menu — $" + economyManager.getMoney(player), NamedTextColor.GOLD));

        // Fill weapon slots
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 9; col++) {
                Weapon w = LAYOUT[row][col];
                if (w == null) continue;
                int slot = row * 9 + col;

                if (!w.getSide().allows(team)) {
                    // Gray out unavailable weapons
                    inv.setItem(slot, unavailable(w));
                } else {
                    inv.setItem(slot, weaponManager.createItem(w));
                }
            }
        }

        // Row 4 — Equipment
        inv.setItem(36, armorItem(false, 650));   // Armor
        inv.setItem(37, armorItem(true, 1000));   // Armor + Helmet
        if (team == GameTeam.CT) {
            inv.setItem(38, kitItem());           // Defuse kit
        }
        inv.setItem(40, heItem());
        inv.setItem(41, flashItem());
        inv.setItem(42, smokeItem());

        // Row 5 — info
        inv.setItem(49, moneyInfoItem(economyManager.getMoney(player)));

        open.add(player.getUniqueId());
        player.openInventory(inv);
    }

    public void close(Player player) {
        open.remove(player.getUniqueId());
    }

    public boolean isOpen(Player player) {
        return open.contains(player.getUniqueId());
    }

    /** Handle a click in the buy menu. Returns true if the click was handled. */
    public boolean handleClick(Player player, int slot, Inventory inv) {
        ItemStack clicked = inv.getItem(slot);
        if (clicked == null || clicked.getType() == Material.AIR) return true;

        // Check if it's a weapon item
        Weapon weapon = weaponManager.getWeapon(clicked);
        if (weapon != null) {
            weaponManager.buy(player, weapon);
            refreshTitle(player, inv);
            return true;
        }

        // Equipment
        String name = clicked.hasItemMeta() && clicked.getItemMeta().hasDisplayName()
                ? net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                        .serialize(clicked.getItemMeta().displayName())
                : "";

        switch (name) {
            case "Armor" -> {
                if (economyManager.spend(player, 650)) {
                    equip(player, new ItemStack(Material.CHAINMAIL_CHESTPLATE));
                    equip(player, new ItemStack(Material.CHAINMAIL_LEGGINGS));
                    equip(player, new ItemStack(Material.CHAINMAIL_BOOTS));
                    player.sendMessage(Component.text("Armor purchased.", NamedTextColor.GREEN));
                } else {
                    player.sendMessage(Component.text("Not enough money.", NamedTextColor.RED));
                }
            }
            case "Armor + Helmet" -> {
                if (economyManager.spend(player, 1000)) {
                    equip(player, new ItemStack(Material.CHAINMAIL_HELMET));
                    equip(player, new ItemStack(Material.CHAINMAIL_CHESTPLATE));
                    equip(player, new ItemStack(Material.CHAINMAIL_LEGGINGS));
                    equip(player, new ItemStack(Material.CHAINMAIL_BOOTS));
                    player.sendMessage(Component.text("Armor + Helmet purchased.", NamedTextColor.GREEN));
                } else {
                    player.sendMessage(Component.text("Not enough money.", NamedTextColor.RED));
                }
            }
            case "Defuse Kit" -> economyManager.buyKit(player);
            case "HE Grenade" -> {
                if (economyManager.spend(player, 300)) {
                    player.getInventory().addItem(new ItemStack(Material.TNT));
                    player.sendMessage(Component.text("HE Grenade purchased.", NamedTextColor.GREEN));
                } else {
                    player.sendMessage(Component.text("Not enough money.", NamedTextColor.RED));
                }
            }
            case "Flashbang" -> {
                if (economyManager.spend(player, 200)) {
                    player.getInventory().addItem(new ItemStack(Material.SNOWBALL));
                    player.sendMessage(Component.text("Flashbang purchased.", NamedTextColor.GREEN));
                } else {
                    player.sendMessage(Component.text("Not enough money.", NamedTextColor.RED));
                }
            }
            case "Smoke Grenade" -> {
                if (economyManager.spend(player, 300)) {
                    player.getInventory().addItem(new ItemStack(Material.GRAY_DYE));
                    player.sendMessage(Component.text("Smoke Grenade purchased.", NamedTextColor.GREEN));
                } else {
                    player.sendMessage(Component.text("Not enough money.", NamedTextColor.RED));
                }
            }
        }

        refreshTitle(player, inv);
        return true;
    }

    private void refreshTitle(Player player, Inventory inv) {
        // Update money display in title by reopening
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && isOpen(player)) {
                open.remove(player.getUniqueId());
                open(player);
            }
        }, 1L);
    }

    // -------------------------------------------------------------------------
    // Item builders

    private ItemStack unavailable(Weapon w) {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(w.getDisplayName() + " (other team)", NamedTextColor.DARK_GRAY));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack armorItem(boolean withHelmet, int cost) {
        ItemStack item = new ItemStack(withHelmet ? Material.CHAINMAIL_HELMET : Material.CHAINMAIL_CHESTPLATE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(withHelmet ? "Armor + Helmet" : "Armor", NamedTextColor.WHITE));
        meta.lore(List.of(Component.text("$" + cost, NamedTextColor.GOLD)));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack kitItem() {
        ItemStack item = new ItemStack(Material.SHEARS);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Defuse Kit", NamedTextColor.AQUA));
        meta.lore(List.of(Component.text("$400", NamedTextColor.GOLD),
                Component.text("Reduces defuse time to 5s", NamedTextColor.DARK_GRAY)));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack heItem() {
        ItemStack item = new ItemStack(Material.TNT);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("HE Grenade", NamedTextColor.RED));
        meta.lore(List.of(Component.text("$300", NamedTextColor.GOLD)));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack flashItem() {
        ItemStack item = new ItemStack(Material.SNOWBALL);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Flashbang", NamedTextColor.WHITE));
        meta.lore(List.of(Component.text("$200", NamedTextColor.GOLD)));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack smokeItem() {
        ItemStack item = new ItemStack(Material.GRAY_DYE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Smoke Grenade", NamedTextColor.GRAY));
        meta.lore(List.of(Component.text("$300", NamedTextColor.GOLD)));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack moneyInfoItem(int money) {
        ItemStack item = new ItemStack(Material.EMERALD);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("$" + money, NamedTextColor.GREEN));
        meta.lore(List.of(Component.text("Your current balance", NamedTextColor.GRAY)));
        item.setItemMeta(meta);
        return item;
    }

    private void equip(Player player, ItemStack armor) {
        switch (armor.getType()) {
            case CHAINMAIL_HELMET    -> player.getInventory().setHelmet(armor);
            case CHAINMAIL_CHESTPLATE -> player.getInventory().setChestplate(armor);
            case CHAINMAIL_LEGGINGS  -> player.getInventory().setLeggings(armor);
            case CHAINMAIL_BOOTS     -> player.getInventory().setBoots(armor);
            default -> {}
        }
    }
}
