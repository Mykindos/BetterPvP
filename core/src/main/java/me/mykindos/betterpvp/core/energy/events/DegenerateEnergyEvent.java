package me.mykindos.betterpvp.core.energy.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;
import org.bukkit.entity.Player;

@EqualsAndHashCode(callSuper = true)
@Data
public class DegenerateEnergyEvent extends CustomCancellableEvent {

    private final Player player;
    private final double energy;



}
