package me.mykindos.betterpvp.core.client.stats;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;

@EqualsAndHashCode(callSuper = true)
@Data
public class StatPropertyUpdateEvent extends CustomEvent {

    private final StatContainer container;
    private final String statName;
    private final Double newValue;
    private final Double oldValue;
}
