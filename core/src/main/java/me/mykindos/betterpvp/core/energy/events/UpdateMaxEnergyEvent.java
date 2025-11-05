package me.mykindos.betterpvp.core.energy.events;

import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;
import org.bukkit.entity.Player;

@Getter
@Setter
public class UpdateMaxEnergyEvent extends CustomCancellableEvent {
    private final Player player;
    private double newMax;

    public UpdateMaxEnergyEvent(Player player, double newMax) {
        super(true);
        this.player = player;
        this.newMax = newMax;
    }
}
