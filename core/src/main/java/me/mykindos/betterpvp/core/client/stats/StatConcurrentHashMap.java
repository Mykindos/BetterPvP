package me.mykindos.betterpvp.core.client.stats;

import lombok.CustomLog;
import lombok.Data;
import lombok.Getter;
import me.mykindos.betterpvp.core.client.stats.events.IStatMapListener;
import me.mykindos.betterpvp.core.client.stats.impl.IStat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@CustomLog
public class StatConcurrentHashMap implements Iterable<StatConcurrentHashMap.StatData> {
    @Getter
    //period, statname, stat
    protected final ConcurrentHashMap<String, ConcurrentHashMap<IStat, Long>> myMap = new ConcurrentHashMap<>();
    protected final List<IStatMapListener> listeners = new ArrayList<>();

    /**
     * Put the specified stat in this map;
     *
     * @param period
     * @param stat
     * @param value
     * @param silent
     */
    public void put(String period, @NotNull IStat stat, Long value, boolean silent) {
        AtomicReference<Long> oldValue = new AtomicReference<>();
        myMap.compute(period, (k, v) -> {
            if (v == null) {
                v = new ConcurrentHashMap<>();
            }
            oldValue.set(v.put(stat, value));
            return v;
        });
        if (!silent) {
            listeners.forEach(l -> l.onMapValueChanged(stat, value, oldValue.get()));
        }
    }

    public void increase(String period, IStat stat, Long amount) {
        synchronized (myMap) {
            Long newValue = myMap.computeIfAbsent(period, (k) -> new ConcurrentHashMap<>())
                    .compute(stat, (sk, sv) -> sv == null ? amount : sv + amount);
            Long oldValue = newValue - amount;
            listeners.forEach(l -> l.onMapValueChanged(stat, newValue, oldValue));
        }
    }

    @Nullable
    public Long get(String period, IStat stat) {
        if (StatContainer.GLOBAL_PERIOD_KEY.equals(period)) return getAll(stat);

        final ConcurrentHashMap<IStat, Long> periodMap = myMap.get(period);
        if (periodMap == null) return null;
        return periodMap.get(stat);
    }

    public Long getAll(IStat stat) {
        return myMap.values().stream()
                .mapToLong(map -> Optional.ofNullable(map.get(stat)).orElse(0L))
                .sum();
    }

    /**
     * Get all the stats from a period
     * @param period the period or {@code "Global"} if global
     * @return
     */
    public Map<IStat, Long> getStatsOfPeriod(@NotNull String period) {
        if (StatContainer.GLOBAL_PERIOD_KEY.equals(period)) {
            final Map<IStat, Long> globalMap = new ConcurrentHashMap<>();
            myMap.values().forEach(map -> {
                map.forEach((statName, stat) ->
                    globalMap.compute(statName, (key, value) ->
                            value == null ? stat : value + stat
                            )
                );
            });
            return globalMap;
        }
        final Map<IStat, Long> periodMap = myMap.get(period);
        if (periodMap != null) return periodMap;
        return Map.of();
    }

    public void registerListener(IStatMapListener listener) {
        listeners.add(listener);
    }

    public void unregisterListener(IStatMapListener listener) {
        listeners.remove(listener);
    }

    public StatConcurrentHashMap copyFrom(StatConcurrentHashMap other) {
        myMap.clear();
        myMap.putAll(other.getMyMap());
        return this;
    }

    /**
     * Returns an iterator over elements of type {@code T}.
     *
     * @return an Iterator.
     */
    @Override
    public @NotNull Iterator<StatData> iterator() {
        return myMap.entrySet().stream().flatMap(entry -> {
                final String period = entry.getKey();
                return entry.getValue().entrySet().stream().map(e -> {
                    final IStat stat = e.getKey();
                    return new StatData(period, stat, e.getValue());
                });
            }).iterator();
    }

    @Data
    public static class StatData {
        private final String period;
        private final IStat stat;
        private final Long value;
    }
}
