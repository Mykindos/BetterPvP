package me.mykindos.betterpvp.core.client.events;

import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;

public class ClientUnloadEvent extends CustomEvent {

    private final Client client;

    public ClientUnloadEvent(final Client client) {
        this.client = client;
    }

    public Client getClient() {
        return this.client;
    }

}
