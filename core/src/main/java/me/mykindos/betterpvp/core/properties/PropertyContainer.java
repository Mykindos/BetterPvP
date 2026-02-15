package me.mykindos.betterpvp.core.properties;

import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.core.framework.customtypes.MyConcurrentHashMap;

import java.util.Optional;

/**
 * Simple container for properties.
 * Properties are stored in a map, with the key being the property name (String) and the value being the property value (Object).
 */
@CustomLog
public abstract class PropertyContainer {


    @Getter
    protected final MyConcurrentHashMap<String, Object> properties = new MyConcurrentHashMap<>();

    public static long forceNumber(Object value) {
        if (value instanceof Boolean bool) {
            return Boolean.TRUE.equals(bool) ? 1L : 0L;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String string) {
            return Long.parseLong(string);
        }

        return (long) value;
    }

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

    public void incrementProperty(Enum<?> key, Number amount) {
        incrementProperty(key.name(), amount);
    }

    public void incrementProperty(String key, Number amount) {
        try {
            Number value = (Number) getProperty(key).orElse(0L);
            putProperty(key, value.longValue() + amount.longValue());
        } catch (ClassCastException e) {
            log.error("Cannot increment a non-number property ", e).submit();
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

    public int getIntProperty(Enum<?> key) {
        return (int) getProperty(key).orElse(0);
    }

    public long getLongProperty(Enum<?> key) {
        return (long) getProperty(key).orElse(0L);
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

    public long forceNumber(Enum<?> key) {
        return forceNumber(key.name());
    }

    /**
     * Forces the key to return a number
     * @param key
     * @return
     */
    public long forceNumber(String key) {
        Object value = getProperty(key).orElse(0L);
        return forceNumber(value);
    }
}
