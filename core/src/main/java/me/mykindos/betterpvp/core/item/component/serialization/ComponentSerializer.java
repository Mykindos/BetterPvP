package me.mykindos.betterpvp.core.item.component.serialization;

import me.mykindos.betterpvp.core.item.component.ItemComponent;
import me.mykindos.betterpvp.core.item.serialization.CustomSerializer;

/**
 * Interface for serializing components to persistent data containers.
 * This separates the responsibility of persistence from the component behavior.
 * 
 * @param <T> The type of component this serializer handles
 */
public interface ComponentSerializer<T extends ItemComponent> extends CustomSerializer<T> {

}