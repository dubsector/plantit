package com.plantit.round;

public enum RoundPhase {
    /** Not enough players or between maps. */
    WAITING,
    /** Players are frozen; buy menu is open. */
    FREEZE,
    /** Round is live. */
    LIVE,
    /** Round has ended; showing results before next round. */
    ROUND_END
}
