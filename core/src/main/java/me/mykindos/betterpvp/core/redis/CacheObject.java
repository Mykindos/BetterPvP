package me.mykindos.betterpvp.core.redis;

/**
 * Represents an object that's able to be cached in {@link Redis}
 */
public interface CacheObject {

    /**
     * @return The key of the object
     */
    String getKey();

}
