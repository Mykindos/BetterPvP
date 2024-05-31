package me.mykindos.betterpvp.core.client.repository;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.Scheduler;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.events.AsyncClientLoadEvent;
import me.mykindos.betterpvp.core.client.events.AsyncClientPreLoadEvent;
import me.mykindos.betterpvp.core.client.events.ClientUnloadEvent;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.redis.Redis;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.manager.PlayerManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

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
@CustomLog
public class ClientManager extends PlayerManager<Client> {

    public static final long TIME_TO_LIVE = TimeUnit.MINUTES.toMillis(5);

    private final Cache<UUID, Client> store; // supposedly thread-safe?

    @Getter
    private final ClientSQLLayer sqlLayer;

    private final ClientRedisLayer redisLayer;
    private final Redis redis;

    @Inject
    public ClientManager(Core plugin, Redis redis) {
        super(plugin);
        this.sqlLayer = plugin.getInjector().getInstance(ClientSQLLayer.class);
        this.redis = redis;
        if (redis.isEnabled()) {
            this.redisLayer = plugin.getInjector().getInstance(ClientRedisLayer.class);
            this.redisLayer.getObserver().register(this::receiveUpdate);
        } else {
            this.redisLayer = null;
        }

        this.store = Caffeine.newBuilder()
                .scheduler(Scheduler.systemScheduler())
                .expireAfter(new ClientExpiry<>())
                .removalListener((final UUID uuid, final Client client, final RemovalCause cause) -> {
                    if (uuid == null || client == null) {
                        return;
                    }

                    Bukkit.getScheduler().runTask(plugin, () -> UtilServer.callEvent(new ClientUnloadEvent(client)));

                    // Only announce the client was unloaded if it expired or was removed forcefully, not replaced.
                    // It won't be removed by size because we didn't set a maximum size.
                    if (client.isLoaded() && (cause == RemovalCause.EXPIRED || cause == RemovalCause.EXPLICIT || cause == RemovalCause.COLLECTED)) {
                        log.info(UNLOAD_ENTITY_FORMAT, client.getName()).submit();
                    }
                })
                .build();
    }

    public void shutdown() {
        if (this.redis.isEnabled()) {
            this.redisLayer.getObserver().shutdown();
        }
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
        load(client);
        if (this.redis.isEnabled()) {
            this.redisLayer.save(client);
        }

        // Executing our success callback
        Bukkit.getPluginManager().callEvent(new AsyncClientLoadEvent(client)); // Call event after a client is loaded
        if (then != null) {
            then.accept(client);
        }
    }

    @Override
    protected void load(Client entity) {
        this.store.put(entity.getUniqueId(), entity);
    }

    @Override
    protected void unload(final Client client) {
        if (this.redis.isEnabled()) {
            this.redisLayer.save(client);
        }
        this.store.invalidate(client.getUniqueId());
    }

    // Override to allow classes in the same package to access
    @Override
    public void loadOnline(UUID playerId, String name, @Nullable Consumer<Client> success, @Nullable Runnable failure) {
        super.loadOnline(playerId, name, success, failure);
    }

    @Override
    protected void loadOnline(final UUID uuid, final String name, final Consumer<Optional<Client>> callback) {
        final Optional<Client> storedUser = this.getStoredExact(uuid);
        if (storedUser.isPresent()) {
            callback.accept(storedUser);
            return;
        }

        // Attempting to load a brand-new client
        Optional<Client> loaded;
        if (this.redis.isEnabled()) {
            loaded = this.redisLayer.getAndUpdate(uuid, name).or(() -> this.sqlLayer.getAndUpdate(uuid, name));
        } else {
            loaded = this.sqlLayer.getAndUpdate(uuid, name);
        }

        // If the client is empty, that means they don't exist in redis or the database.
        // Attempt to create a new client in the database.
        if (loaded.isEmpty()) {
            loaded = Optional.of(this.sqlLayer.create(uuid, name));
        }

        // Attempt to store the client in the cache.
        // New client was found
        this.storeNewClient(loaded.get(), client -> callback.accept(Optional.of(client)), true);
    }

    protected void loadOffline(final Supplier<Optional<Client>> searchStorageFilter,
                               final Supplier<Optional<Client>> loader,
                               final Consumer<Optional<Client>> callback) {
        // If the client is already loaded, then we will return that instead of loading it again.
        // This is to prevent the client from being loaded every time someone queries it.
        //
        // We don't do the same for loading online clients (like when joining) because we want to
        // make sure that the client is always up-to-date for online people. If an offline client
        // logs on during its expiry time, it'll be overwritten with the new data.
        final Optional<Client> storedUser = searchStorageFilter.get();
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
        if (this.redis.isEnabled()) {
            this.loadOffline(() -> getStoredUser(client -> client.getName().equalsIgnoreCase(name)),
                    () -> this.redisLayer.getClient(name).or(() -> this.sqlLayer.getClient(name)),
                    clientConsumer
            );
        } else {
            this.loadOffline(() -> getStoredUser(client -> client.getName().equalsIgnoreCase(name)),
                    () -> this.sqlLayer.getClient(name),
                    clientConsumer
            );
        }
    }

    @Override
    protected void loadOffline(UUID uuid, Consumer<Optional<Client>> clientConsumer) {
        if (this.redis.isEnabled()) {
            this.loadOffline(() -> getStoredExact(uuid),
                    () -> this.redisLayer.getClient(uuid).or(() -> this.sqlLayer.getClient(uuid)),
                    clientConsumer
            );
        } else {
            this.loadOffline(() -> getStoredExact(uuid),
                    () -> this.sqlLayer.getClient(uuid),
                    clientConsumer
            );
        }
    }

    protected Optional<Client> getStoredExact(UUID uuid) {
        return Optional.ofNullable(this.store.getIfPresent(uuid));
    }

    @Override
    protected Optional<Client> getStoredUser(final Predicate<Client> predicate) {
        final Optional<Client> found = this.store.asMap().values().stream().filter(predicate).findFirst();
        found.ifPresent(this::load);
        return found;
    }

    @Override
    public Set<Client> getLoaded() {
        return Set.copyOf(this.store.asMap().values());
    }

    @Override
    public Set<Client> getOnline() {
        return this.store.asMap().values().stream().filter(Client::isLoaded).collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public void processStatUpdates(boolean async) {
        this.sqlLayer.processStatUpdates(async);
    }

    @Override
    public void saveProperty(Client client, String property, Object value) {
        CompletableFuture.runAsync(() -> {
            if (this.redis.isEnabled()) {
                this.redisLayer.save(client);
            }
            this.sqlLayer.saveProperty(client, property, value);
        });
    }

    public void saveGamerProperty(Gamer gamer, String property, Object value) {
        CompletableFuture.runAsync(() -> {
            // no need to save to redis because gamers are not persistent across servers
            this.sqlLayer.saveGamerProperty(gamer, property, value);
        });
    }

    public void loadGamerProperties(Client client) {
        this.sqlLayer.loadGamerProperties(client);
    }

    @Override
    public void save(Client client) {
        CompletableFuture.runAsync(() -> {
            if (this.redis.isEnabled()) {
                this.redisLayer.save(client);
            }
            this.sqlLayer.save(client);
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
        if (!this.redis.isEnabled()) {
            throw new IllegalStateException("Redis is not enabled.");
        }

        // Attempt to get a loaded client with the same UUID.
        final Optional<Client> client = this.getStoredExact(uuid);
        if (client.isEmpty()) {
            // No client loaded with the same UUID, meaning we don't need to update anything
            // as the updates will be applied when the client is loaded.
            return;
        }

        // Otherwise, update
        final Client stored = client.get();
        this.redisLayer.getClient(uuid).ifPresent(stored::copy);
    }

}

