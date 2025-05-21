package me.mykindos.betterpvp.core.energy.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bukkit.entity.Player;

@EqualsAndHashCode(callSuper = true)
@Data
public class RegenerateEnergyEvent extends EnergyEvent {

    private final Player player;
    private double energy;


    public RegenerateEnergyEvent(Player player, double energy, EnergyEvent.CAUSE cause) {
        super(cause);
        this.player = player;
        this.energy = energy;
    }

}
