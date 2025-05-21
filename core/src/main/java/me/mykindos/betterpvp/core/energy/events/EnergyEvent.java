package me.mykindos.betterpvp.core.energy.events;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;

@EqualsAndHashCode(callSuper = true)
@Getter
public abstract class EnergyEvent extends CustomCancellableEvent {
    private final CAUSE cause;

    protected EnergyEvent(CAUSE cause) {
        this.cause = cause;
    }


    public enum CAUSE {
        /**
         * This event was caused by using energy
         */
        USE,
        /**
         * This event was caused by natural events (i.e. normal regeneration)
         */
        NATURAL,
        /**
         * This event was caused by a custom reason (o.e. a direct transfer)
         */
        CUSTOM
    }
}
