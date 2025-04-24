package me.mykindos.betterpvp.game.framework.event;

import lombok.Getter;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import me.mykindos.betterpvp.game.framework.state.GameState;

@Getter
public class PreGameStateChangeEvent extends CustomEvent {

    private final GameState oldState;
    private final GameState newState;

    public PreGameStateChangeEvent(GameState oldState, GameState newState) {
        this.oldState = oldState;
        this.newState = newState;
    }
}
