package me.mykindos.betterpvp.clans.world.camp;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import me.mykindos.betterpvp.core.world.site.storage.SiteStorages;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reads and writes camp records, through whichever store this server is configured with.
 * <p>
 * A record is read once and held for as long as the clan is around, because the questions asked of it come from the
 * main thread while a world is being opened and cannot wait on a store that may be another machine away. Writing is
 * the other way round and never waited on.
 */
@Singleton
@CustomLog
public class CampStore {

    private final SiteStorages storages;
    private final Map<Long, Camp> records = new ConcurrentHashMap<>();

    @Inject
    public CampStore(@NotNull SiteStorages storages) {
        this.storages = storages;
    }

    /**
     * Reads a clan's camp, or makes an empty one for a clan that has never had a camp before. Reading is what puts it
     * within reach of {@link #cached(long)}, so anything that will need it should ask well before it does.
     */
    public @NotNull CompletableFuture<Camp> load(long clanId) {
        final Camp known = records.get(clanId);
        if (known != null) {
            return CompletableFuture.completedFuture(known);
        }

        return storages.storage().read(Camps.keyFor(clanId), Camp.class)
                .exceptionally(ex -> {
                    // A record that cannot be read is left where it is rather than being replaced with an empty one,
                    // since overwriting it would turn a bad read into a lost camp.
                    log.error("Could not read the camp record for clan {}", clanId, ex).submit();
                    return Optional.empty();
                })
                .thenApply(found -> records.computeIfAbsent(clanId, unused -> found.orElseGet(Camp::new)));
    }

    /** A clan's camp as it was last read, or empty when it has not been read yet. */
    public @NotNull Optional<Camp> cached(long clanId) {
        return Optional.ofNullable(records.get(clanId));
    }

    /** Writes what is held for a clan. Does nothing for one whose record was never read. */
    public void save(long clanId) {
        final Camp camp = records.get(clanId);
        if (camp == null) {
            return;
        }

        storages.storage().write(Camps.keyFor(clanId), camp).exceptionally(ex -> {
            log.error("Could not write the camp record for clan {}", clanId, ex).submit();
            return null;
        });
    }

    /** Throws a camp away for good, for a clan that no longer exists. */
    public void delete(long clanId) {
        records.remove(clanId);
        storages.storage().delete(Camps.keyFor(clanId)).exceptionally(ex -> {
            log.error("Could not delete the camp record for clan {}", clanId, ex).submit();
            return null;
        });
    }

    /** Drops what is held in memory without touching the store. */
    public void forget(long clanId) {
        records.remove(clanId);
    }
}
