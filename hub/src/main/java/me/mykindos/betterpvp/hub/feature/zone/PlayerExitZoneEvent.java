package me.mykindos.betterpvp.hub.feature.zone;

import lombok.EqualsAndHashCode;
import lombok.Value;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import org.bukkit.entity.Player;

@EqualsAndHashCode(callSuper = true)
@Value
public class PlayerExitZoneEvent extends CustomEvent {

    Player player;
    Zone zone;

}
