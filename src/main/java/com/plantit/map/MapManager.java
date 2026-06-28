package com.plantit.map;

import com.plantit.PlantIt;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;

public class MapManager {

    private final PlantIt plugin;
    private final List<Location> tSpawns = new ArrayList<>();
    private final List<Location> ctSpawns = new ArrayList<>();
    private String siteARegion = "";
    private String siteBRegion = "";

    private int tSpawnIndex = 0;
    private int ctSpawnIndex = 0;

    public MapManager(PlantIt plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        tSpawns.clear();
        ctSpawns.clear();
        for (String s : plugin.getConfig().getStringList("spawns.t")) {
            Location l = parseLocation(s);
            if (l != null) tSpawns.add(l);
        }
        for (String s : plugin.getConfig().getStringList("spawns.ct")) {
            Location l = parseLocation(s);
            if (l != null) ctSpawns.add(l);
        }
        siteARegion = plugin.getConfig().getString("sites.a", "");
        siteBRegion = plugin.getConfig().getString("sites.b", "");
    }

    public void addTSpawn(Location loc) {
        tSpawns.add(loc.clone());
        save();
    }

    public void addCtSpawn(Location loc) {
        ctSpawns.add(loc.clone());
        save();
    }

    public void clearTSpawns() { tSpawns.clear(); save(); }
    public void clearCtSpawns() { ctSpawns.clear(); save(); }

    public void setSiteRegion(String site, String region) {
        if (site.equalsIgnoreCase("a")) siteARegion = region;
        else siteBRegion = region;
        plugin.getConfig().set("sites." + site.toLowerCase(), region);
        plugin.saveConfig();
    }

    public void resetSpawnIndexes() { tSpawnIndex = 0; ctSpawnIndex = 0; }

    public Location nextTSpawn() {
        if (tSpawns.isEmpty()) return null;
        return tSpawns.get(tSpawnIndex++ % tSpawns.size());
    }

    public Location nextCtSpawn() {
        if (ctSpawns.isEmpty()) return null;
        return ctSpawns.get(ctSpawnIndex++ % ctSpawns.size());
    }

    public String getBombSiteAt(Location location) {
        if (isInRegion(location, siteARegion)) return "A";
        if (isInRegion(location, siteBRegion)) return "B";
        return null;
    }

    public boolean isInBombSite(Location location) {
        return getBombSiteAt(location) != null;
    }

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

    public boolean hasSpawns() { return !tSpawns.isEmpty() && !ctSpawns.isEmpty(); }

    private void save() {
        plugin.getConfig().set("spawns.t", tSpawns.stream().map(MapManager::serialize).toList());
        plugin.getConfig().set("spawns.ct", ctSpawns.stream().map(MapManager::serialize).toList());
        plugin.saveConfig();
    }

    private static String serialize(Location loc) {
        return loc.getWorld().getName() + ":" + loc.getX() + ":" + loc.getY() + ":"
                + loc.getZ() + ":" + loc.getYaw() + ":" + loc.getPitch();
    }

    private Location parseLocation(String s) {
        String[] p = s.split(":");
        if (p.length < 4) return null;
        World world = plugin.getServer().getWorld(p[0]);
        if (world == null) return null;
        try {
            double x = Double.parseDouble(p[1]);
            double y = Double.parseDouble(p[2]);
            double z = Double.parseDouble(p[3]);
            float yaw   = p.length > 4 ? Float.parseFloat(p[4]) : 0f;
            float pitch = p.length > 5 ? Float.parseFloat(p[5]) : 0f;
            return new Location(world, x, y, z, yaw, pitch);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public List<Location> getTSpawns() { return tSpawns; }
    public List<Location> getCtSpawns() { return ctSpawns; }
    public String getSiteARegion() { return siteARegion; }
    public String getSiteBRegion() { return siteBRegion; }
}
