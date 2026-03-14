package me.mykindos.betterpvp.core.tracking;

import me.mykindos.betterpvp.core.tracking.model.GridKey;
import me.mykindos.betterpvp.core.tracking.model.ZoneClassification;

import java.util.List;

/**
 * Immutable point-in-time view of the heatmap grid.
 * Entries are sorted by heat value descending — index 0 is always the hottest cell.
 * Created on the main thread; safe to read from any thread.
 */
public record ActivitySnapshot(long timestamp, List<Entry> entries) {

    public record Entry(
            GridKey key,
            double heatValue,
            double peakHeat,
            int currentPlayers,
            int combatEvents,
            ZoneClassification classification
    ) {}

}
