package me.mykindos.betterpvp.clans.clans.fatigue.events;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.mykindos.betterpvp.clans.clans.fatigue.FatigueTier;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import org.bukkit.entity.Player;

/**
 * Fired when a player crosses from one {@link FatigueTier} to another, in either
 * direction. The scoring layer fires this and is done; punishment and messaging
 * listen independently, keeping those concerns fully decoupled.
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public class PlayerFatigueTierChangeEvent extends CustomEvent {

    private final Player player;
    private final FatigueTier oldTier;
    private final FatigueTier newTier;

    public PlayerFatigueTierChangeEvent(Player player, FatigueTier oldTier, FatigueTier newTier) {
        this.player = player;
        this.oldTier = oldTier;
        this.newTier = newTier;
    }

    /** @return true when the player got worse (moved up a tier). */
    public boolean isEscalation() {
        return newTier.ordinal() > oldTier.ordinal();
    }
}
