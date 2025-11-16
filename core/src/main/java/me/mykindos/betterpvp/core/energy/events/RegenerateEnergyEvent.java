package me.mykindos.betterpvp.core.energy.events;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;

@EqualsAndHashCode(callSuper = true)
@Getter
@Setter
public class RegenerateEnergyEvent extends EnergyEvent {

    private final Player player;
    private double energy;


    public RegenerateEnergyEvent(Player player, double energy, Cause cause) {
        super(cause);
        this.player = player;
        this.energy = energy;
    }

}
