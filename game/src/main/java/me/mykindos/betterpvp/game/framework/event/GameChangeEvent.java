package me.mykindos.betterpvp.game.framework.event;

import lombok.Getter;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import me.mykindos.betterpvp.game.framework.AbstractGame;

@Getter
public class GameChangeEvent extends CustomEvent {

    private final AbstractGame<?, ?> oldGame;
    private final AbstractGame<?, ?> newGame;

    public GameChangeEvent(AbstractGame<?, ?> oldGame, AbstractGame<?, ?> newGame) {
        this.oldGame = oldGame;
        this.newGame = newGame;
    }
}
