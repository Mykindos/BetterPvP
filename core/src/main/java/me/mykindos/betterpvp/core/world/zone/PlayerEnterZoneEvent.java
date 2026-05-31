package me.mykindos.betterpvp.core.world.zone;

import lombok.EqualsAndHashCode;
import lombok.Value;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import org.bukkit.entity.Player;

/**
 * Fired when a player enters a {@link Zone} (after having exited their previous one).
 */
@EqualsAndHashCode(callSuper = true)
@Value
public class PlayerEnterZoneEvent extends CustomEvent {

    Player player;
    Zone zone;
}
