package me.mykindos.betterpvp.core.properties;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;

@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Data
public class PropertyUpdateEvent<T extends PropertyContainer> extends CustomCancellableEvent {

    private final T container;
    private final String property;
    private final Object value;

}
