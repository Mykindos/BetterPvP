package me.mykindos.betterpvp.core.utilities.model.manager;

import lombok.CustomLog;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.utilities.model.Unique;
import me.mykindos.betterpvp.core.utilities.search.SearchEngineBase;
import me.mykindos.betterpvp.core.utilities.search.SearchEngineHuman;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Predicate;

@CustomLog
public abstract class PlayerManager<T extends Unique> {

    public static final String LOAD_ENTITY_FORMAT = "Loaded entity for {}";
    public static final String UNLOAD_ENTITY_FORMAT = "Unloaded entity for {}";
    public static final String LOAD_ERROR_FORMAT_ENTITY = "Your entity could not be loaded.";
    public static final String LOAD_ERROR_FORMAT_SERVER = "Could not load entity for: {}";
    public static final String RETRIEVE_ERROR_FORMAT_SERVER = "Could not retrieve entity for: {}";
    
    private final BPvPPlugin plugin;

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

    protected abstract void loadOnline(UUID uuid, String name, Consumer<Optional<T>> callback);

    protected abstract void loadOffline(final String name, final Consumer<Optional<T>> entityConsumer);

    protected abstract void loadOffline(final UUID uuid, final Consumer<Optional<T>> entityConsumer);

    protected abstract Optional<T> getStoredExact(UUID uuid);

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

    public abstract void processStatUpdates(boolean async);

    public abstract void saveProperty(T entity, String property, Object value);

    public abstract void save(T entity);

    /**
     * <b>This methods calls a loadevent upon loading the entity.</b>
     * <p>
     * Asynchronously load a entity and prepare for online behavior.
     *
     * @param playerId The {@link Player}'s UUID.
     * @param name     The {@link Player}'s name.
     * @param success  The success {@link Consumer}
     * @param failure  The failure {@link Runnable}
     */
    protected void loadOnline(final UUID playerId,
                              final String name,
                              @Nullable final Consumer<T> success,
                              @Nullable final Runnable failure) {
        CompletableFuture.runAsync(() -> this.loadOnline(playerId, name, user -> {
            if (user.isEmpty()) {
                log.warn(LOAD_ERROR_FORMAT_SERVER, name);
                if (failure != null) {
                    Bukkit.getScheduler().runTask(this.plugin, failure);
                }

                Optional.ofNullable(Bukkit.getPlayer(playerId))
                        .ifPresent(player -> player.kick(Component.text(LOAD_ERROR_FORMAT_ENTITY)));
            } else {
                final T entity = user.get();
                log.info(LOAD_ENTITY_FORMAT, entity.getUniqueId());
                if (success != null) {
                    Bukkit.getScheduler().runTask(this.plugin, () -> success.accept(entity));
                }
            }
        })).exceptionally(throwable -> {
            log.error(LOAD_ERROR_FORMAT_SERVER, name, throwable);
            if (failure != null) {
                failure.run();
            }
            return null;
        });
    }

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
