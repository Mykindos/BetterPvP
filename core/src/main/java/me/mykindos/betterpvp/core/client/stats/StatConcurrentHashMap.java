package me.mykindos.betterpvp.core.client.stats;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import lombok.Data;
import lombok.Getter;
import me.mykindos.betterpvp.core.framework.customtypes.IMapListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class StatConcurrentHashMap implements Iterable<StatConcurrentHashMap.StatData> {
    @Getter
    //period, statname, stat
    protected final ConcurrentHashMap<String, ConcurrentHashMap<String, Double>> myMap = new ConcurrentHashMap<>();
    protected final List<IMapListener> listeners = new ArrayList<>();

    /**
     * Put the specified stat in this map;
     *
     * @param period
     * @param key
     * @param value
     * @param silent
     */
    public void put(String period, String key, Double value, boolean silent) {
        AtomicReference<Number> oldValue = new AtomicReference<>();
        myMap.compute(key, (k, v) -> {
            if (v == null) {
                v = new ConcurrentHashMap<>();
            }
            oldValue.set(v.put(period, value));
            return v;
        });
        if (!silent) {
            listeners.forEach(l -> l.onMapValueChanged(key, value, oldValue.get()));
        }
    }

    public void increase(String key, String period, Double amount) {
        AtomicReference<Double> newValue = new AtomicReference<>();
        myMap.compute(key, (k, v) -> {
            if (v == null) {
                v = new ConcurrentHashMap<>();
            }
            newValue.set(v.compute(period, (sk, sv) -> sv == null ? amount : sv + amount));
            return v;
        });
        Double oldValue = newValue.get() - amount;
        listeners.forEach(l -> l.onMapValueChanged(key, newValue.get(), oldValue));
    }

    @Nullable
    public Double get(String key, String period) {
        if (period == null || period.isEmpty()) return getAll(key);

        final ConcurrentHashMap<String, Double> periodMap = myMap.get(key);
        if (periodMap == null) return null;
        return periodMap.get(period);
    }

    public Double getAll(String key) {
        final ConcurrentHashMap<String, Double> periodMap = myMap.get(key);
        if (periodMap == null) return 0d;
        return periodMap.values().stream().mapToDouble(Number::longValue).sum();
    }

    public void registerListener(IMapListener listener) {
        listeners.add(listener);
    }

    public void unregisterListener(IMapListener listener) {
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
                    final String statname = e.getKey();
                    return new StatData(period, statname, e.getValue());
                });
            }).iterator();
    }

    @Data
    public static class StatData {
        private final String period;
        private final String statName;
        private final Double stat;
    }
}
