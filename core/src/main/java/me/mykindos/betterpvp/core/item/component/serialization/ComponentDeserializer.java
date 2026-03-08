package me.mykindos.betterpvp.core.item.component.serialization;

import me.mykindos.betterpvp.core.item.component.ItemComponent;
import me.mykindos.betterpvp.core.item.serialization.CustomDeserializer;

/**
 * Interface for deserializing components from persistent data containers.
 * This separates the responsibility of persistence from the component behavior.
 * 
 * @param <T> The type of component this deserializer creates
 */
public interface ComponentDeserializer<T extends ItemComponent> extends CustomDeserializer<T> {

} 