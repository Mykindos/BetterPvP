package me.mykindos.betterpvp.clans.world.camp.upgrade;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.world.site.Whereabouts;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jooq.Record;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;

/** Keeps last deaths in the {@code camp_casualties} table, one row per player, names as component JSON. */
@Singleton
@CustomLog
public class DatabaseCasualtyStore implements CasualtyStore {

    private static final String TABLE = "camp_casualties";

    private final Database database;

    @Inject
    public DatabaseCasualtyStore(@NotNull Database database) {
        this.database = database;
    }

    @Override
    public @NotNull CompletableFuture<Void> save(@NotNull Casualty casualty) {
        final Whereabouts where = casualty.getWhere();
        final String member = casualty.getMember().toString();
        final String place = json(where.getPlace());
        final String zone = json(where.getZone());
        final String killer = json(casualty.getKiller());
        return database.getAsyncDslContext().executeAsyncVoid(ctx -> ctx.insertInto(table(name(TABLE)),
                        field(name("member"), String.class),
                        field(name("server"), String.class),
                        field(name("place"), String.class),
                        field(name("zone"), String.class),
                        field(name("x"), Integer.class),
                        field(name("y"), Integer.class),
                        field(name("z"), Integer.class),
                        field(name("killer"), String.class),
                        field(name("died_at"), Long.class))
                .values(member, where.getServer(), place, zone, where.getX(), where.getY(), where.getZ(), killer,
                        casualty.getDiedAt())
                .onConflict(field(name("member"), String.class))
                .doUpdate()
                .set(field(name("server"), String.class), where.getServer())
                .set(field(name("place"), String.class), place)
                .set(field(name("zone"), String.class), zone)
                .set(field(name("x"), Integer.class), where.getX())
                .set(field(name("y"), Integer.class), where.getY())
                .set(field(name("z"), Integer.class), where.getZ())
                .set(field(name("killer"), String.class), killer)
                .set(field(name("died_at"), Long.class), casualty.getDiedAt())
                .execute()).exceptionally(ex -> {
            log.error("Could not save the last death of {}", member, ex).submit();
            return null;
        });
    }

    @Override
    public @NotNull CompletableFuture<Map<UUID, Casualty>> lastDeaths(@NotNull Collection<UUID> members) {
        final Collection<String> ids = members.stream().map(UUID::toString).toList();
        return database.getAsyncDslContext().executeAsync(ctx -> {
            final Map<UUID, Casualty> found = new HashMap<>();
            for (Record record : ctx.select().from(table(name(TABLE)))
                    .where(field(name("member"), String.class).in(ids))
                    .fetch()) {
                final UUID member = UUID.fromString(record.get(field(name("member"), String.class)));
                final Whereabouts where = new Whereabouts(record.get(field(name("server"), String.class)),
                        component(record.get(field(name("place"), String.class))),
                        component(record.get(field(name("zone"), String.class))),
                        record.get(field(name("x"), Integer.class)),
                        record.get(field(name("y"), Integer.class)),
                        record.get(field(name("z"), Integer.class)));
                found.put(member, new Casualty(member, where,
                        component(record.get(field(name("killer"), String.class))),
                        record.get(field(name("died_at"), Long.class))));
            }
            return found;
        });
    }

    private static @Nullable String json(@Nullable Component component) {
        return component == null ? null : GsonComponentSerializer.gson().serialize(component);
    }

    private static @Nullable Component component(@Nullable String json) {
        return json == null ? null : GsonComponentSerializer.gson().deserialize(json);
    }
}
