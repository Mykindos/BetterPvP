package me.mykindos.betterpvp.core.framework.net;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * What instances exist across the network, and who is allowed into them.
 * <p>
 * This is a store rather than a pipe, and that distinction is the whole point. Two parties on different servers can
 * ask for the same instance at the same moment, so deciding whether there is room has to happen in one step that
 * nobody can interleave with. A message bus cannot do that, which is why the directory is separate from it.
 */
public interface SiteDirectory {

    /**
     * Replaces what this server is advertising.
     * <p>
     * Called on a heartbeat rather than on every change, so an entry that stops being refreshed expires by itself.
     * A server that crashes therefore leaves nothing behind for anybody to clean up.
     */
    void publish(@NotNull Collection<RemoteInstance> instances);

    /** Every live instance of a site across the network, this server's included. */
    @NotNull CompletableFuture<List<RemoteInstance>> lookup(@NotNull String siteId, long ownerId);

    /**
     * Takes {@code seats} on an instance, if they are there to take.
     * <p>
     * The check and the taking are one step. A caller that gets {@code true} has the seats and can travel; one that
     * gets {@code false} lost the race and should look elsewhere.
     *
     * @param capacity the ceiling, or zero for no ceiling
     */
    @NotNull CompletableFuture<Boolean> reserve(@NotNull UUID instance, int seats, int capacity);

    /** Gives back seats taken for a journey that did not happen. */
    void release(@NotNull UUID instance, int seats);

    /**
     * The server that holds an owned site, claimed by the first server to ask and unchanged afterwards.
     * <p>
     * An owned world lives on one machine's disk, so its host cannot be picked afresh each time somebody visits.
     *
     * @param candidate the server to claim it for if nobody has yet
     * @return whoever holds it, which is {@code candidate} only if this call is what claimed it
     */
    @NotNull CompletableFuture<String> stickyHost(@NotNull String siteId, long ownerId, @NotNull String candidate);

    /** The servers reporting in, emptiest first, for deciding where a new instance should be made. */
    @NotNull CompletableFuture<List<String>> serversByLoad();

    /** Records that a player is on their way to an instance, so the server they land on knows what to do with them. */
    void expect(@NotNull UUID player, @NotNull RemoteInstance instance);

    /** Takes the arrival a player was expected for, if there is one. Reading it consumes it. */
    @NotNull CompletableFuture<Optional<RemoteInstance>> claimArrival(@NotNull UUID player);

    /** Whether the directory can be reached right now. */
    boolean isAvailable();

    /**
     * Whether other servers can see what this one publishes.
     * <p>
     * A directory only this server reads means this server is the whole network, whatever the reason. Placement asks
     * this rather than asking what is backing the directory, so a network that brings its own answers here.
     */
    boolean isShared();

    /** Releases whatever the directory holds. */
    default void close() {
    }
}
