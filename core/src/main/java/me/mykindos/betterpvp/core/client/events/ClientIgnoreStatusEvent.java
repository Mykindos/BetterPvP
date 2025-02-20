package me.mykindos.betterpvp.core.client.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;

@EqualsAndHashCode(callSuper = true)
@Data

public class ClientIgnoreStatusEvent extends CustomEvent {

    private final Client client;
    private final Client target;

    public ClientIgnoreStatusEvent(Client client, Client target){
        super(true);
        this.client = client;
        this.target = target;
    }

    /**
     * Result is DENY if the client is ignoring the target
     */
    private Result result = Result.DEFAULT;
}
