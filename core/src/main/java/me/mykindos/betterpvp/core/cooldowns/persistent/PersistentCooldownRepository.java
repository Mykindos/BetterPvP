package me.mykindos.betterpvp.core.cooldowns.persistent;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.database.Database;
import org.jetbrains.annotations.NotNull;
import org.jooq.Record;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.jooq.impl.DSL.*;

/**
 * Persistence for {@code player_cooldowns}. Uses the dynamic jOOQ DSL, so no generated classes are needed for this
 * table.
 */
@Singleton
@CustomLog
public class PersistentCooldownRepository {

    private static final long DB_TIMEOUT_SECONDS = 3;
    private static final String TABLE = "player_cooldowns";

    private final Database database;

    @Inject
    public PersistentCooldownRepository(@NotNull Database database) {
        this.database = database;
    }

    /**
     * @return the epoch millis at which the cooldown expires, or {@code 0} if this client has none under that key
     */
    public CompletableFuture<Long> expiresAt(long client, @NotNull String key) {
        return database.getAsyncDslContext().executeAsync(ctx -> {
            final Record record = ctx.select(field(name("expires_at"), Long.class))
                    .from(table(name(TABLE)))
                    .where(field(name("client"), Long.class).eq(client))
                    .and(field(name("cooldown_key"), String.class).eq(key))
                    .fetchOne();
            return record == null ? 0L : record.get(field(name("expires_at"), Long.class));
        }).orTimeout(DB_TIMEOUT_SECONDS, TimeUnit.SECONDS).exceptionally(ex -> {
            // Fail closed: a cooldown that cannot be read is treated as still running rather than handed out for free.
            log.error("Failed to read cooldown {} for client {}", key, client, ex).submit();
            return Long.MAX_VALUE;
        });
    }

    /** Sets (or replaces) the moment this cooldown expires. */
    public CompletableFuture<Void> set(long client, @NotNull String key, long expiresAtMs) {
        return database.getAsyncDslContext().executeAsyncVoid(ctx -> ctx.insertInto(table(name(TABLE)),
                        field(name("client"), Long.class),
                        field(name("cooldown_key"), String.class),
                        field(name("expires_at"), Long.class))
                .values(client, key, expiresAtMs)
                .onConflict(field(name("client"), Long.class), field(name("cooldown_key"), String.class))
                .doUpdate()
                .set(field(name("expires_at"), Long.class), expiresAtMs)
                .execute()).exceptionally(ex -> {
            log.error("Failed to set cooldown {} for client {}", key, client, ex).submit();
            return null;
        });
    }

    /** Ends a cooldown early. */
    public CompletableFuture<Void> clear(long client, @NotNull String key) {
        return database.getAsyncDslContext().executeAsyncVoid(ctx -> ctx.deleteFrom(table(name(TABLE)))
                .where(field(name("client"), Long.class).eq(client))
                .and(field(name("cooldown_key"), String.class).eq(key))
                .execute()).exceptionally(ex -> {
            log.error("Failed to clear cooldown {} for client {}", key, client, ex).submit();
            return null;
        });
    }

    /**
     * Drops rows that expired before {@code nowMs}. Housekeeping only — an expired row already reads as "not on
     * cooldown", so this changes nothing anyone can observe.
     *
     * @return the number of rows removed
     */
    public CompletableFuture<Integer> pruneExpired(long nowMs) {
        return database.getAsyncDslContext().executeAsync(ctx -> ctx.deleteFrom(table(name(TABLE)))
                .where(field(name("expires_at"), Long.class).lt(nowMs))
                .execute()).exceptionally(ex -> {
            log.error("Failed to prune expired cooldowns", ex).submit();
            return 0;
        });
    }
}
