package me.mykindos.betterpvp.core.client.stats.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.impl.IStat;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;

@EqualsAndHashCode(callSuper = true)
@Data
public class StatPropertyUpdateEvent extends CustomEvent {

    private final StatContainer container;
    private final IStat stat;
    private final Long newValue;
    private final Long oldValue;
}
