package me.mykindos.betterpvp.core.framework.server.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import me.mykindos.betterpvp.core.framework.server.ServerMessage;

@EqualsAndHashCode(callSuper = true)
@Data
public class ServerMessageReceivedEvent extends CustomEvent {

    private final String channel;
    private final ServerMessage message;

    public ServerMessageReceivedEvent(String channel, ServerMessage message) {
        super(true);
        this.channel = channel;
        this.message = message;
    }
}
