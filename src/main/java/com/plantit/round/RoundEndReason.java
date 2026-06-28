package com.plantit.round;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

public enum RoundEndReason {

    T_ELIMINATED("Terrorists Eliminated", NamedTextColor.BLUE, false),
    CT_ELIMINATED("Counter-Terrorists Eliminated", NamedTextColor.RED, true),
    BOMB_EXPLODED("Bomb Has Exploded", NamedTextColor.RED, true),
    BOMB_DEFUSED("Bomb Has Been Defused", NamedTextColor.BLUE, false),
    TIME_EXPIRED("Time Expired", NamedTextColor.BLUE, false);

    private final String message;
    private final TextColor color;
    /** True if Terrorists win this round. */
    private final boolean tWin;

    RoundEndReason(String message, TextColor color, boolean tWin) {
        this.message = message;
        this.color = color;
        this.tWin = tWin;
    }

    public String getMessage() {
        return message;
    }

    public TextColor getColor() {
        return color;
    }

    public boolean isTWin() {
        return tWin;
    }
}
