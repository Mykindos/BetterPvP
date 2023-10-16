package me.mykindos.betterpvp.core.energy.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;
import org.bukkit.entity.Player;

@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Data
public class RegenerateEnergyEvent extends CustomCancellableEvent {

    private final Player player;
    private double energy;

}
