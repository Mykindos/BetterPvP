package me.mykindos.betterpvp.core.client.events;

import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;

/**
 * Called when a client is loaded from any source.
 * This client may or may not be online. Check {@link #isOnline()}.
 */
public class AsyncClientLoadEvent extends CustomEvent {

    private final Client client;

    public AsyncClientLoadEvent(final Client client) {
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
