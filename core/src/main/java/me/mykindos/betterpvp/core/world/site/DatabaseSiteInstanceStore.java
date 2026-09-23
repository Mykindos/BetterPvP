package me.mykindos.betterpvp.core.world.site;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.database.Database;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;

/** Keeps site instances in the {@code site_instances} table, scoped to the current realm and server. */
@CustomLog
@Singleton
public class DatabaseSiteInstanceStore implements SiteInstanceStore {

    private final Database database;

    @Inject
    public DatabaseSiteInstanceStore(@NotNull Database database) {
        this.database = database;
    }

    @Override
    public void save(@NotNull SiteInstance instance) {
        database.getAsyncDslContext().executeAsyncVoid(ctx -> ctx.insertInto(table(name("site_instances")))
                        .columns(field(name("id"), String.class),
                                field(name("realm"), Integer.class),
                                field(name("server"), String.class),
                                field(name("site"), String.class),
                                field(name("owner"), Long.class),
                                field(name("world"), String.class),
                                field(name("state"), String.class),
                                field(name("created_at"), Long.class))
                        .values(instance.getId().toString(),
                                Core.getCurrentRealm().getId(),
                                Core.getCurrentRealm().getServer().getName(),
                                instance.getKey().getSiteId(),
                                instance.getKey().getOwnerId(),
                                instance.getWorldName(),
                                instance.getState().name(),
                                instance.getCreatedAt())
                        .execute())
                .exceptionally(ex -> {
                    log.error("Failed to save site instance {}", instance.getId(), ex).submit();
                    return null;
                });
    }

    @Override
    public void updateState(@NotNull UUID id, @NotNull SiteInstance.State state) {
        database.getAsyncDslContext().executeAsyncVoid(ctx -> ctx.update(table(name("site_instances")))
                        .set(field(name("state"), String.class), state.name())
                        .where(field(name("id"), String.class).eq(id.toString()))
                        .execute())
                .exceptionally(ex -> {
                    log.error("Failed to update site instance {} state to {}", id, state, ex).submit();
                    return null;
                });
    }

    @Override
    public void delete(@NotNull UUID id) {
        database.getAsyncDslContext().executeAsyncVoid(ctx -> ctx.deleteFrom(table(name("site_instances")))
                        .where(field(name("id"), String.class).eq(id.toString()))
                        .execute())
                .exceptionally(ex -> {
                    log.error("Failed to delete site instance {}", id, ex).submit();
                    return null;
                });
    }

    @Override
    public @NotNull List<Record> loadAll() {
        final List<Record> records = new ArrayList<>();

        try {
            final var results = database.getDslContext()
                    .select(field(name("id"), String.class),
                            field(name("site"), String.class),
                            field(name("owner"), Long.class),
                            field(name("world"), String.class),
                            field(name("created_at"), Long.class))
                    .from(table(name("site_instances")))
                    .where(field(name("realm"), Integer.class).eq(Core.getCurrentRealm().getId()))
                    .and(field(name("server"), String.class).eq(Core.getCurrentRealm().getServer().getName()))
                    .fetch();

            for (var result : results) {
                records.add(new Record(
                        UUID.fromString(result.get(field(name("id"), String.class))),
                        SiteKey.of(result.get(field(name("site"), String.class)), result.get(field(name("owner"), Long.class))),
                        result.get(field(name("world"), String.class)),
                        result.get(field(name("created_at"), Long.class))));
            }
        } catch (Exception ex) {
            log.error("Failed to load site instances", ex).submit();
        }

        return records;
    }
}
