package me.mykindos.betterpvp.core.framework.customtypes;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class MyConcurrentHashMap<K, V> {

    protected final ConcurrentHashMap<K, V> myMap = new ConcurrentHashMap<>();
    protected final List<IMapListener> listeners = new ArrayList<>();

    public ConcurrentHashMap<K, V> getMap() {
        return myMap;
    }

    public void put(K key, V value) {
        V oldValue = getMap().put(key, value);
        listeners.forEach(l -> l.onMapValueChanged(key.toString(), value, oldValue));
    }

    public void putSilent(K key, V value) {
        getMap().put(key, value);
    }

    public V get(K key) {
        return getMap().get(key);
    }

    public V getOrDefault(K key, V defaultValue) {
        return getMap().getOrDefault(key, defaultValue);
    }

    public void remove(K key) {
        getMap().remove(key);
    }

    public void registerListener(IMapListener listener) {
        listeners.add(listener);
    }

    public void unregisterListener(IMapListener listener) {
        listeners.remove(listener);
    }

}
