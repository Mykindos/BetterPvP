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
import me.mykindos.betterpvp.core.client.stats.impl.IStat;
import me.mykindos.betterpvp.core.redis.Redis;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.manager.PlayerManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Singleton
@CustomLog
public class ClientManager extends PlayerManager<Client> {

    public static final long TIME_TO_LIVE = TimeUnit.MINUTES.toMillis(5);

    /**
     * A thread-safe cache used to store {@link Client} objects associated with their unique {@link UUID}s.
     * <p>
     * The `store` variable acts as the main storage facility for managing active and cached clients
     * in the `ClientManager`. This cache relies on a defined expiration policy to manage client lifecycle,
     **/
    private final Cache<UUID, Client> store; // supposedly thread-safe?

    /**
     * Provides a SQL-based data access layer for managing client-related information.
     * This layer is used for executing SQL queries and interacting with the database to
     * perform operations such as loading, storing, and updating client data.
     * <p>
     * It is a core component of the Client
     */
    @Getter
    private final ClientSQLLayer sqlLayer;

    private final Redis redis;
    private ClientRedisLayer redisLayer;

    public ClientRedisLayer getRedisLayer() {
        if (redisLayer == null && redis.isEnabled()) {
            this.redisLayer = plugin.getInjector().getInstance(ClientRedisLayer.class);
            this.redisLayer.getObserver().register(this::receiveUpdate);
        }
        return redisLayer;
    }

    /**
     *
     */
    @Inject
    public ClientManager(Core plugin, Redis redis, ClientSQLLayer sqlLayer) {
        super(plugin);
        this.sqlLayer = sqlLayer;
        this.redis = redis;
        this.store = Caffeine.newBuilder()
                .scheduler(Scheduler.systemScheduler())
                .expireAfter(new ClientExpiry<>())
                .removalListener((final UUID uuid, final Client client, final RemovalCause cause) -> {
                    Bukkit.getScheduler().runTask(plugin, () -> UtilServer.callEvent(new ClientUnloadEvent(client)));

                    // Only announce the client was unloaded if it expired or was removed forcefully, not replaced.
                    // It won't be removed by size because we didn't set a maximum size.
                    if (client.isLoaded() && (cause == RemovalCause.EXPIRED || cause == RemovalCause.EXPLICIT || cause == RemovalCause.COLLECTED)) {
                        log.info(UNLOAD_ENTITY_FORMAT, client.getName()).submit();
                    }
                })
                .build();
    }

    /**
     * Safely shuts down the associated Redis observer if Redis integration is enabled.
     * <p>
     * This method checks if the Redis integration is active by evaluating the enabled
     * state of the Redis instance. If Redis is enabled, it retrieves the associated observer
     * from the Redis layer and invokes its shutdown process.
     * <p>
     * Use this method to clean up resources and properly terminate the Redis observer
     * when the associated application or manager is shutting down
     */
    public void shutdown() {
        if (this.redis.isEnabled()) {
            this.getRedisLayer().getObserver().shutdown();
        }
    }

    /**
     *
     */
    public void sendMessageToRank(String prefix, Component message, Rank rank) {
        List<Client> clients = this.getOnline().stream().filter(client -> client.hasRank(rank)).toList();
        clients.forEach(client -> {
            final Player player = client.getGamer().getPlayer();
            if (player != null) {
                UtilMessage.message(player, prefix, message);
            }
        });
    }

    public Collection<Player> getPlayersOfRank(Rank rank) {
        return this.getOnline().stream().filter(client -> client.hasRank(rank))
                .map(client -> client.getGamer().getPlayer())
                .collect(Collectors.toSet());
    }

    /**
     * Stores a new client asynchronously, updates its status, and triggers related events.
     * <p>
     * The method ensures that if a client with the same UUID is already loaded,
     * new data is merged into the existing client while retaining its state.
     * The client information is persisted in storage, and relevant events
     * are dispatched to notify other parts of the system about both the pre
     */
    private CompletableFuture<Void> storeNewClient(Client client, final boolean online) {

        return CompletableFuture.runAsync(() -> {
            // If applicable, the client is removed after CLIENT_EXPIRY_TIME milliseconds.
            //
            // If there is another client loaded previously for the same UUID, the new client
            // is voided, but the new data is copied to the existing one. The expiration status
            // will be inherited from the previously loaded client.

            // If we already have a client loaded for this same id, we update it with the new data
            // to not break any references to the older client in the code.
            client.setOnline(online);

            // Adding into storage because no existing client was present.
            UtilServer.callEvent(new AsyncClientPreLoadEvent(client)); // Call event after a client is loaded
            load(client);
            if (this.redis.isEnabled()) {
                this.getRedisLayer().save(client);
            }

            // Executing our success callback
            UtilServer.callEvent(new AsyncClientLoadEvent(client)); // Call event after a client is loaded
            log.info("Loading offline client {} ({})", client.getName(), client.getUniqueId().toString()).submit();

        }).exceptionally(throwable -> {
            log.error("Failed to store new client", throwable).submit();
            return null;
        }); // Block until above operation is complete


    }

    /**
     * Loads the provided client entity into the internal store. The client is identified
     * by its unique ID, which is used as the key to store the entity.
     *
     * @param entity the Client object to be loaded into the store; must contain
     */
    @Override
    protected void load(Client entity) {
        this.store.put(entity.getUniqueId(), entity);
    }

    /**
     * Unloads the given client by saving its state to a Redis layer if Redis is enabled
     * and then invalidating the client's unique ID in the local store cache.
     *
     * @param client the client instance to unload
     */
    @Override
    protected void unload(final Client client) {
        if (this.redis.isEnabled()) {
            this.getRedisLayer().save(client);
        }
        this.store.invalidate(client.getUniqueId());
    }

    /**
     * Loads a Client to be online and stores it if it is not already loaded.
     * After function completes, the client will be stored in the cache.
     * @param uuid the unique identifier of the client to load
     * @param name the name of the client to load
     * @return a CompletableFuture that will complete with an Optional containing the loaded Client, or empty if loading failed
     */
    @Override
    protected CompletableFuture<Optional<Client>> loadOnline(final UUID uuid, final String name) {
        return CompletableFuture.supplyAsync(() -> {
            final Optional<Client> storedUser = this.getStoredExact(uuid);
            if (storedUser.isPresent()) {
                return storedUser;
            }

            Optional<Client> loaded;
            if (this.redis.isEnabled()) {
                loaded = this.getRedisLayer().getAndUpdate(uuid, name).or(() -> this.sqlLayer.getAndUpdate(uuid));
            } else {
                loaded = this.sqlLayer.getAndUpdate(uuid);
            }

            if (loaded.isEmpty()) {
                loaded = Optional.of(this.sqlLayer.create(uuid, name));
            }

            final Client client = loaded.get();
            this.storeNewClient(client, true).join();
            return Optional.of(client);
        });
    }

    /**
     * Loads an offline {@link Client} by first attempting to retrieve it from local storage and then,
     * if not found, attempting to retrieve it using a provided loader. The method applies timeouts to
     * both retrieval processes and logs any exceptions that occur.
     */
    protected Optional<Client> loadOffline(final Supplier<Optional<Client>> searchStorageFilter,
                                           final Supplier<Optional<Client>> loader) {

        try {
            // First check if client is already in the store
            Optional<Client> storedClient = searchStorageFilter.get();

            if (storedClient.isPresent()) {
                return storedClient;
            }

            // If not in store, try loading from database
            Optional<Client> loadedClient = loader.get();

            if (loadedClient.isPresent()) {
                this.storeNewClient(loadedClient.get(), false);
                return loadedClient;
            }

            return Optional.empty();
        } catch (Exception e) {
            log.error("Error in loadOffline", e).submit();
            return Optional.empty();
        }
    }

    /**
     *
     */
    @Override
    protected Optional<Client> loadOffline(@Nullable String name) {
        if (this.redis.isEnabled()) {
            return this.loadOffline(() -> getStoredUser(client -> client.getName().equalsIgnoreCase(name)),
                    () -> this.getRedisLayer().getClient(name).or(() -> this.sqlLayer.getClient(name))
            );
        } else {
            return this.loadOffline(() -> getStoredUser(client -> client.getName().equalsIgnoreCase(name)),
                    () -> this.sqlLayer.getClient(name)
            );
        }
    }

    /**
     * Loads an offline {@link Client} based on the specified UUID by searching in the storage
     * and optionally using Redis and SQL as data sources. This method applies timeouts for data retrieval
     * to ensure responsiveness.
     *
     * @param uuid the unique identifier of the
     */
    @Override
    protected Optional<Client> loadOffline(@Nullable UUID uuid) {
        if (this.redis.isEnabled()) {
            return this.loadOffline(() -> getStoredExact(uuid),
                    () -> this.getRedisLayer().getClient(uuid).or(() -> this.sqlLayer.getClient(uuid))
            );
        } else {
            return this.loadOffline(() -> getStoredExact(uuid),
                    () -> this.sqlLayer.getClient(uuid)
            );
        }
    }

    /**
     *
     */
    protected Optional<Client> getStoredExact(@Nullable UUID uuid) {
        if (uuid == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(this.store.getIfPresent(uuid));
    }

    /**
     * Retrieves a stored {@link Client} from the cache that matches the given predicate.
     * If a matching {@link Client} is found, it is loaded into the storage.
     *
     * @param predicate the condition to match the {@link Client} instances against
     * @return an
     */
    @Override
    protected Optional<Client> getStoredUser(final Predicate<Client> predicate) {
        final Optional<Client> found = this.store.asMap().values().stream().filter(predicate).findFirst();
        found.ifPresent(this::load);
        return found;
    }

    /**
     * Retrieves an immutable set of all currently loaded Client objects.
     *
     * @return a Set containing all loaded Client instances present in the store.
     */
    @Override
    public Set<Client> getLoaded() {
        return Set.copyOf(this.store.asMap().values());
    }

    /**
     * Retrieves a set of currently online clients. A client is considered online
     * if their {@code isLoaded} method returns {@code true}, which indicates that
     * the underlying player is actively online in the system.
     *
     * @return an un
     */
    @Override
    public synchronized Set<Client> getOnline() {
        return this.store.asMap().values().stream().filter(Client::isLoaded).collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Processes statistical updates for both gamer and client data. The updates can be handled
     * either synchronously or asynchronously based on the provided parameter.
     *
     * @param async if true, the updates will be processed asynchronously; otherwise, they will
     *              be processed synchronously.
     */
    @Override
    public void processPropertyUpdates(boolean async) {
        this.sqlLayer.processPropertyUpdates(async);
    }

    public CompletableFuture<Void> processStatUpdates(String period) {
        return this.sqlLayer.processStatUpdates(getLoaded(), period);
    }


    /**
     * Saves a property for the given client with the specified value.
     * This method utilizes the SQL layer to persist the property without
     * executing any asynchronous tasks or direct SQL queries within itself.
     *
     * @param client the client for whom the property is being saved
     * @
     */
    @Override
    public void saveProperty(Client client, String property, Object value) {
        // Does not need to be async as it doesnt actually execute any SQL queries
        this.sqlLayer.saveProperty(client, property, value);
    }

    /**
     * Saves a property associated with a gamer. This method doesn't
     */
    public void saveGamerProperty(Gamer gamer, String property, Object value) {
        // Does not need to be async as it doesnt actually execute any SQL queries
        this.sqlLayer.saveGamerProperty(gamer, property, value);

    }

    /**
     * Saves the provided client instance to persistent storage layers.
     * If the Redis
     */
    @Override
    public void save(Client client) {

        if (this.redis.isEnabled()) {
            this.getRedisLayer().save(client);
        }
        this.sqlLayer.save(client);

    }

    /**
     * Saves the target client to the ignore list of the specified client.
     * If Redis is enabled, updates are saved to Redis. The ignore relationship is
     * also persisted in the SQL database.
     *
     * @param client the client performing the ignore operation
     * @param target the client to be ignored
     */
    public void saveIgnore(Client client, Client target) {
        client.getIgnores().add(target.getUniqueId());

        if (this.redis.isEnabled()) {
            this.getRedisLayer().save(client);
        }
        this.sqlLayer.saveIgnore(client, target);

    }

    /**
     * Removes a target client from the ignore list of the specified client.
     * This operation updates both local and persistent storage layers,
     * including Redis and SQL databases, if applicable.
     *
     * @param client The client whose ignore list will be modified.
     * @
     */
    public void removeIgnore(Client client, Client target) {
        client.getIgnores().remove(target.getUniqueId());

        if (this.redis.isEnabled()) {
            this.getRedisLayer().save(client);
        }

        this.sqlLayer.removeIgnore(client, target);
    }

    /**
     * Retrieves a list of players currently not engaged in combat. The method filters the online clients
     * based on their combat status and returns the corresponding player instances.
     *
     * @return a list of players who are currently out of combat
     */
    public List<Player> getPlayersOutOfCombat() {
        return getOnline().stream()
                .filter(client -> !client.getGamer().isInCombat())
                .map(client -> client.getGamer().getPlayer())
                .toList();
    }

    /**
     *
     */
    public List<Player> getPlayersInCombat() {
        return getOnline().stream()
                .filter(client -> client.getGamer().isInCombat())
                .map(client -> client.getGamer().getPlayer())
                .toList();
    }

    /**
     *
     */
    public boolean isInCombat(Player player) {
        return search().online(player).getGamer().isInCombat();
    }

    /**
     *
     */
    public boolean isMoving(Player player) {
        return search().online(player).getGamer().isMoving();
    }

    /**
     * Shortcut to increment the stat for a player
     * @param player the player
     * @param amount the amount to increment by
     */
    public void incrementStat(Player player, IStat iStat, double amount) {
        search().online(player).getStatContainer().incrementStat(iStat, amount);
    }

    /**
     *
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
        this.getRedisLayer().getClient(uuid).ifPresent(stored::copy);
    }

}