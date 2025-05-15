package me.mykindos.betterpvp.game.framework;

import com.google.inject.Injector;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.game.framework.configuration.GenericGameConfiguration;
import me.mykindos.betterpvp.game.framework.model.Lifecycled;
import me.mykindos.betterpvp.game.framework.model.attribute.BoundAttribute;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Represents a game on the network.
 *
 * @param <C> The configuration of this game.
 * @param <W> The winner type of this game.
 */
public abstract sealed class AbstractGame<C extends GenericGameConfiguration, W extends Audience> implements Lifecycled permits TeamGame {


    @Getter
    private final C configuration;

    protected Injector injector;

    @Setter @Getter
    private List<W> winners = new ArrayList<>();

    protected AbstractGame(C configuration) {
        this.configuration = configuration;
    }

    /**
     * Sets the injector for this game
     * @param injector The injector to use
     */
    void bindInjector(Injector injector) {
        this.injector = injector;
    }

    /**
     * @return The description of this game.
     */
    public abstract Component getDescription();

    @Override
    public void setup() {
        // Default implementation
    }

    @Override
    public void tearDown() {
        // Default implementation
    }

    public <T extends BoundAttribute<?>> T getAttribute(Class<T> attributeClass) {
        return injector.getInstance(attributeClass);
    }

    /**
     * Checks if the game has a winner based on current conditions.
     * Each game implementation should override this to determine its own win conditions.
     * <p>
     * This method is called every time an important event occurs in the game, like a player
     * leaving or dying.
     *
     * @return true if win conditions met and the game should end, false otherwise
     */
    public abstract boolean attemptGracefulEnding();

    /**
     * Forcefully declares the given winners.
     */
    public abstract void forceEnd();

    /**
     * Retrieves all participants in the game.
     *
     * @return A set containing all participants of type {@code W}.
     */
    public abstract Set<W> getParticipants();

    /**
     * @return The description of the winner.
     */
    public abstract Component getWinnerDescription();

}