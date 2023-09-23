package me.mykindos.betterpvp.core.framework.customtypes;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class MyConcurrentHashMap<K, V> {

    private final ConcurrentHashMap<K, V> myMap = new ConcurrentHashMap<>();
    private final List<IMapListener> listeners = new ArrayList<>();

    public ConcurrentHashMap<K, V> getMap() {
        return myMap;
    }

    public void put(K key, V value) {
        myMap.put(key, value);
        listeners.forEach(l -> l.onMapValueChanged(key.toString(), value));
    }

    public void putSilent(K key, V value) {
        myMap.put(key, value);
    }

    public V get(K key) {
        return myMap.get(key);
    }

    public V getOrDefault(K key, V defaultValue) {
        return myMap.getOrDefault(key, defaultValue);
    }

    public void registerListener(IMapListener listener) {
        listeners.add(listener);
    }

    public void unregisterListener(IMapListener listener) {
        listeners.remove(listener);
    }

}
