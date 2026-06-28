package com.plantit.command;

import com.plantit.PlantIt;
import com.plantit.economy.EconomyManager;
import com.plantit.map.MapManager;
import com.plantit.round.RoundManager;
import com.plantit.round.RoundPhase;
import com.plantit.team.GameTeam;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public class PlantItCommand implements CommandExecutor, TabCompleter {

    private final PlantIt plugin;
    private final MapManager mapManager;
    private final RoundManager roundManager;
    private final EconomyManager economyManager;

    public PlantItCommand(PlantIt plugin, MapManager mapManager,
                          RoundManager roundManager, EconomyManager economyManager) {
        this.plugin = plugin;
        this.mapManager = mapManager;
        this.roundManager = roundManager;
        this.economyManager = economyManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Player only.");
            return true;
        }

        if (args.length == 0) { sendHelp(player); return true; }

        switch (args[0].toLowerCase()) {
            case "setmap" -> {
                if (!player.hasPermission("plantit.admin")) { deny(player); return true; }
                if (args.length < 2) { usage(player, "/plantit setmap <mapName>"); return true; }
                String prev = mapManager.getActiveMap();
                mapManager.loadMap(args[1]);
                if (mapManager.getActiveMap().equals(args[1])) {
                    player.sendMessage(ok("Loaded map '" + args[1] + "'. setspawn/setsite now apply to this map."));
                } else {
                    mapManager.loadMap(prev); // revert
                    player.sendMessage(Component.text("Map '" + args[1] + "' not found in config.yml — add it first.", NamedTextColor.RED));
                }
            }
            case "setspawn" -> {
                if (!player.hasPermission("plantit.admin")) { deny(player); return true; }
                if (args.length < 3) { usage(player, "/plantit setspawn <t|ct> <regionName>"); return true; }
                String spawnRegion = args[2];
                float yaw = player.getLocation().getYaw();
                if (args[1].equalsIgnoreCase("t")) {
                    mapManager.setTSpawnRegion(spawnRegion, yaw);
                    player.sendMessage(ok("T spawn region set to '" + spawnRegion + "' (facing " + Math.round(yaw) + "°)."));
                } else if (args[1].equalsIgnoreCase("ct")) {
                    mapManager.setCtSpawnRegion(spawnRegion, yaw);
                    player.sendMessage(ok("CT spawn region set to '" + spawnRegion + "' (facing " + Math.round(yaw) + "°)."));
                } else {
                    usage(player, "/plantit setspawn <t|ct> <regionName>");
                }
            }
            case "setsite" -> {
                if (!player.hasPermission("plantit.admin")) { deny(player); return true; }
                if (args.length < 3) { usage(player, "/plantit setsite <a|b> <regionName>"); return true; }
                String site = args[1].toLowerCase();
                if (!site.equals("a") && !site.equals("b")) { usage(player, "/plantit setsite <a|b> <regionName>"); return true; }
                mapManager.setSiteRegion(site, args[2]);
                player.sendMessage(ok("Site " + site.toUpperCase() + " mapped to region '" + args[2] + "'."));
            }
            case "forcestart" -> {
                if (!player.hasPermission("plantit.admin")) { deny(player); return true; }
                roundManager.startRound();
                player.sendMessage(ok("Round force-started."));
            }
            case "status" -> {
                if (!player.hasPermission("plantit.admin")) { deny(player); return true; }
                player.sendMessage(Component.text("=== PlantIt Status ===", NamedTextColor.GOLD));
                player.sendMessage(info("Phase",  roundManager.getPhase().toString()));
                player.sendMessage(info("Round",  roundManager.getCurrentRound() + " / " + plugin.getGameConfig().getMaxRounds()));
                player.sendMessage(info("Score",  "T " + roundManager.getTScore() + " : " + roundManager.getCtScore() + " CT"));
                player.sendMessage(info("Active map", mapManager.getActiveMap().isEmpty() ? "legacy/none" : mapManager.getActiveMap()));
                player.sendMessage(info("T spawn region",  mapManager.getTSpawnRegion().isEmpty()  ? "not set" : mapManager.getTSpawnRegion()));
                player.sendMessage(info("CT spawn region", mapManager.getCtSpawnRegion().isEmpty() ? "not set" : mapManager.getCtSpawnRegion()));
                player.sendMessage(info("Site A", mapManager.getSiteARegion().isEmpty() ? "not set" : mapManager.getSiteARegion()));
                player.sendMessage(info("Site B", mapManager.getSiteBRegion().isEmpty() ? "not set" : mapManager.getSiteBRegion()));
            }
            case "buy" -> {
                if (args.length < 2) { usage(player, "/plantit buy <kit>"); return true; }
                if (roundManager.getPhase() != RoundPhase.FREEZE) {
                    player.sendMessage(Component.text("Buy phase only.", NamedTextColor.RED));
                    return true;
                }
                if (args[1].equalsIgnoreCase("kit")) {
                    economyManager.buyKit(player);
                } else {
                    player.sendMessage(Component.text("Unknown item. Available: kit", NamedTextColor.RED));
                }
            }
            default -> sendHelp(player);
        }
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(Component.text("=== PlantIt ===", NamedTextColor.GOLD));
        player.sendMessage(Component.text("/plantit buy kit", NamedTextColor.YELLOW)
                .append(Component.text(" — buy defuse kit ($400, CT only, buy phase)", NamedTextColor.GRAY)));
        if (player.hasPermission("plantit.admin")) {
            player.sendMessage(Component.text("/plantit setspawn <t|ct> <region>", NamedTextColor.YELLOW)
                    .append(Component.text(" — set spawn zone region for team (face the map first)", NamedTextColor.GRAY)));
            player.sendMessage(Component.text("/plantit setsite <a|b> <region>", NamedTextColor.YELLOW)
                    .append(Component.text(" — assign WorldGuard region as bomb site", NamedTextColor.GRAY)));
            player.sendMessage(Component.text("/plantit forcestart", NamedTextColor.YELLOW)
                    .append(Component.text(" — force-start the current round", NamedTextColor.GRAY)));
            player.sendMessage(Component.text("/plantit status", NamedTextColor.YELLOW)
                    .append(Component.text(" — show game state", NamedTextColor.GRAY)));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) return List.of("buy", "setmap", "setspawn", "setsite", "forcestart", "status");
        if (args.length == 2) {
            return switch (args[0].toLowerCase()) {
                case "buy" -> List.of("kit");
                case "setspawn" -> List.of("t", "ct");
                case "setsite" -> List.of("a", "b");
                default -> List.of();
            };
        }
        return List.of();
    }

    private static Component ok(String msg)   { return Component.text(msg, NamedTextColor.GREEN); }
    private static void deny(Player p)        { p.sendMessage(Component.text("No permission.", NamedTextColor.RED)); }
    private static void usage(Player p, String u) { p.sendMessage(Component.text("Usage: " + u, NamedTextColor.RED)); }
    private static Component info(String k, String v) {
        return Component.text(k + ": ", NamedTextColor.GRAY).append(Component.text(v, NamedTextColor.WHITE));
    }
}
