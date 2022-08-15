package me.mykindos.betterpvp.core.combat.throwables.events;

import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.combat.throwables.ThrowableItem;

@EqualsAndHashCode(callSuper = true)
public class ThrowableHitGroundEvent extends ThrowableHitEvent {

    public ThrowableHitGroundEvent(ThrowableItem throwable) {
        super(throwable);
    }
}
