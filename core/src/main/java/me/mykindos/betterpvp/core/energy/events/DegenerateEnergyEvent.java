package me.mykindos.betterpvp.core.energy.events;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;

@EqualsAndHashCode(callSuper = true)
@Getter
@Setter
public class DegenerateEnergyEvent extends EnergyEvent {

    private final Player player;
    private double energy;

    public DegenerateEnergyEvent(Player player, double energy, Cause cause) {
        super(cause);
        this.player = player;
        this.energy = energy;
    }
}
