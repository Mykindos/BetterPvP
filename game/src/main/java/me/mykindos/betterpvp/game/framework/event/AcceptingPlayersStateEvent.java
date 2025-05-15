package me.mykindos.betterpvp.game.framework.event;

import lombok.Getter;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;

@Getter
public class AcceptingPlayersStateEvent extends CustomEvent {

    private final boolean acceptingPlayers;

    public AcceptingPlayersStateEvent(boolean acceptingPlayers) {
        this.acceptingPlayers = acceptingPlayers;
    }
}
