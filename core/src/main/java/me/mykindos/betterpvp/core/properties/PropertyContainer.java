package me.mykindos.betterpvp.core.properties;

import lombok.Getter;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple container for properties.
 * Properties are stored in a map, with the key being the property name (String) and the value being the property value (Object).
 */
public abstract class PropertyContainer {

    @Getter
    protected final Map<String, Object> properties = new ConcurrentHashMap<>();

    /**
     * Get a property by enum key.
     *
     * @param key The key of the property.
     * @param <T> The type of the property.
     * @return Optional.empty() if the property is not found.
     */
    public <T> Optional<T> getProperty(Enum<?> key) {
        return getProperty(key.name());
    }

    /**
     * Get a property by string key.
     *
     * @param key The key of the property.
     * @param <T> The type of the property.
     * @return Optional.empty() if the property is not found.
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getProperty(String key) {
        return Optional.ofNullable((T) properties.getOrDefault(key, null));
    }

    /**
     * Put a property.
     *
     * @param key    The key of the property.
     * @param object The value of the property.
     */
    public void putProperty(Enum<?> key, Object object) {
        putProperty(key.name(), object);
    }

    /**
     * Put a property.
     *
     * @param key    The key of the property.
     * @param object The value of the property.
     */
    public void putProperty(String key, Object object) {
        properties.put(key, object);
    }

    /**
     * Save a property.
     *
     * @param key    The key of the property.
     * @param object The value of the property.
     */
    public void saveProperty(Enum<?> key, Object object) {
        saveProperty(key.name(), object);
    }

    /**
     * Save a property.
     *
     * @param key    The key of the property.
     * @param object The value of the property.
     */
    public void saveProperty(String key, Object object) {
        saveProperty(key, object, false);
    }

    /**
     * Save a property.
     *
     * @param key              The key of the property.
     * @param object           The value of the property.
     * @param updateScoreboard Whether to update the property holders scoreboard.
     */
    public abstract void saveProperty(String key, Object object, boolean updateScoreboard);
}
