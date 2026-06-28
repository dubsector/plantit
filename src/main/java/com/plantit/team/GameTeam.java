package com.plantit.team;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

public enum GameTeam {

    T("Terrorist", NamedTextColor.RED),
    CT("Counter-Terrorist", NamedTextColor.BLUE),
    SPECTATOR("Spectator", NamedTextColor.GRAY);

    private final String displayName;
    private final TextColor color;

    GameTeam(String displayName, TextColor color) {
        this.displayName = displayName;
        this.color = color;
    }

    public String getDisplayName() {
        return displayName;
    }

    public TextColor getColor() {
        return color;
    }
}
