package me.mykindos.betterpvp.clans.combat.throwables.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.clans.combat.throwables.ThrowableItem;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;

@EqualsAndHashCode(callSuper = true)
public class ThrowableHitGroundEvent extends ThrowableHitEvent {

    public ThrowableHitGroundEvent(ThrowableItem throwable) {
        super(throwable);
    }
}
