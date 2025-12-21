package me.mykindos.betterpvp.core.client.stats;

import lombok.CustomLog;
import lombok.Data;
import lombok.Getter;
import me.mykindos.betterpvp.core.client.stats.events.IStatMapListener;
import me.mykindos.betterpvp.core.client.stats.impl.IStat;
import me.mykindos.betterpvp.core.server.Period;
import me.mykindos.betterpvp.core.server.Realm;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

@CustomLog
public class StatConcurrentHashMap implements Iterable<StatConcurrentHashMap.StatData> {
    @Getter
    //realm, istat, value
    protected final ConcurrentMap<Realm, ConcurrentMap<IStat, Long>> myMap = new ConcurrentHashMap<>();
    protected final List<IStatMapListener> listeners = new ArrayList<>();

    /**
     * Put the specified stat in this map;
     *
     * @param realm
     * @param stat
     * @param value
     * @param silent
     */
    public void put(Realm realm, @NotNull IStat stat, Long value, boolean silent) {
        AtomicReference<Long> oldValue = new AtomicReference<>();
        myMap.compute(realm, (k, v) -> {
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

    public void increase(Realm realm, IStat stat, Long amount) {
        synchronized (myMap) {
            Long newValue = myMap.computeIfAbsent(realm, (k) -> new ConcurrentHashMap<>())
                    .compute(stat, (sk, sv) -> sv == null ? amount : sv + amount);
            Long oldValue = newValue - amount;
            listeners.forEach(l -> l.onMapValueChanged(stat, newValue, oldValue));
        }
    }

    @Nullable
    public Long get(StatFilterType type, @Nullable Period period, IStat stat) {
        if (type == StatFilterType.ALL) {
            return getAll(stat);
        }

        if (type == StatFilterType.REALM) {
            if (!(period instanceof Realm realm)) throw new ClassCastException("Object passed when StatFilterType is REALM must be Realm, found: " + period);
            final ConcurrentMap<IStat, Long> realmMap = myMap.get(realm);
            if (realmMap == null) return null;
            return realmMap.get(stat);
        }

        return myMap.entrySet().stream()
                .filter(entry -> type.filter(period, entry.getKey()))
                .mapToLong(entry -> Optional.ofNullable(entry.getValue().get(stat)).orElse(0L))
                .sum();

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
    public Map<IStat, Long> getStatsOfPeriod(StatFilterType type, @Nullable Period period) {

        if (type == StatFilterType.REALM) {
            if (!(period instanceof Realm realm))
                throw new ClassCastException("Object passed when StatFilterType is REALM must be Realm, found: " + period);
            final ConcurrentMap<IStat, Long> realmMap = myMap.get(realm);
            if (realmMap != null) return realmMap;
            return Map.of();
        }
        final Map<IStat, Long> globalMap = new ConcurrentHashMap<>();
        myMap.entrySet()
                .stream()
                .filter(entry -> type.filter(period, entry.getKey()))
                .map(Map.Entry::getValue)
                .forEach(map -> {
                    map.forEach((statName, stat) ->
                    globalMap.compute(statName, (key, value) ->
                            value == null ? stat : value + stat
                    )
            );
        });
        return globalMap;
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
                final Realm realm = entry.getKey();
                return entry.getValue().entrySet().stream().map(e -> {
                    final IStat stat = e.getKey();
                    return new StatData(realm, stat, e.getValue());
                });
            }).iterator();
    }

    @Data
    public static class StatData {
        private final Realm realm;
        private final IStat stat;
        private final Long value;

        @Override
        public String toString() {
            return "StatData{" +
                    "realm='" + realm + '\'' +
                    ", stat=" + stat.getQualifiedName() +
                    ", value=" + value +
                    '}';
        }
    }
}
