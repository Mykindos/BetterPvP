package me.mykindos.betterpvp.core.properties;

import lombok.Getter;
import me.mykindos.betterpvp.core.framework.customtypes.MyConcurrentHashMap;

import java.util.Optional;

/**
 * Simple container for properties.
 * Properties are stored in a map, with the key being the property name (String) and the value being the property value (Object).
 */
public abstract class PropertyContainer {

    @Getter
    protected final MyConcurrentHashMap<String, Object> properties = new MyConcurrentHashMap<>();

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
        putProperty(key, object, false);
    }

    /**
     * Put a property.
     *
     * @param key    The key of the property.
     * @param object The value of the property.
     * @param silent Whether to update the property in the database
     */
    public void putProperty(String key, Object object, boolean silent) {
        if (!silent) {
            properties.put(key, object);
        } else {
            properties.putSilent(key, object);
        }
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
    public abstract void saveProperty(String key, Object object);

    /**
     * Delete a property from the property map
     * @param key The key of the property to remove
     */
    public void removeProperty(String key) {
        properties.remove(key);
    }
}
