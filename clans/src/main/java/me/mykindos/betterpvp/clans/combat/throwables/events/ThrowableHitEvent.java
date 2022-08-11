package me.mykindos.betterpvp.clans.combat.throwables.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.clans.combat.throwables.ThrowableItem;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;

@EqualsAndHashCode(callSuper = true)
@Data
public class ThrowableHitEvent extends CustomEvent {

    protected final ThrowableItem throwable;

}
