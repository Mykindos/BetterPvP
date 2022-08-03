package me.mykindos.betterpvp.clans.energy.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

@EqualsAndHashCode(callSuper = true)
@Data
public class RegenerateEnergyEvent extends CustomEvent {

    private final Player player;
    private final double energy;

}
