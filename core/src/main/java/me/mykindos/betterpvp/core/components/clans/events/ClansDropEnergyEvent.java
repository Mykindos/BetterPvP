package me.mykindos.betterpvp.core.components.clans.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

@EqualsAndHashCode(callSuper = true)
@Data
public class ClansDropEnergyEvent extends CustomEvent {

    /**
     * The {@link LivingEntity} that caused this event (i.e. block breaker, killer)
     */
    private final LivingEntity livingEntity;
    private final Location location;
    private final int amount;

}