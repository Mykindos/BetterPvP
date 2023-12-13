package me.mykindos.betterpvp.core.client.events;

import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;

/**
 * Called before a client is loaded from any source.
 * This client may or may not be online. Check {@link #isOnline()}.
 */
public class AsyncClientPreLoadEvent extends CustomEvent {

    private final Client client;

    public AsyncClientPreLoadEvent(final Client client) {
        super(true);
        this.client = client;
    }

    /**
     * See if the client is online.
     *
     * @return true if the client is online.
     * @see Client#isLoaded()
     */
    public boolean isOnline() {
        return this.client.isOnline();
    }

    public Client getClient() {
        return this.client;
    }
}
