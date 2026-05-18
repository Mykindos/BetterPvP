package me.mykindos.betterpvp.clans.clans.fatigue.events;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import org.bukkit.entity.Player;

/**
 * Fired on every fatigue gain that did <i>not</i> change the player's tier
 * (a tier change fires {@link PlayerFatigueTierChangeEvent} instead). Drives the
 * lighter "your body aches" chat flavor without coupling scoring to presentation.
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public class PlayerFatigueGainEvent extends CustomEvent {

    private final Player player;
    private final double previousScore;
    private final double newScore;

    public PlayerFatigueGainEvent(Player player, double previousScore, double newScore) {
        this.player = player;
        this.previousScore = previousScore;
        this.newScore = newScore;
    }

    public double getDelta() {
        return newScore - previousScore;
    }
}
