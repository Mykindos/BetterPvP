package me.mykindos.betterpvp.core.components.clans.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import org.bukkit.Location;

@EqualsAndHashCode(callSuper = true)
@Data
public class ClansDropEnergyEvent extends CustomEvent {

    private final Location location;
    private final int amount;

}
