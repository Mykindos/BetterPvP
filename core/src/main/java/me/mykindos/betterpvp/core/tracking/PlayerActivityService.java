package me.mykindos.betterpvp.core.tracking;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.tracking.model.GridKey;
import me.mykindos.betterpvp.core.tracking.model.HeatCell;
import me.mykindos.betterpvp.core.tracking.model.ZoneClassification;
import org.bukkit.Location;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Owns the heatmap grid and exposes the public API for recording activity and querying heat data.
 * <p>
 * All grid mutations must occur on the main server thread.
 * Use {@link #getSnapshot()} to obtain an immutable view safe for off-thread reads.
 */
@Singleton
public class PlayerActivityService {

    @Inject
    @Config(path = "activity.decay.factor", defaultValue = "0.85")
    private double decayFactor;

    @Inject
    @Config(path = "activity.decay.dead-cell", defaultValue = "0.01")
    private double deadCellThreshold;

    @Inject
    @Config(path = "activity.weight.presence", defaultValue = "1.0")
    private double presenceWeight;

    @Inject
    @Config(path = "activity.weight.combat", defaultValue = "1.0")
    private double combatWeight;

    @Inject
    @Config(path = "activity.weight.kill", defaultValue = "5.0")
    private double killWeight;

    @Inject
    @Config(path = "activity.threshold.hotspot", defaultValue = "300.0")
    private double hotspotThreshold;

    @Inject
    @Config(path = "activity.threshold.active", defaultValue = "150.0")
    private double activeThreshold;

    @Inject
    @Config(path = "activity.threshold.quiet", defaultValue = "60.0")
    private double quietThreshold;

    // Main-thread-only grid; plain HashMap is sufficient.
    private final Map<GridKey, HeatCell> cells = new HashMap<>();

    // Latest immutable snapshot — volatile so async threads always see the newest reference.
    private volatile ActivitySnapshot latestSnapshot;

    public void recordPresence(Location location) {
        cells.computeIfAbsent(GridKey.of(location), k -> new HeatCell())
                .addPresence(presenceWeight);
    }

    public void recordCombat(Location location) {
        cells.computeIfAbsent(GridKey.of(location), k -> new HeatCell())
                .addCombat(combatWeight);
    }

    public void recordKill(Location location) {
        cells.computeIfAbsent(GridKey.of(location), k -> new HeatCell())
                .addCombat(killWeight);
    }

    /**
     * Applies exponential decay to all cells and removes cells that have cooled below
     * the dead-cell threshold, keeping the map sparse.
     * Call periodically from the main thread.
     */
    public void decay() {
        cells.values().forEach(cell -> cell.decay(decayFactor));
        cells.entrySet().removeIf(e -> e.getValue().getHeatValue() < deadCellThreshold);
    }

    /**
     * Builds and stores a new immutable snapshot from the current grid state.
     *
     * @param liveCounts per-cell current player counts computed immediately before this call
     * @return the freshly built snapshot
     */
    public ActivitySnapshot refreshSnapshot(Map<GridKey, Integer> liveCounts) {
        List<ActivitySnapshot.Entry> entries = cells.entrySet().stream()
                .map(e -> {
                    GridKey key = e.getKey();
                    HeatCell cell = e.getValue();
                    return new ActivitySnapshot.Entry(
                            key,
                            cell.getHeatValue(),
                            cell.getPeakHeat(),
                            liveCounts.getOrDefault(key, 0),
                            cell.getCombatEvents(),
                            classify(cell.getHeatValue())
                    );
                })
                .sorted(Comparator.comparingDouble(ActivitySnapshot.Entry::heatValue).reversed())
                .toList();

        latestSnapshot = new ActivitySnapshot(System.currentTimeMillis(), entries);
        return latestSnapshot;
    }

    /** Returns the latest snapshot, or {@code null} if none has been generated yet. */
    public ActivitySnapshot getSnapshot() {
        return latestSnapshot;
    }

    public double getHeatAt(Location location) {
        HeatCell cell = cells.get(GridKey.of(location));
        return cell == null ? 0.0 : cell.getHeatValue();
    }

    public ZoneClassification getClassification(Location location) {
        return classify(getHeatAt(location));
    }

    /**
     * Returns the top {@code n} hottest cells from the latest snapshot.
     * Returns an empty list if no snapshot exists yet.
     */
    public List<ActivitySnapshot.Entry> getTopHotspots(int n) {
        ActivitySnapshot snapshot = latestSnapshot;
        if (snapshot == null) return List.of();
        return snapshot.entries().stream().limit(n).toList();
    }

    public Map<GridKey, HeatCell> getCells() {
        return Collections.unmodifiableMap(cells);
    }

    public ZoneClassification classify(double heat) {
        if (heat >= hotspotThreshold) return ZoneClassification.HOTSPOT;
        if (heat >= activeThreshold) return ZoneClassification.ACTIVE;
        if (heat >= quietThreshold) return ZoneClassification.QUIET;
        return ZoneClassification.EMPTY;
    }

}
