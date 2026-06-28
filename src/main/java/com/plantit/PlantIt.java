package com.plantit;

import com.plantit.config.GameConfig;
import com.plantit.messaging.GameMessenger;
import com.plantit.round.RoundListener;
import com.plantit.round.RoundManager;
import com.plantit.team.TeamManager;
import org.bukkit.plugin.java.JavaPlugin;

public class PlantIt extends JavaPlugin {

    private static PlantIt instance;

    private GameConfig gameConfig;
    private TeamManager teamManager;
    private RoundManager roundManager;
    private GameMessenger gameMessenger;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        gameConfig = new GameConfig(getConfig());
        teamManager = new TeamManager(this);
        gameMessenger = new GameMessenger(this);
        roundManager = new RoundManager(this, teamManager, gameConfig, gameMessenger);

        getServer().getPluginManager().registerEvents(
                new RoundListener(this, roundManager, teamManager), this);

        getLogger().info("Plant It enabled.");
    }

    @Override
    public void onDisable() {
        if (roundManager != null) roundManager.shutdown();
        if (gameMessenger != null) gameMessenger.shutdown();
        getLogger().info("Plant It disabled.");
    }

    public static PlantIt getInstance() {
        return instance;
    }

    public GameConfig getGameConfig() {
        return gameConfig;
    }

    public TeamManager getTeamManager() {
        return teamManager;
    }

    public RoundManager getRoundManager() {
        return roundManager;
    }
}
