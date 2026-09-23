package me.mykindos.betterpvp.clans.world.camp.settler.prosperity;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.database.Database;
import org.jetbrains.annotations.NotNull;
import org.jooq.Record;

import java.util.LinkedHashMap;
import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;

/** Keeps Prosperity in the {@code camp_prosperity} table, one row per clan. */
@Singleton
@CustomLog
public class DatabaseProsperityStore implements ProsperityStore {

    public static final String NAME = "database";
    private static final String TABLE = "camp_prosperity";

    private final Database database;

    @Inject
    public DatabaseProsperityStore(@NotNull Database database) {
        this.database = database;
    }

    @Override
    public @NotNull CompletableFuture<Void> save(long clanId, int prosperity) {
        final long now = System.currentTimeMillis();
        return database.getAsyncDslContext().executeAsyncVoid(ctx -> ctx.insertInto(table(name(TABLE)),
                        field(name("clan"), Long.class),
                        field(name("prosperity"), Integer.class),
                        field(name("updated_at"), Long.class))
                .values(clanId, prosperity, now)
                .onConflict(field(name("clan"), Long.class))
                .doUpdate()
                .set(field(name("prosperity"), Integer.class), prosperity)
                .set(field(name("updated_at"), Long.class), now)
                .execute()).exceptionally(ex -> {
            log.error("Could not save Prosperity for clan {}", clanId, ex).submit();
            return null;
        });
    }

    @Override
    public @NotNull CompletableFuture<Void> delete(long clanId) {
        return database.getAsyncDslContext().executeAsyncVoid(ctx -> ctx.deleteFrom(table(name(TABLE)))
                .where(field(name("clan"), Long.class).eq(clanId))
                .execute()).exceptionally(ex -> {
            log.error("Could not delete Prosperity for clan {}", clanId, ex).submit();
            return null;
        });
    }

    @Override
    public @NotNull LinkedHashMap<Long, Integer> top(int limit) {
        final LinkedHashMap<Long, Integer> top = new LinkedHashMap<>();
        for (Record record : database.getDslContext().select()
                .from(table(name(TABLE)))
                .orderBy(field(name("prosperity"), Integer.class).desc())
                .limit(limit)
                .fetch()) {
            top.put(record.get(field(name("clan"), Long.class)), record.get(field(name("prosperity"), Integer.class)));
        }
        return top;
    }

    @Override
    public @NotNull OptionalInt find(long clanId) {
        final Integer prosperity = database.getDslContext()
                .select(field(name("prosperity"), Integer.class))
                .from(table(name(TABLE)))
                .where(field(name("clan"), Long.class).eq(clanId))
                .fetchOne(field(name("prosperity"), Integer.class));
        return prosperity == null ? OptionalInt.empty() : OptionalInt.of(prosperity);
    }
}
