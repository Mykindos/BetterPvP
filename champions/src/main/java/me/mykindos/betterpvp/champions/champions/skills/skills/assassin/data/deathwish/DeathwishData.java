package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.data.deathwish;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

@Data
public class DeathwishData {
    private @NotNull DeathwishThreshold currentThreshold = DeathwishThreshold.NONE;

    /**
     * The time at which the last effect was applied, used to determine when to remove the effect after the duration expires.
     * <p>
     * Note: If a player reaches a new threshold while an effect is active, the timer will reset to the time of the new
     * effect application, so the effect will last for the full duration from that point.
     */
    private long lastEffectTime = -1;

    private boolean reachedMaxThreshold = false;
}
