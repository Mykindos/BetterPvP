package me.mykindos.betterpvp.core.item.component.impl.stat.serialization;

import me.mykindos.betterpvp.core.item.component.impl.stat.ItemStat;
import me.mykindos.betterpvp.core.item.serialization.CustomSerializer;

/**
 * Interface for serializing stats to persistent data containers.
 * This separates the responsibility of persistence from the stat behavior.
 * 
 * @param <T> The type of stat this serializer handles
 */
public interface StatSerializer<T extends ItemStat<?>> extends CustomSerializer<T> {

} 