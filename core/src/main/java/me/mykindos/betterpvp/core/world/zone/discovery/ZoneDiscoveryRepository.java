package me.mykindos.betterpvp.core.world.zone.discovery;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.database.Database;
import org.jooq.Record;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static me.mykindos.betterpvp.core.utilities.SnowflakeIdGenerator.ID_GENERATOR;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;

/**
 * Persistence for zone discoveries. The {@code zones} table holds (snowflake id, unique zone key, display name) and is
 * populated lazily on first discovery; {@code zone_discoveries} links a client to a zone with a timestamp.
 * <p>
 * Uses the dynamic jOOQ DSL ({@code table(name(...))} / {@code field(name(...), Type.class)}) so no generated jOOQ
 * classes are required for these tables.
 */
@Singleton
@CustomLog
public class ZoneDiscoveryRepository {

    private static final long DB_TIMEOUT_SECONDS = 3;

    private final Database database;

    @Inject
    public ZoneDiscoveryRepository(Database database) {
        this.database = database;
    }

    /**
     * Inserts the zone row if absent (keeping the display name current on conflict) and returns its snowflake id.
     *
     * @param zoneKey     the stable zone key (namespace:value)
     * @param displayName the zone's plain-text display name
     * @return the persisted zone id, or {@code null} if the operation failed
     */
    public CompletableFuture<Long> ensureZoneId(String zoneKey, String displayName) {
        final long newId = ID_GENERATOR.nextId();
        return database.getAsyncDslContext().executeAsync(ctx -> {
            final Record record = ctx.insertInto(table(name("zones")),
                            field(name("id"), Long.class),
                            field(name("zone_key"), String.class),
                            field(name("display_name"), String.class))
                    .values(newId, zoneKey, displayName)
                    .onConflict(field(name("zone_key"), String.class))
                    .doUpdate()
                    .set(field(name("display_name"), String.class), displayName)
                    .returning(field(name("id"), Long.class))
                    .fetchOne();
            return record == null ? null : record.get(field(name("id"), Long.class));
        }).orTimeout(DB_TIMEOUT_SECONDS, TimeUnit.SECONDS).exceptionally(ex -> {
            log.error("Failed to ensure zone id for {}", zoneKey, ex).submit();
            return null;
        });
    }

    /**
     * Records a discovery, ignoring duplicates (a client discovers a given zone at most once).
     */
    public CompletableFuture<Void> insertDiscovery(long clientId, long zoneId) {
        return database.getAsyncDslContext().executeAsyncVoid(ctx -> ctx.insertInto(table(name("zone_discoveries")),
                        field(name("client"), Long.class),
                        field(name("zone_id"), Long.class))
                .values(clientId, zoneId)
                .onConflict(field(name("client"), Long.class), field(name("zone_id"), Long.class))
                .doNothing()
                .execute()).exceptionally(ex -> {
            log.error("Failed to insert zone discovery for client {} zone {}", clientId, zoneId, ex).submit();
            return null;
        });
    }

    /**
     * @return a map of discovered zone key → zone id for the given client (used to warm in-memory caches on join)
     */
    public CompletableFuture<Map<String, Long>> loadForClient(long clientId) {
        return database.getAsyncDslContext().executeAsync(ctx -> {
            final Map<String, Long> result = new HashMap<>();
            ctx.select(field(name("z", "zone_key"), String.class), field(name("z", "id"), Long.class))
                    .from(table(name("zone_discoveries")).as("d"))
                    .join(table(name("zones")).as("z"))
                    .on(field(name("d", "zone_id"), Long.class).eq(field(name("z", "id"), Long.class)))
                    .where(field(name("d", "client"), Long.class).eq(clientId))
                    .fetch()
                    .forEach(record -> result.put(record.value1(), record.value2()));
            return result;
        }).orTimeout(DB_TIMEOUT_SECONDS, TimeUnit.SECONDS).exceptionally(ex -> {
            log.error("Failed to load zone discoveries for client {}", clientId, ex).submit();
            return new HashMap<>();
        });
    }

    /**
     * @return the client's discoveries joined with each zone's display name and timestamp, newest last
     */
    public CompletableFuture<List<ZoneDiscoveryRecord>> listForClient(long clientId) {
        return database.getAsyncDslContext().executeAsync(ctx -> {
            final List<ZoneDiscoveryRecord> result = new ArrayList<>();
            ctx.select(field(name("z", "zone_key"), String.class),
                            field(name("z", "display_name"), String.class),
                            field(name("d", "discovered_at"), LocalDateTime.class))
                    .from(table(name("zone_discoveries")).as("d"))
                    .join(table(name("zones")).as("z"))
                    .on(field(name("d", "zone_id"), Long.class).eq(field(name("z", "id"), Long.class)))
                    .where(field(name("d", "client"), Long.class).eq(clientId))
                    .orderBy(field(name("d", "discovered_at"), LocalDateTime.class).asc())
                    .fetch()
                    .forEach(record -> result.add(new ZoneDiscoveryRecord(record.value1(), record.value2(), record.value3())));
            return result;
        }).orTimeout(DB_TIMEOUT_SECONDS, TimeUnit.SECONDS).exceptionally(ex -> {
            log.error("Failed to list zone discoveries for client {}", clientId, ex).submit();
            return new ArrayList<>();
        });
    }

    /**
     * Removes a single discovery (by zone key) for a client, if present.
     */
    public CompletableFuture<Void> deleteForClientAndKey(long clientId, String zoneKey) {
        return database.getAsyncDslContext().executeAsyncVoid(ctx -> ctx.deleteFrom(table(name("zone_discoveries")))
                .where(field(name("client"), Long.class).eq(clientId))
                .and(field(name("zone_id"), Long.class).in(
                        ctx.select(field(name("id"), Long.class))
                                .from(table(name("zones")))
                                .where(field(name("zone_key"), String.class).eq(zoneKey))))
                .execute()).exceptionally(ex -> {
            log.error("Failed to delete zone discovery for client {} zone {}", clientId, zoneKey, ex).submit();
            return null;
        });
    }

    /**
     * Removes all discoveries for a client.
     */
    public CompletableFuture<Void> deleteAllForClient(long clientId) {
        return database.getAsyncDslContext().executeAsyncVoid(ctx -> ctx.deleteFrom(table(name("zone_discoveries")))
                .where(field(name("client"), Long.class).eq(clientId))
                .execute()).exceptionally(ex -> {
            log.error("Failed to delete all zone discoveries for client {}", clientId, ex).submit();
            return null;
        });
    }
}
