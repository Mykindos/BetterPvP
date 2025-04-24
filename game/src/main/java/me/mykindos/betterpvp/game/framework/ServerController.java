package me.mykindos.betterpvp.game.framework;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.game.framework.event.*;
import me.mykindos.betterpvp.game.framework.state.GameState;
import me.mykindos.betterpvp.game.framework.state.GameStateMachine;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

/**
 * Controls the entirety of this server instance's state.
 * <p>
 * This class is responsible for managing the game state, player state, and other server-wide
 * state.
 */
@Singleton
@CustomLog
public final class ServerController implements Listener {

    @Getter
    private final GameStateMachine stateMachine;
    @Getter
    private AbstractGame<?, ?> currentGame;
    @Getter
    @Setter
    private boolean acceptingPlayers = false;

    @Inject
    public ServerController() {
        this.stateMachine = new GameStateMachine();
    }

    public void setAcceptingPlayers(boolean acceptingPlayers) {
        boolean changed = this.acceptingPlayers != acceptingPlayers;
        this.acceptingPlayers = acceptingPlayers;

        if (changed) {
            new AcceptingPlayersStateEvent(acceptingPlayers).callEvent();
        }
    }

    /**
     * Gets the current game state
     * @return the current game state
     */
    public GameState getCurrentState() {
        return stateMachine.getCurrentState();
    }

    /**
     * Sets the current game. Can only be done when in WAITING state.
     *
     * @param game the game to set as current
     * @return true if the game was set successfully, false otherwise
     * @throws IllegalStateException if not in WAITING state
     */
    public boolean setGame(AbstractGame<?, ?> game) {
        if (getCurrentState() != GameState.WAITING) {
            throw new IllegalStateException("Cannot change game while not in WAITING state");
        }

        final PreGameChangeEvent preEvent = new PreGameChangeEvent(currentGame, game);
        preEvent.callEvent();
        if (preEvent.isCancelled()) {
            return false;
        }
        final GameChangeEvent event = new GameChangeEvent(currentGame, game);
        this.currentGame = game;
        event.callEvent();
        return true;
    }

    /**
     * Attempts to transition to the given state
     *
     * @param state the state to transition to
     * @return true if the transition was successful
     * @throws IllegalStateException if trying to leave WAITING state with no game assigned
     */
    public boolean transitionTo(@NotNull GameState state) {
        // Check if trying to leave WAITING state with no game assigned
        if (getCurrentState() == GameState.WAITING && state != GameState.WAITING && currentGame == null) {
            throw new IllegalStateException("Cannot transition from WAITING state when no game is assigned");
        }

        return stateMachine.transitionTo(state);
    }

    /**
     * Checks if the server can transition to the given state
     *
     * @param state the state to check
     * @return true if can transition to the state
     */
    public boolean canTransitionTo(@NotNull GameState state) {
        // Cannot leave WAITING state with no game assigned
        if (getCurrentState() == GameState.WAITING && state != GameState.WAITING && currentGame == null) {
            return false;
        }

        return stateMachine.canTransitionTo(state);
    }

    /**
     * Clears the current game. Can only be done when in WAITING state.
     *
     * @return true if the game was cleared successfully, false otherwise
     * @throws IllegalStateException if not in WAITING state
     */
    public boolean clearGame() {
        if (getCurrentState() != GameState.WAITING) {
            throw new IllegalStateException("Cannot clear game while not in WAITING state");
        }

        this.currentGame = null;
        return true;
    }

}