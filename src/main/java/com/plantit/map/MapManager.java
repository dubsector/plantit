package com.plantit.map;

import com.plantit.PlantIt;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.concurrent.ThreadLocalRandom;

public class MapManager {

    private final PlantIt plugin;

    private String tSpawnRegion  = "";
    private String ctSpawnRegion = "";
    private float  tSpawnYaw     = 0f;
    private float  ctSpawnYaw    = 0f;

    private String siteARegion = "";
    private String siteBRegion = "";

    public MapManager(PlantIt plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        tSpawnRegion  = plugin.getConfig().getString("spawns.t.region", "");
        ctSpawnRegion = plugin.getConfig().getString("spawns.ct.region", "");
        tSpawnYaw     = (float) plugin.getConfig().getDouble("spawns.t.yaw", 0.0);
        ctSpawnYaw    = (float) plugin.getConfig().getDouble("spawns.ct.yaw", 0.0);
        siteARegion   = plugin.getConfig().getString("sites.a", "");
        siteBRegion   = plugin.getConfig().getString("sites.b", "");
    }

    // -------------------------------------------------------------------------
    // Spawn region setters

    public void setTSpawnRegion(String region, float yaw) {
        tSpawnRegion = region;
        tSpawnYaw    = yaw;
        plugin.getConfig().set("spawns.t.region", region);
        plugin.getConfig().set("spawns.t.yaw", (double) yaw);
        plugin.saveConfig();
    }

    public void setCtSpawnRegion(String region, float yaw) {
        ctSpawnRegion = region;
        ctSpawnYaw    = yaw;
        plugin.getConfig().set("spawns.ct.region", region);
        plugin.getConfig().set("spawns.ct.yaw", (double) yaw);
        plugin.saveConfig();
    }

    // -------------------------------------------------------------------------
    // Random spawn within region

    public Location getRandomTSpawn(World world) {
        return randomInRegion(world, tSpawnRegion, tSpawnYaw);
    }

    public Location getRandomCtSpawn(World world) {
        return randomInRegion(world, ctSpawnRegion, ctSpawnYaw);
    }

    private Location randomInRegion(World world, String regionName, float yaw) {
        ProtectedRegion region = getRegion(world, regionName);
        if (region == null) return null;

        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();

        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        int x = min.x() + rnd.nextInt(max.x() - min.x() + 1);
        int z = min.z() + rnd.nextInt(max.z() - min.z() + 1);
        int y = world.getHighestBlockYAt(x, z);

        return new Location(world, x + 0.5, y + 1, z + 0.5, yaw, 0f);
    }

    // -------------------------------------------------------------------------
    // Bomb site helpers

    public void setSiteRegion(String site, String region) {
        if (site.equalsIgnoreCase("a")) siteARegion = region;
        else siteBRegion = region;
        plugin.getConfig().set("sites." + site.toLowerCase(), region);
        plugin.saveConfig();
    }

    public String getBombSiteAt(Location location) {
        if (isInRegion(location, siteARegion)) return "A";
        if (isInRegion(location, siteBRegion)) return "B";
        return null;
    }

    public boolean isInBombSite(Location location) {
        return getBombSiteAt(location) != null;
    }

    // -------------------------------------------------------------------------
    // WorldGuard region lookup

    private boolean isInRegion(Location location, String regionName) {
        if (regionName == null || regionName.isEmpty()) return false;
        World world = location.getWorld();
        if (world == null) return false;
        try {
            RegionManager manager = WorldGuard.getInstance().getPlatform()
                    .getRegionContainer().get(BukkitAdapter.adapt(world));
            if (manager == null) return false;
            return manager.getApplicableRegions(BukkitAdapter.asBlockVector(location))
                    .getRegions().stream().anyMatch(r -> r.getId().equalsIgnoreCase(regionName));
        } catch (Exception e) {
            return false;
        }
    }

    private ProtectedRegion getRegion(World world, String regionName) {
        if (regionName == null || regionName.isEmpty()) return null;
        try {
            RegionManager manager = WorldGuard.getInstance().getPlatform()
                    .getRegionContainer().get(BukkitAdapter.adapt(world));
            if (manager == null) return null;
            return manager.getRegion(regionName);
        } catch (Exception e) {
            return null;
        }
    }

    // -------------------------------------------------------------------------

    public boolean hasSpawns() { return !tSpawnRegion.isEmpty() && !ctSpawnRegion.isEmpty(); }

    public String getTSpawnRegion()  { return tSpawnRegion; }
    public String getCtSpawnRegion() { return ctSpawnRegion; }
    public String getSiteARegion()   { return siteARegion; }
    public String getSiteBRegion()   { return siteBRegion; }
}
