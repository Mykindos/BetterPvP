package me.mykindos.betterpvp.core.framework.net;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * Moves a player from this server to another one.
 * <p>
 * Separate from {@link MessageBus} because a network can carry messages one way and move players another. A proxy
 * network moves them by asking the proxy, and something else may hand them a new address or route them through a
 * queue, none of which the caller should have to know.
 */
public interface PlayerTransfer {

    /**
     * Sends one player to {@code server}.
     * <p>
     * Completing does not mean they arrived. It means the request was made, since a player leaving this server stops
     * being observable from it, and nothing here can wait for a landing it will never see.
     */
    @NotNull CompletableFuture<Boolean> transfer(@NotNull Player traveller, @NotNull String server);

    /** Sends several, which for most networks is the same request repeated. */
    default @NotNull CompletableFuture<Boolean> transferAll(@NotNull Collection<Player> travellers,
                                                            @NotNull String server) {
        final CompletableFuture<?>[] sent = travellers.stream()
                .map(traveller -> transfer(traveller, server))
                .toArray(CompletableFuture[]::new);

        return CompletableFuture.allOf(sent).thenApply(ignored -> !travellers.isEmpty());
    }

    /** Whether a transfer requested right now would go anywhere. */
    boolean isAvailable();
}
