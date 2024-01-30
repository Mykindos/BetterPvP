package me.mykindos.betterpvp.core.combat.throwables.events;

import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.combat.throwables.ThrowableItem;
import org.bukkit.Location;

@EqualsAndHashCode(callSuper = true)
public class ThrowableHitGroundEvent extends ThrowableHitEvent {

    private final Location location;

    public ThrowableHitGroundEvent(ThrowableItem throwable, Location location) {
        super(throwable);
        this.location = location;
    }
}
