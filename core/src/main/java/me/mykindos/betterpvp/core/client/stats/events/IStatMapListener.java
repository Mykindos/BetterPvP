package me.mykindos.betterpvp.core.client.stats.events;

import me.mykindos.betterpvp.core.client.stats.impl.IStat;
import org.jetbrains.annotations.Nullable;

public interface IStatMapListener {
    void onMapValueChanged(IStat stat, Double newValue, @Nullable Double oldValue);
}
