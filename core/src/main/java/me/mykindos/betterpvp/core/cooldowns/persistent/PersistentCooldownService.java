package me.mykindos.betterpvp.core.cooldowns.persistent;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Cooldowns that survive a reconnect, a restart, and a move to another server.
 * <p>
 * Distinct from {@link me.mykindos.betterpvp.core.cooldowns.CooldownManager CooldownManager}, and not a replacement for
 * it. That one is in-memory and per tick, which is exactly right for an ability: it is cheap, it drives the action bar,
 * and a cooldown that clears on reconnect costs nothing there. This one is for the cooldowns a player would gain
 * something by clearing — the ones metering out an item, a reward, a limited action — where "log out and back in" must
 * not be a way around the wait.
 * <p>
 * State lives in the database rather than in memory or in a player's property bag, because both are per server: if
 * spawn is ever sharded, a limit one shard is holding is no limit at all to someone who hops to another. One shared
 * table is the only place the answer can be the same everywhere.
 * <p>
 * Everything here is asynchronous, and deliberately not cached. A cache is precisely what would reintroduce the
 * double-dip — a shard holding a stale "not on cooldown" across another shard's grant. These are read when a player
 * interacts with something, not on a hot path, so the read can simply be made.
 *
 * <h3>Working with a synchronous caller</h3>
 * Gates like a conversation's response predicate are synchronous and cannot wait on a query. Resolve the cooldown once
 * at the point of interaction, then build the gate around the answer:
 * <pre>{@code
 * cooldowns.remaining(player, PICKAXE).thenAccept(remaining -> UtilServer.runTask(plugin, () -> {
 *     openDialogue(player, remaining.isZero());
 * }));
 * }</pre>
 */
@Singleton
@BPvPListener
public class PersistentCooldownService implements Listener {

    private final PersistentCooldownRepository repository;
    private final ClientManager clientManager;

    @Inject
    public PersistentCooldownService(@NotNull PersistentCooldownRepository repository,
                                     @NotNull ClientManager clientManager) {
        this.repository = repository;
        this.clientManager = clientManager;
    }

    /**
     * How much longer this cooldown has to run.
     *
     * @return the remaining time, or {@link Duration#ZERO} when the player is free to act
     */
    public CompletableFuture<Duration> remaining(@NotNull Player player, @NotNull String key) {
        final long now = System.currentTimeMillis();
        return repository.expiresAt(clientId(player), key)
                .thenApply(expiresAt -> Duration.ofMillis(Math.max(0L, expiresAt - now)));
    }

    /** Convenience over {@link #remaining} for a caller that only needs the yes or no. */
    public CompletableFuture<Boolean> isOnCooldown(@NotNull Player player, @NotNull String key) {
        return remaining(player, key).thenApply(remaining -> !remaining.isZero());
    }

    /** Starts (or restarts) the cooldown, running from now. */
    public CompletableFuture<Void> start(@NotNull Player player, @NotNull String key, @NotNull Duration duration) {
        return repository.set(clientId(player), key, System.currentTimeMillis() + duration.toMillis());
    }

    /** Ends the cooldown early, so the player may act again immediately. */
    public CompletableFuture<Void> clear(@NotNull Player player, @NotNull String key) {
        return repository.clear(clientId(player), key);
    }

    /**
     * Housekeeping. An expired row already reads as "not on cooldown", so this only keeps the table from accumulating
     * rows nobody will ever look at again.
     */
    @UpdateEvent(delay = 60 * 60 * 1000)
    public void pruneExpired() {
        repository.pruneExpired(System.currentTimeMillis());
    }

    private long clientId(@NotNull Player player) {
        return clientManager.search().online(player).getId();
    }
}
