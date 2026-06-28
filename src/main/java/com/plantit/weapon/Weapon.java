package com.plantit.weapon;

import com.plantit.team.GameTeam;
import org.bukkit.Material;

public enum Weapon {

    // --- Pistols ---
    GLOCK       ("Glock-18",      200,  Material.WOODEN_SWORD,    4,  300, Side.T),
    USP         ("USP-S",         200,  Material.WOODEN_SWORD,    4,  300, Side.CT),
    TEC9        ("Tec-9",         500,  Material.STONE_SWORD,     5,  300, Side.T),
    FIVE_SEVEN  ("Five-SeveN",    500,  Material.STONE_SWORD,     5,  300, Side.CT),
    DEAGLE      ("Desert Eagle",  700,  Material.GOLDEN_SWORD,    9,  300, Side.BOTH),

    // --- SMGs ---
    MAC10       ("MAC-10",       1050,  Material.WOODEN_AXE,      5,  600, Side.T),
    MP9         ("MP9",          1250,  Material.WOODEN_AXE,      5,  600, Side.CT),
    UMP         ("UMP-45",       1200,  Material.STONE_AXE,       6,  600, Side.BOTH),
    P90         ("P90",          2350,  Material.IRON_AXE,        6,  600, Side.BOTH),

    // --- Rifles ---
    GALIL       ("Galil AR",     2050,  Material.IRON_SWORD,      7,  300, Side.T),
    FAMAS       ("FAMAS",        2050,  Material.IRON_SWORD,      7,  300, Side.CT),
    AK47        ("AK-47",        2700,  Material.DIAMOND_SWORD,   9,  300, Side.T),
    M4A4        ("M4A4",         3100,  Material.DIAMOND_SWORD,   8,  300, Side.CT),
    SG553       ("SG 553",       3000,  Material.GOLDEN_AXE,      9,  300, Side.T),
    AUG         ("AUG",          3300,  Material.GOLDEN_AXE,      8,  300, Side.CT),

    // --- Snipers ---
    SSG08       ("SSG 08",       1700,  Material.CROSSBOW,       14,  300, Side.BOTH),
    AWP         ("AWP",          4750,  Material.BOW,            20,  100, Side.BOTH),

    // --- Heavy ---
    NOVA        ("Nova",         1050,  Material.WOODEN_HOE,      8,  900, Side.BOTH),
    XM1014      ("XM1014",       2000,  Material.STONE_HOE,       9,  900, Side.BOTH),

    // --- Melee (default, free) ---
    KNIFE       ("Knife",           0,  Material.NETHERITE_SWORD, 9, 1500, Side.BOTH);

    // -------------------------------------------------------------------------

    public enum Side {
        T, CT, BOTH;
        public boolean allows(GameTeam team) {
            return switch (this) {
                case BOTH -> team == GameTeam.T || team == GameTeam.CT;
                case T    -> team == GameTeam.T;
                case CT   -> team == GameTeam.CT;
            };
        }
    }

    private final String displayName;
    private final int cost;
    private final Material material;
    private final int damage;
    private final int killBonus;
    private final Side side;

    Weapon(String displayName, int cost, Material material, int damage, int killBonus, Side side) {
        this.displayName = displayName;
        this.cost        = cost;
        this.material    = material;
        this.damage      = damage;
        this.killBonus   = killBonus;
        this.side        = side;
    }

    public String getDisplayName() { return displayName; }
    public int getCost()           { return cost; }
    public Material getMaterial()  { return material; }
    public int getDamage()         { return damage; }
    public int getKillBonus()      { return killBonus; }
    public Side getSide()          { return side; }

    public boolean isFree()        { return cost == 0; }
    public boolean isPistol()      { return this == GLOCK || this == USP || this == TEC9 || this == FIVE_SEVEN || this == DEAGLE; }
}
