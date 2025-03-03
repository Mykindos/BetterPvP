package me.mykindos.betterpvp.game.framework.state;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Manages game state transitions.
 */
public class GameStateMachine {

    @Getter
    private GameState currentState;
    private final Map<GameState, Set<GameState>> allowedTransitions;
    private final Map<GameState, List<Consumer<GameState>>> exitHandlers;
    private final Map<GameState, List<Consumer<GameState>>> enterHandlers;

    public GameStateMachine() {
        this.currentState = GameState.WAITING;
        this.allowedTransitions = new EnumMap<>(GameState.class);
        this.exitHandlers = new EnumMap<>(GameState.class);
        this.enterHandlers = new EnumMap<>(GameState.class);

        // Define valid transitions
        defineTransitions();
    }

    private void defineTransitions() {
        // From WAITING, can go to STARTING
        addTransition(GameState.WAITING, GameState.STARTING);

        // From STARTING, can go to IN_GAME or back to WAITING (if canceled)
        addTransition(GameState.STARTING, GameState.IN_GAME);
        addTransition(GameState.STARTING, GameState.WAITING);

        // From IN_GAME, can only go to ENDING
        addTransition(GameState.IN_GAME, GameState.ENDING);

        // From ENDING, can go back to WAITING (for next game)
        addTransition(GameState.ENDING, GameState.WAITING);
    }

    public void addTransition(GameState from, GameState to) {
        allowedTransitions.computeIfAbsent(from, k -> EnumSet.noneOf(GameState.class)).add(to);
    }

    /**
     * Adds an exit handler for the specified state
     *
     * @param state The state to add an exit handler for
     * @param handler The handler to execute when exiting the state
     */
    public void addExitHandler(GameState state, Consumer<GameState> handler) {
        exitHandlers.computeIfAbsent(state, k -> new ArrayList<>()).add(handler);
    }

    /**
     * Adds an enter handler for the specified state
     *
     * @param state The state to add an enter handler for
     * @param handler The handler to execute when entering the state
     */
    public void addEnterHandler(GameState state, Consumer<GameState> handler) {
        enterHandlers.computeIfAbsent(state, k -> new ArrayList<>()).add(handler);
    }

    public boolean canTransitionTo(@NotNull GameState state) {
        Set<GameState> allowed = allowedTransitions.get(currentState);
        return allowed != null && allowed.contains(state);
    }

    public boolean transitionTo(@NotNull GameState newState) {
        if (!canTransitionTo(newState)) {
            throw new IllegalStateException("Cannot transition from " + currentState + " to " + newState);
        }

        GameState oldState = currentState;

        // Execute all exit handlers for current state
        List<Consumer<GameState>> exitHandlersForState = exitHandlers.get(oldState);
        if (exitHandlersForState != null) {
            for (Consumer<GameState> handler : exitHandlersForState) {
                handler.accept(newState);
            }
        }

        // Change state
        currentState = newState;

        // Execute all enter handlers for new state
        List<Consumer<GameState>> enterHandlersForState = enterHandlers.get(newState);
        if (enterHandlersForState != null) {
            for (Consumer<GameState> handler : enterHandlersForState) {
                handler.accept(oldState);
            }
        }

        return true;
    }
}