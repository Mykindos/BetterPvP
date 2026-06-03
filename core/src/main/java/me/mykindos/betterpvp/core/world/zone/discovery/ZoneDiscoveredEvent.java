package me.mykindos.betterpvp.core.world.zone.discovery;

import lombok.EqualsAndHashCode;
import lombok.Value;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import me.mykindos.betterpvp.core.world.zone.Zone;
import org.bukkit.entity.Player;

/**
 * Fired the first time a player discovers a {@link Zone#isDiscoverable() discoverable} zone (after the notification has
 * been shown and the discovery scheduled for persistence). Other modules can listen for this to grant rewards,
 * achievements, etc. without coupling to the discovery system.
 */
@EqualsAndHashCode(callSuper = true)
@Value
public class ZoneDiscoveredEvent extends CustomEvent {

    Player player;
    Zone zone;
}
