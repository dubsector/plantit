package com.plantit;

import com.plantit.bomb.BombListener;
import com.plantit.bomb.BombManager;
import com.plantit.command.PlantItCommand;
import com.plantit.config.GameConfig;
import com.plantit.economy.EconomyManager;
import com.plantit.hud.HudManager;
import com.plantit.map.MapManager;
import com.plantit.messaging.GameMessenger;
import com.plantit.round.RoundListener;
import com.plantit.round.RoundManager;
import com.plantit.team.TeamManager;
import org.bukkit.plugin.java.JavaPlugin;

public class PlantIt extends JavaPlugin {

    private static PlantIt instance;

    private GameConfig gameConfig;
    private TeamManager teamManager;
    private MapManager mapManager;
    private EconomyManager economyManager;
    private BombManager bombManager;
    private HudManager hudManager;
    private RoundManager roundManager;
    private GameMessenger gameMessenger;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        gameConfig    = new GameConfig(getConfig());
        teamManager   = new TeamManager(this);
        mapManager    = new MapManager(this);
        economyManager = new EconomyManager(this, teamManager);
        gameMessenger = new GameMessenger(this);
        bombManager   = new BombManager(this, teamManager, economyManager, mapManager);
        roundManager  = new RoundManager(this, teamManager, gameConfig, gameMessenger,
                                          mapManager, economyManager, bombManager);
        bombManager.setRoundManager(roundManager);
        hudManager    = new HudManager(this, roundManager, teamManager);

        getServer().getPluginManager().registerEvents(
                new RoundListener(this, roundManager, teamManager, economyManager, bombManager, hudManager), this);
        getServer().getPluginManager().registerEvents(
                new BombListener(bombManager, roundManager, teamManager), this);

        PlantItCommand cmd = new PlantItCommand(this, mapManager, roundManager, economyManager);
        getCommand("plantit").setExecutor(cmd);
        getCommand("plantit").setTabCompleter(cmd);

        getLogger().info("Plant It enabled.");
    }

    @Override
    public void onDisable() {
        if (bombManager  != null) bombManager.reset();
        if (roundManager != null) roundManager.shutdown();
        if (gameMessenger != null) gameMessenger.shutdown();
        getLogger().info("Plant It disabled.");
    }

    public static PlantIt getInstance() { return instance; }
    public GameConfig getGameConfig()   { return gameConfig; }
    public TeamManager getTeamManager() { return teamManager; }
    public RoundManager getRoundManager() { return roundManager; }
    public MapManager getMapManager()   { return mapManager; }
    public EconomyManager getEconomyManager() { return economyManager; }
    public BombManager getBombManager() { return bombManager; }
}
