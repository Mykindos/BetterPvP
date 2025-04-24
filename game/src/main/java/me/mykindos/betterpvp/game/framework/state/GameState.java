package me.mykindos.betterpvp.game.framework.state;

/**
 * Represents the possible states of a game.
 */
public enum GameState {
    /**
     * Players are in the lobby waiting for enough players to join.
     */
    WAITING,
    
    /**
     * Game is about to begin (countdown phase).
     */
    STARTING,
    
    /**
     * Game is in progress on the map.
     */
    IN_GAME,
    
    /**
     * Game has ended, 10-second window showing results.
     */
    ENDING
}