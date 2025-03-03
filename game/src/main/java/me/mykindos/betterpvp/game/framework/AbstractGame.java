package me.mykindos.betterpvp.game.framework;

import me.mykindos.betterpvp.game.framework.configuration.GenericGameConfiguration;
import com.google.inject.Injector;

/**
 * Represents a game on the network.
 */
public sealed class AbstractGame<C extends GenericGameConfiguration> permits TeamGame {

    private final C configuration;
    protected Injector injector;

    protected AbstractGame(C configuration) {
        this.configuration = configuration;
    }

    /**
     * @return The configuration of this game.
     */
    public C getConfiguration() {
        return configuration;
    }

    /**
     * Sets the injector for this game
     * @param injector The injector to use
     */
    void bindInjector(Injector injector) {
        this.injector = injector;
    }
}