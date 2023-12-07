package me.mykindos.betterpvp.core.client.repository;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.Scheduler;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.events.AsyncClientLoadEvent;
import me.mykindos.betterpvp.core.client.events.AsyncClientPreLoadEvent;
import me.mykindos.betterpvp.core.client.events.ClientUnloadEvent;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.manager.PlayerManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Singleton
@Slf4j
public class ClientManager extends PlayerManager<Client> {

    public static final long TIME_TO_LIVE = TimeUnit.MINUTES.toMillis(5);
    public static final Object CACHE_DUMMY = new Object();

    private final LoadingCache<Client, Object> store; // supposedly thread-safe?
    private final ClientSQLLayer sql;
    private final ClientRedisLayer redis;

    @Inject
    public ClientManager(Core plugin, ClientSQLLayer sql, ClientRedisLayer redis) {
        super(plugin);
        this.sql = sql;
        this.redis = redis;
        this.redis.getObserver().register(this::receiveUpdate);

        this.store = Caffeine.newBuilder()
                .scheduler(Scheduler.systemScheduler())
                .expireAfter(new ClientExpiry())
                .removalListener((final Client unloaded, final Object value, final RemovalCause cause) -> {
                    if (unloaded == null) {
                        return;
                    }

                    Bukkit.getScheduler().runTask(plugin, () -> UtilServer.callEvent(new ClientUnloadEvent(unloaded)));
                    
                    // Only announce the client was unloaded if it expired or was removed forcefully, not replaced.
                    // It won't be removed by size because we didn't set a maximum size.
                    if (cause == RemovalCause.EXPIRED || cause == RemovalCause.EXPLICIT || cause == RemovalCause.COLLECTED) {
                        log.info(UNLOAD_ENTITY_FORMAT, unloaded.getName());
                    }
                })
                .build(key -> CACHE_DUMMY);
    }

    public void sendMessageToRank(String prefix, Component message, Rank rank) {
        List<Client> clients = this.getOnline().stream().filter(client -> client.hasRank(rank)).toList();
        clients.forEach(client -> {
            final Player player = client.getGamer().getPlayer();
            if (player != null) {
                UtilMessage.message(player, prefix, message);
            }
        });
    }

    private void storeNewClient(Client client, final Consumer<Client> then, final boolean online) {
        // If applicable, the client is removed after CLIENT_EXPIRY_TIME milliseconds.
        //
        // If there is another client loaded previously for the same UUID, the new client
        // is voided, but the new data is copied to the existing one. The expiration status
        // will be inherited from the previously loaded client.

        // If we already have a client loaded for this same id, we update it with the new data
        // to not break any references to the older client in the code.
        client.setOnline(online);

        // Adding into storage because no existing client was present.
        Bukkit.getPluginManager().callEvent(new AsyncClientPreLoadEvent(client)); // Call event after a client is loaded
        this.store.get(client);
        this.redis.save(client);

        // Executing our success callback
        Bukkit.getPluginManager().callEvent(new AsyncClientLoadEvent(client)); // Call event after a client is loaded
        if (then != null) {
            then.accept(client);
        }
    }

    @Override
    protected void unload(final Client client) {
        this.redis.save(client);
        this.store.invalidate(client);
    }

    // Override to allow classes in the same package to access
    @Override
    public void loadOnline(UUID playerId, String name, @Nullable Consumer<Client> success, @Nullable Runnable failure) {
        super.loadOnline(playerId, name, success, failure);
    }

    @Override
    protected void loadOnline(final UUID uuid, final String name, final Consumer<Optional<Client>> callback) {
        final Optional<Client> storedUser = this.getStoredUser(stored -> stored.getUniqueId().equals(uuid));
        if (storedUser.isPresent()) {
            callback.accept(storedUser);
            return;
        }

        // Attempting to load a brand-new client
        Optional<Client> loaded = this.redis.getAndUpdate(uuid, name).or(() -> this.sql.getAndUpdate(uuid, name));

        // If the client is empty, that means they don't exist in redis or the database.
        // Attempt to create a new client in the database.
        if (loaded.isEmpty()) {
            loaded = Optional.of(this.sql.create(uuid, name));
        }

        // Attempt to store the client in the cache.
        // New client was found
        this.storeNewClient(loaded.get(), client -> callback.accept(Optional.of(client)), true);
    }

    protected void loadOffline(final Predicate<Client> searchStorageFilter,
                               final Supplier<Optional<Client>> loader,
                               final Consumer<Optional<Client>> callback) {
        // If the client is already loaded, then we will return that instead of loading it again.
        // This is to prevent the client from being loaded every time someone queries it.
        //
        // We don't do the same for loading online clients (like when joining) because we want to
        // make sure that the client is always up-to-date for online people. If an offline client
        // logs on during its expiry time, it'll be overwritten with the new data.
        final Optional<Client> storedUser = this.getStoredUser(searchStorageFilter);
        if (storedUser.isPresent()) {
            callback.accept(storedUser);
            return;
        }

        CompletableFuture.runAsync(() -> {
            final Optional<Client> loaded = loader.get();
            loaded.ifPresent(client -> this.storeNewClient(client, morphed -> callback.accept(Optional.of(morphed)), false));
            if (loaded.isEmpty()) {
                callback.accept(Optional.empty());
            }
        });
    }

    @Override
    protected void loadOffline(String name, Consumer<Optional<Client>> clientConsumer) {
        this.loadOffline(client -> client.getName().equalsIgnoreCase(name),
                () -> this.redis.getClient(name).or(() -> this.sql.getClient(name)),
                clientConsumer
        );
    }

    @Override
    protected void loadOffline(UUID uuid, Consumer<Optional<Client>> clientConsumer) {
        this.loadOffline(client -> client.getUniqueId().equals(uuid),
                () -> this.redis.getClient(uuid).or(() -> this.sql.getClient(uuid)),
                clientConsumer
        );
    }

    @Override
    protected Optional<Client> getStoredUser(final Predicate<Client> predicate) {
        final Optional<Client> found = this.store.asMap().keySet().stream().filter(predicate).findFirst();
        found.ifPresent(this.store::refresh); // Refreshing the client's expiry time
        return found;
    }

    @Override
    public Set<Client> getLoaded() {
        return Collections.unmodifiableSet(this.store.asMap().keySet());
    }

    @Override
    public Set<Client> getOnline() {
        return this.store.asMap().keySet().stream().filter(Client::isLoaded).collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public void processStatUpdates(boolean async) {
        this.sql.processStatUpdates(async);
    }

    @Override
    public void saveProperty(Client client, String property, Object value) {
        CompletableFuture.runAsync(() -> {
            this.redis.save(client);
            this.sql.saveProperty(client, property, value);
        });
    }

    public void saveGamerProperty(Gamer gamer, String property, Object value) {
        CompletableFuture.runAsync(() -> {
            // no need to save to redis because gamers are not persistent across servers
            this.sql.saveGamerProperty(gamer, property, value);
        });
    }

    public void loadGamerProperties(Client client) {
        this.sql.loadGamerProperties(client);
    }

    @Override
    public void save(Client client) {
        CompletableFuture.runAsync(() -> {
            this.redis.save(client);
            this.sql.save(client);
        });
    }

    /**
     * Called whenever this server instance has been notified that a client has been updated
     * elsewhere.
     * For example, when a client is updated on another server, this server will
     * receive a message from redis that the client has been updated.
     * @param uuid The UUID of the client that was updated.
     */
    protected void receiveUpdate(UUID uuid) {
        // Attempt to get a loaded client with the same UUID.
        final Optional<Client> client = this.getStoredUser(stored -> stored.getUniqueId().equals(uuid));
        if (client.isEmpty()) {
            // No client loaded with the same UUID, meaning we don't need to update anything
            // as the updates will be applied when the client is loaded.
            return;
        }

        // Otherwise, update
        final Client stored = client.get();
        this.redis.getClient(uuid).ifPresent(stored::copy);
    }
}

