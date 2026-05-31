package me.mykindos.betterpvp.core.world.zone;

import lombok.EqualsAndHashCode;
import lombok.Value;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import org.bukkit.entity.Player;

/**
 * Fired when a player leaves a {@link Zone}. The carried zone is the one being left.
 */
@EqualsAndHashCode(callSuper = true)
@Value
public class PlayerExitZoneEvent extends CustomEvent {

    Player player;
    Zone zone;
}
