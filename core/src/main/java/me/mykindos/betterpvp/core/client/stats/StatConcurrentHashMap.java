package me.mykindos.betterpvp.core.client.stats;

import lombok.CustomLog;
import lombok.Data;
import lombok.Getter;
import me.mykindos.betterpvp.core.client.stats.events.IStatMapListener;
import me.mykindos.betterpvp.core.client.stats.impl.IStat;
import me.mykindos.betterpvp.core.client.stats.impl.IWrapperStat;
import me.mykindos.betterpvp.core.server.Period;
import me.mykindos.betterpvp.core.server.Realm;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

@CustomLog
public class StatConcurrentHashMap implements Iterable<StatConcurrentHashMap.StatData> {
    @Getter
    //realm, istat, value
    protected final ConcurrentMap<Realm, ConcurrentMap<IStat, Long>> myMap = new ConcurrentHashMap<>();
    protected final List<IStatMapListener> listeners = new CopyOnWriteArrayList<>();

    /** Cached aggregate over ALL realms: stat → sum */
    private final ConcurrentMap<IStat, Long> allMap = new ConcurrentHashMap<>();
    /** Cached aggregate per Season: season → stat → sum */
    private final ConcurrentMap<Season, ConcurrentMap<IStat, Long>> seasonMap = new ConcurrentHashMap<>();

    /**
     * Leaf aggregate indexes: keyed by the unwrapped root stat (after stripping all {@link IWrapperStat} layers).
     * These enable O(1) lookups for {@link me.mykindos.betterpvp.core.client.stats.impl.GenericStat} instead
     * of O(n) iteration over the full stat map.
     */
    private final ConcurrentMap<IStat, Long> leafAllMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<Season, ConcurrentMap<IStat, Long>> leafSeasonMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<Realm, ConcurrentMap<IStat, Long>> leafRealmMap = new ConcurrentHashMap<>();

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
        // keep secondary indexes in sync
        long delta = value - (oldValue.get() == null ? 0L : oldValue.get());
        if (delta != 0) {
            allMap.merge(stat, delta, Long::sum);
            seasonMap.computeIfAbsent(realm.getSeason(), s -> new ConcurrentHashMap<>())
                    .merge(stat, delta, Long::sum);
            // leaf indexes
            IStat leaf = getLeaf(stat);
            leafAllMap.merge(leaf, delta, Long::sum);
            leafSeasonMap.computeIfAbsent(realm.getSeason(), s -> new ConcurrentHashMap<>())
                    .merge(leaf, delta, Long::sum);
            leafRealmMap.computeIfAbsent(realm, r -> new ConcurrentHashMap<>())
                    .merge(leaf, delta, Long::sum);
        }
        if (!silent) {
            listeners.forEach(l -> l.onMapValueChanged(stat, value, oldValue.get()));
        }
    }

    public void increase(Realm realm, IStat stat, Long amount) {
        final long newValue;
        final long oldValue;
        synchronized (myMap) {
            long nv = myMap.computeIfAbsent(realm, (k) -> new ConcurrentHashMap<>())
                    .compute(stat, (sk, sv) -> sv == null ? amount : sv + amount);
            newValue = nv;
            oldValue = nv - amount;
            // keep secondary indexes in sync
            allMap.merge(stat, amount, Long::sum);
            seasonMap.computeIfAbsent(realm.getSeason(), s -> new ConcurrentHashMap<>())
                    .merge(stat, amount, Long::sum);
            // leaf indexes
            IStat leaf = getLeaf(stat);
            leafAllMap.merge(leaf, amount, Long::sum);
            leafSeasonMap.computeIfAbsent(realm.getSeason(), s -> new ConcurrentHashMap<>())
                    .merge(leaf, amount, Long::sum);
            leafRealmMap.computeIfAbsent(realm, r -> new ConcurrentHashMap<>())
                    .merge(leaf, amount, Long::sum);
        }
        // notify listeners outside the lock so writes are not blocked during (potentially async) dispatch
        listeners.forEach(l -> l.onMapValueChanged(stat, newValue, oldValue));
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

    /**
     * Fast O(1) lookup for the aggregate value of all stats that share the given leaf (root) stat.
     * <p>
     * A "leaf" stat is the non-{@link IWrapperStat} root obtained by recursively unwrapping wrapper chains.
     * For example, {@code ClanWrapperStat(wrappedStat=KILLS)} and
     * {@code GameTeamMapWrapperStat(wrappedStat=ClanWrapperStat(wrappedStat=KILLS))} both have leaf {@code KILLS}.
     * </p>
     *
     * @param type     the filter type
     * @param period   realm or season when type is not ALL, otherwise null
     * @param leafStat the unwrapped root stat to look up
     * @return the aggregate value, or {@code null} if never recorded
     */
    @Nullable
    public Long getLeafAggregate(StatFilterType type, @Nullable Period period, IStat leafStat) {
        return switch (type) {
            case ALL -> leafAllMap.get(leafStat);
            case SEASON -> {
                if (!(period instanceof Season season)) yield null;
                ConcurrentMap<IStat, Long> m = leafSeasonMap.get(season);
                yield m == null ? null : m.get(leafStat);
            }
            case REALM -> {
                if (!(period instanceof Realm realm)) yield null;
                ConcurrentMap<IStat, Long> m = leafRealmMap.get(realm);
                yield m == null ? null : m.get(leafStat);
            }
        };
    }

    /**
     * Unwraps a chain of {@link IWrapperStat} layers to find the root (leaf) stat.
     * Non-wrapper stats are returned as-is.
     */
    public static IStat getLeaf(IStat stat) {
        while (stat instanceof IWrapperStat wrapper) {
            stat = wrapper.getWrappedStat();
        }
        return stat;
    }

    public StatConcurrentHashMap copyFrom(StatConcurrentHashMap other) {
        myMap.clear();
        allMap.clear();
        seasonMap.clear();
        leafAllMap.clear();
        leafSeasonMap.clear();
        leafRealmMap.clear();
        myMap.putAll(other.getMyMap());
        // rebuild all secondary indexes
        myMap.forEach((realm, statMap) ->
                statMap.forEach((stat, value) -> {
                    allMap.merge(stat, value, Long::sum);
                    seasonMap.computeIfAbsent(realm.getSeason(), s -> new ConcurrentHashMap<>())
                            .merge(stat, value, Long::sum);
                    IStat leaf = getLeaf(stat);
                    leafAllMap.merge(leaf, value, Long::sum);
                    leafSeasonMap.computeIfAbsent(realm.getSeason(), s -> new ConcurrentHashMap<>())
                            .merge(leaf, value, Long::sum);
                    leafRealmMap.computeIfAbsent(realm, r -> new ConcurrentHashMap<>())
                            .merge(leaf, value, Long::sum);
                })
        );
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
