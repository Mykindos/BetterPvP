package me.mykindos.betterpvp.core.framework.mineplex.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import me.mykindos.betterpvp.core.framework.mineplex.MineplexMessage;

@EqualsAndHashCode(callSuper = true)
@Data
public class MineplexMessageSentEvent extends CustomEvent {

    private final String channel;
    private final MineplexMessage message;

    public MineplexMessageSentEvent(String channel, MineplexMessage message) {
        super(true);
        this.channel = channel;
        this.message = message;
    }
}
