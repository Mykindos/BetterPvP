package me.mykindos.betterpvp.core.combat.throwables.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.combat.throwables.ThrowableItem;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;

@EqualsAndHashCode(callSuper = true)
@Data
public class ThrowableHitEvent extends CustomCancellableEvent {

    protected final ThrowableItem throwable;

}
