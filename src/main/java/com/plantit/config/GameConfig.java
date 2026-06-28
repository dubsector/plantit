package com.plantit.config;

import org.bukkit.configuration.file.FileConfiguration;

public class GameConfig {

    private final FileConfiguration config;

    public GameConfig(FileConfiguration config) {
        this.config = config;
    }

    public int getMinPlayers() {
        return config.getInt("game.min-players", 2);
    }

    public int getMaxRounds() {
        return config.getInt("game.max-rounds", 24);
    }

    public int getOvertimeRounds() {
        return config.getInt("game.overtime-rounds", 6);
    }

    public int getFreezeDuration() {
        return config.getInt("phases.freeze-duration", 15);
    }

    public int getRoundDuration() {
        return config.getInt("phases.round-duration", 115);
    }

    public int getRoundEndDelay() {
        return config.getInt("phases.round-end-delay", 5);
    }
}
