package me.mykindos.betterpvp.core.world.site;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.utilities.UtilWorld;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.jooq.impl.DSL.*;

/**
 * The {@code site_residency} table, holding one row per player per {@link Kind}.
 * <p>
 * The record is spread across columns rather than stored as one serialised string, so a stale instance can be found
 * by id when an instance is released and the table can be read without the code that wrote it.
 */
@CustomLog
@Singleton
public class ResidencyStore {

    /** Which of a player's two records a row holds. */
    public enum Kind {
        /** The instance they were last in, and the spot they were standing on. */
        RESIDENCE,
        /** The last site that will have them back. */
        ANCHOR
    }

    private static final String TABLE = "site_residency";

    private final Database database;

    @Inject
    public ResidencyStore(@NotNull Database database) {
        this.database = database;
    }

    /**
     * Both of a player's records, blocking on the query.
     * <p>
     * Called while a connection is still being configured, which is off the main thread and is the only moment the
     * answer is needed before the player exists. Anything on the main thread reads the copy {@link Residency} keeps.
     */
    public @NotNull Map<Kind, Residence> load(long client) {
        final Map<Kind, Residence> records = new EnumMap<>(Kind.class);

        try {
            final var results = database.getDslContext()
                    .select(field(name("kind"), String.class),
                            field(name("site"), String.class),
                            field(name("owner"), Long.class),
                            field(name("instance"), String.class),
                            field(name("server"), String.class),
                            field(name("location"), String.class))
                    .from(table(name(TABLE)))
                    .where(field(name("client"), Long.class).eq(client))
                    .fetch();

            for (var result : results) {
                final String kind = result.get(field(name("kind"), String.class));
                try {
                    records.put(Kind.valueOf(kind), new Residence(
                            SiteKey.of(result.get(field(name("site"), String.class)),
                                    result.get(field(name("owner"), Long.class))),
                            UUID.fromString(result.get(field(name("instance"), String.class))),
                            new ServerLocation(result.get(field(name("server"), String.class)),
                                    UtilWorld.stringToLocation(result.get(field(name("location"), String.class))))));
                } catch (RuntimeException exception) {
                    log.warn("Discarding unreadable {} row for client {}", kind, client, exception).submit();
                }
            }
        } catch (Exception exception) {
            log.error("Failed to load residency for client {}", client, exception).submit();
        }

        return records;
    }

    /** Writes one record, replacing whatever that player had under the same kind. */
    public @NotNull CompletableFuture<Void> save(long client, @NotNull Kind kind, @NotNull Residence residence) {
        return database.getAsyncDslContext().executeAsyncVoid(ctx -> ctx.insertInto(table(name(TABLE)))
                        .columns(field(name("client"), Long.class),
                                field(name("kind"), String.class),
                                field(name("site"), String.class),
                                field(name("owner"), Long.class),
                                field(name("instance"), String.class),
                                field(name("server"), String.class),
                                field(name("location"), String.class),
                                field(name("updated_at"), Long.class))
                        .values(client,
                                kind.name(),
                                residence.getSite().getSiteId(),
                                residence.getSite().getOwnerId(),
                                residence.getInstanceId().toString(),
                                residence.getLocation().getServer(),
                                UtilWorld.locationToString(residence.getLocation().getLocation(), false),
                                System.currentTimeMillis())
                        .onConflict(field(name("client"), Long.class), field(name("kind"), String.class))
                        .doUpdate()
                        .set(field(name("site"), String.class), residence.getSite().getSiteId())
                        .set(field(name("owner"), Long.class), residence.getSite().getOwnerId())
                        .set(field(name("instance"), String.class), residence.getInstanceId().toString())
                        .set(field(name("server"), String.class), residence.getLocation().getServer())
                        .set(field(name("location"), String.class),
                                UtilWorld.locationToString(residence.getLocation().getLocation(), false))
                        .set(field(name("updated_at"), Long.class), System.currentTimeMillis())
                        .execute())
                .exceptionally(exception -> {
                    log.error("Failed to save {} for client {}", kind, client, exception).submit();
                    return null;
                });
    }

    public @NotNull CompletableFuture<Void> clear(long client, @NotNull Kind kind) {
        return database.getAsyncDslContext().executeAsyncVoid(ctx -> ctx.deleteFrom(table(name(TABLE)))
                        .where(field(name("client"), Long.class).eq(client))
                        .and(field(name("kind"), String.class).eq(kind.name()))
                        .execute())
                .exceptionally(exception -> {
                    log.error("Failed to clear {} for client {}", kind, client, exception).submit();
                    return null;
                });
    }
}
