package me.mykindos.betterpvp.core.utilities.model;

/**
 * A marker interface to indicate that this class must be invoked whenever the
 * owning plugin is reloaded.
 */
@FunctionalInterface
public interface Reloadable {

    /**
     * Reloads the configuration or state of this class.
     */
    void reload();

}
