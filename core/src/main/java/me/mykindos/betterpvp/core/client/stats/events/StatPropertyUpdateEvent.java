package me.mykindos.betterpvp.core.client.stats.events;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.impl.IStat;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;

@EqualsAndHashCode(callSuper = true)
@Getter
@Setter
@ToString
public class StatPropertyUpdateEvent extends CustomCancellableEvent {

    private final StatContainer container;
    private final IStat stat;
    private final Long newValue;
    private final Long oldValue;

    public StatPropertyUpdateEvent(StatContainer container, IStat stat, Long newValue, Long oldValue) {
        super(true); // always fired from an async thread
        this.container = container;
        this.stat = stat;
        this.newValue = newValue;
        this.oldValue = oldValue;
    }
}
