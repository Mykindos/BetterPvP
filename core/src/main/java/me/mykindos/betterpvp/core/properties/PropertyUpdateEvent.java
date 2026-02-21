package me.mykindos.betterpvp.core.properties;

import java.util.concurrent.ConcurrentHashMap;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;
import org.jetbrains.annotations.Nullable;

@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Data
public class PropertyUpdateEvent<T extends PropertyContainer> extends CustomCancellableEvent {

    /**
     * The container this property is apart of
     */
    private final T container;
    /**
     * The property/key of the updated value
     */
    private final String property;
    /**
     * The new value for this property
     */
    private final Object newValue;
    /**
     * The old value for this property, {@code null} if no previous value
     * @see ConcurrentHashMap#put(Object, Object)
     */
    @Nullable
    private final Object oldValue;

}
