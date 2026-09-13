package me.mykindos.betterpvp.core.framework.net;

import lombok.Value;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * One instance somewhere on the network, as the directory knows it.
 * <p>
 * Deliberately not a {@code SiteInstance}: the network layer holds a site id and an owner id rather than the site
 * types, so nothing here has to know what a site is. Whoever asked does the translating.
 */
@Value
public class RemoteInstance {

    @NotNull UUID id;
    @NotNull String siteId;
    long ownerId;

    /** The server holding it, which is the whole reason this type exists. */
    @NotNull String server;

    @NotNull String world;

    /** Whether the instance will take arrivals right now. */
    boolean ready;

    /** Occupants counted network-wide, including seats reserved by parties still in transit. */
    int occupants;

    public boolean isLocal(@NotNull String currentServer) {
        return server.equals(currentServer);
    }
}
