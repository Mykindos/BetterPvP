package me.mykindos.betterpvp.core.cutscene.skip;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.database.Database;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jooq.Record1;
import org.jooq.Result;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static org.jooq.impl.DSL.*;

/**
 * Which cutscenes each online player has already finished.
 * <p>
 * Loaded once per session on join and answered from memory afterwards, because {@link SkipPolicy#seenBefore} is
 * resolved at the instant a cutscene starts and must not block the main thread to decide whether a skip prompt is
 * drawn. Writes are fire-and-forget for the same reason - a lost view record costs one extra mandatory viewing, which
 * is the harmless direction to fail in.
 */
@Singleton
@CustomLog
public class CutsceneViewStore {

    private static final String TABLE = "cutscene_views";

    private final Database database;
    private final Map<UUID, Set<String>> seen = new ConcurrentHashMap<>();

    @Inject
    public CutsceneViewStore(Database database) {
        this.database = database;
    }

    /** Warms the cache for a player who just joined. */
    public CompletableFuture<Void> load(@NotNull UUID viewer) {
        return database.getAsyncDslContext().executeAsyncVoid(context -> {
            final Result<Record1<String>> rows = context
                    .select(field(name("cutscene"), String.class))
                    .from(table(name(TABLE)))
                    .where(field(name("viewer"), UUID.class).eq(viewer))
                    .fetch();

            final Set<String> ids = ConcurrentHashMap.newKeySet();
            for (Record1<String> row : rows) {
                ids.add(row.value1().toLowerCase(Locale.ROOT));
            }
            seen.put(viewer, ids);
        }).exceptionally(throwable -> {
            log.error("Failed to load cutscene views for {}", viewer, throwable).submit();
            seen.putIfAbsent(viewer, ConcurrentHashMap.newKeySet());
            return null;
        });
    }

    public void unload(@NotNull UUID viewer) {
        seen.remove(viewer);
    }

    /**
     * @return true if this player has finished {@code cutsceneId} before. A player whose row set has not loaded yet
     * reads as unseen, which errs towards making them watch it - the safe direction.
     */
    public boolean hasSeen(@NotNull UUID viewer, @NotNull String cutsceneId) {
        final Set<String> ids = seen.get(viewer);
        return ids != null && ids.contains(cutsceneId.toLowerCase(Locale.ROOT));
    }

    /** Records that this player has now finished {@code cutsceneId}, bumping the count if they had seen it before. */
    public void record(@NotNull Player player, @NotNull String cutsceneId) {
        final UUID viewer = player.getUniqueId();
        seen.computeIfAbsent(viewer, key -> ConcurrentHashMap.newKeySet())
                .add(cutsceneId.toLowerCase(Locale.ROOT));

        database.getAsyncDslContext().executeAsyncVoid(context -> context
                .insertInto(table(name(TABLE)))
                .columns(field(name("viewer"), UUID.class), field(name("cutscene"), String.class))
                .values(viewer, cutsceneId)
                .onConflict(field(name("viewer"), UUID.class), field(name("cutscene"), String.class))
                .doUpdate()
                .set(field(name("times"), Integer.class), field(name("cutscene_views", "times"), Integer.class).plus(1))
                .set(field(name("last_seen"), OffsetDateTime.class), currentOffsetDateTime())
                .execute()
        ).exceptionally(throwable -> {
            log.error("Failed to record cutscene view {} for {}", cutsceneId, viewer, throwable).submit();
            return null;
        });
    }

    /** Forgets a player's history for one cutscene, so it plays as a first viewing again. For testing a cutscene. */
    public void forget(@NotNull UUID viewer, @NotNull String cutsceneId) {
        final Set<String> ids = seen.get(viewer);
        if (ids != null) {
            ids.remove(cutsceneId.toLowerCase(Locale.ROOT));
        }
        database.getAsyncDslContext().executeAsyncVoid(context -> context
                .deleteFrom(table(name(TABLE)))
                .where(field(name("viewer"), UUID.class).eq(viewer))
                .and(field(name("cutscene"), String.class).eq(cutsceneId))
                .execute()
        ).exceptionally(throwable -> {
            log.error("Failed to clear cutscene view {} for {}", cutsceneId, viewer, throwable).submit();
            return null;
        });
    }

    /** @return the ids this player has finished, for debug output. */
    public @NotNull Set<String> seenBy(@NotNull UUID viewer) {
        return new HashSet<>(seen.getOrDefault(viewer, Set.of()));
    }
}
