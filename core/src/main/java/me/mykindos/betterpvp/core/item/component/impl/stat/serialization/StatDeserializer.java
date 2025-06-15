package me.mykindos.betterpvp.core.item.component.impl.stat.serialization;

import me.mykindos.betterpvp.core.item.component.impl.stat.ItemStat;
import me.mykindos.betterpvp.core.item.serialization.CustomDeserializer;

/**
 * Interface for deserializing stats from persistent data containers.
 * This separates the responsibility of persistence from the stat behavior.
 * 
 * @param <T> The type of stat this deserializer creates
 */
public interface StatDeserializer<T extends ItemStat<?>> extends CustomDeserializer<T> {

} 