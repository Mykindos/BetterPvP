package me.mykindos.betterpvp.clans.clans.fatigue;

import lombok.Getter;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

/**
 * Severity bands a player's battle fatigue can fall into.
 * <p>
 * Score thresholds live in config (resolved by {@link BattleFatigueManager});
 * per-tier punishment magnitudes (hold duration, slowness) are owned by each
 * punishment strategy. This enum only carries presentation metadata.
 */
@Getter
public enum FatigueTier {

    FRESH("clans.fatigue.tier.rested", NamedTextColor.GREEN),
    WORN("clans.fatigue.tier.worn", NamedTextColor.YELLOW),
    WEARY("clans.fatigue.tier.weary", NamedTextColor.GOLD),
    EXHAUSTED("clans.fatigue.tier.exhausted", NamedTextColor.RED);

    private final String displayName;
    private final TextColor color;

    FatigueTier(String displayName, TextColor color) {
        this.displayName = displayName;
        this.color = color;
    }

    /**
     * @return whether this tier should trap the player in the respawn hold on death.
     */
    public boolean requiresHold() {
        return ordinal() >= WORN.ordinal();
    }

}
