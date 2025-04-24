package me.mykindos.betterpvp.game.framework.event;

import lombok.Getter;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;
import me.mykindos.betterpvp.game.framework.AbstractGame;

@Getter
public class PreGameChangeEvent extends CustomCancellableEvent {

    private final AbstractGame<?, ?> oldGame;
    private final AbstractGame<?, ?> newGame;

    public PreGameChangeEvent(AbstractGame<?, ?> oldGame, AbstractGame<?, ?> newGame) {
        this.oldGame = oldGame;
        this.newGame = newGame;
    }
}
