package me.mykindos.betterpvp.game.guice;

import com.google.inject.AbstractModule;
import org.bukkit.event.Listener;

import java.util.HashSet;
import java.util.Set;

/**
 * Base implementation of a game module.
 */
public abstract class AbstractGameModule extends AbstractModule implements GameModule {

    private final String id;
    private final Set<Class<? extends Listener>> listeners = new HashSet<>();

    protected AbstractGameModule(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Set<Class<? extends Listener>> getListeners() {
        return listeners;
    }

    @Override
    public void onEnable() {
        // Default implementation does nothing
    }

    @Override
    public void onDisable() {
        // Default implementation does nothing
    }

    /**
     * Registers a listener class with this module
     * @param listenerClass The listener class to register
     */
    protected void registerListener(Class<? extends Listener> listenerClass) {
        listeners.add(listenerClass);
    }

    /**
     * Configures a basic game module.
     * Override to add custom bindings.
     */
    @Override
    protected void configure() {
        // Bind game-scoped components here
    }
}