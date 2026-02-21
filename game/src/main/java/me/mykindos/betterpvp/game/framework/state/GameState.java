package me.mykindos.betterpvp.game.framework.state;

import lombok.Getter;

/**
 * Represents the possible states of a game.
 */
@Getter
public enum GameState {
    /**
     * Players are in the lobby waiting for enough players to join.
     */
    WAITING(true),
    
    /**
     * Game is about to begin (countdown phase).
     */
    STARTING(true),
    
    /**
     * Game is in progress on the map.
     */
    IN_GAME(false),
    
    /**
     * Game has ended, 10-second window showing results.
     */
    ENDING(false);

    /**
     * {@code true} if in lobby, {@code false} if in a game
     */
    private final boolean inLobby;

    GameState(boolean inLobby) {
        this.inLobby = inLobby;
    }
}