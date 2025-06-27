package me.mykindos.betterpvp.core.utilities.model.manager;

import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.utilities.model.Unique;
import me.mykindos.betterpvp.core.utilities.search.SearchEngineBase;
import me.mykindos.betterpvp.core.utilities.search.SearchEngineHuman;
import org.bukkit.command.CommandSender;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

@CustomLog
public abstract class PlayerManager<T extends Unique> {

    public static final String LOAD_ENTITY_FORMAT = "Loaded entity for {}";
    public static final String UNLOAD_ENTITY_FORMAT = "Unloaded entity for {}";
    public static final String LOAD_ERROR_FORMAT_ENTITY = "Your entity could not be loaded.";
    public static final String LOAD_ERROR_FORMAT_SERVER = "Could not load entity for: {}";
    public static final String RETRIEVE_ERROR_FORMAT_SERVER = "Could not retrieve entity for: {}";
    
    protected final BPvPPlugin plugin;

    protected PlayerManager(BPvPPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Unload an entity from storage.
     * This won't remove the entity from redis or sql, it will just remove it from the cache.
     * @param entity The entity to unload.
     */
    protected abstract void unload(T entity);

    /**
     * Load an entity into storage.
     * @param entity The entity to load.
     */
    protected abstract void load(T entity);

    protected abstract CompletableFuture<Optional<Client>> loadOnline(UUID uuid, String name);

    protected abstract Optional<T> loadOffline(@Nullable final String name);

    protected abstract Optional<T> loadOffline(@Nullable final UUID uuid);

    protected abstract Optional<T> getStoredExact(@Nullable UUID uuid);

    protected abstract Optional<T> getStoredUser(Predicate<T> predicate);

    /**
     * Get all loaded entities. Not to be confused with {@link #getOnline()}, as this returns
     * a complete list of all loaded entities in storage.
     *
     * @return A list of all loaded entities.
     */
    public abstract Set<T> getLoaded();

    /**
     * Get all online entities. Not to be confused with {@link #getLoaded()}, as this only
     * returns a sublist of all online entities within the loaded storage.
     *
     * @return A list of all online entities.
     */
    public abstract Set<T> getOnline();

    public abstract void processPropertyUpdates(boolean async);

    public abstract void saveProperty(T entity, String property, Object value);

    public abstract void save(T entity);


    public SearchEngineHuman<T> search(final CommandSender human) {
        return new SearchEngineHuman<>(human,
                this::getStoredExact,
                this::loadOffline,
                this::loadOffline
        );
    }

    public SearchEngineBase<T> search() {
        return new SearchEngineBase<>(
                this::getStoredExact,
                this::loadOffline,
                this::loadOffline
        );
    }
}
