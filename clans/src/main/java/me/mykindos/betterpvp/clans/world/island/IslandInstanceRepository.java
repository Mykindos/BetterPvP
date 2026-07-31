package me.mykindos.betterpvp.clans.world.island;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.repository.IRepository;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.jooq.impl.DSL.*;

/**
 * Persists {@link IslandInstance} rows to the {@code island_instances} table, scoped to both {@link Core#getCurrentRealm()}
 * and the current server name. That second scope matters once instances are hosted across servers: two servers can
 * share a realm, and without it a restart of one would load — and {@link IslandBootRecovery} would destroy — the
 * other's live instances and strand their occupants.
 * <p>
 * Every instance the manager holds in memory is mirrored here so a crash or restart can recover (or discard) them via
 * {@link IslandBootRecovery}.
 */
@CustomLog
@Singleton
public class IslandInstanceRepository implements IRepository<IslandInstance> {

    private final Database database;

    @Inject
    public IslandInstanceRepository(@NotNull Database database) {
        this.database = database;
    }

    @Override
    public void save(IslandInstance instance) {
        database.getAsyncDslContext().executeAsyncVoid(ctx -> ctx.insertInto(table(name("island_instances")))
                        .columns(field(name("id"), String.class),
                                field(name("realm"), Integer.class),
                                field(name("server"), String.class),
                                field(name("template"), String.class),
                                field(name("world"), String.class),
                                field(name("state"), String.class),
                                field(name("created_at"), Long.class))
                        .values(instance.getId().toString(),
                                Core.getCurrentRealm().getId(),
                                Core.getCurrentRealm().getServer().getName(),
                                instance.getTemplate().getKey(),
                                instance.getWorldName(),
                                instance.getState().name(),
                                instance.getCreatedAt())
                        .execute())
                .exceptionally(ex -> {
                    log.error("Failed to save island instance {}", instance.getId(), ex).submit();
                    return null;
                });
    }

    public void updateState(@NotNull UUID id, @NotNull IslandInstanceState state) {
        database.getAsyncDslContext().executeAsyncVoid(ctx -> ctx.update(table(name("island_instances")))
                        .set(field(name("state"), String.class), state.name())
                        .where(field(name("id"), String.class).eq(id.toString()))
                        .execute())
                .exceptionally(ex -> {
                    log.error("Failed to update island instance {} state to {}", id, state, ex).submit();
                    return null;
                });
    }

    public void delete(@NotNull UUID id) {
        database.getAsyncDslContext().executeAsyncVoid(ctx -> ctx.deleteFrom(table(name("island_instances")))
                        .where(field(name("id"), String.class).eq(id.toString()))
                        .execute())
                .exceptionally(ex -> {
                    log.error("Failed to delete island instance {}", id, ex).submit();
                    return null;
                });
    }

    /** Loads every persisted row for the current realm and server. Templates are resolved by the caller. */
    public @NotNull List<IslandInstanceRecord> loadAll() {
        List<IslandInstanceRecord> records = new ArrayList<>();

        try {
            var results = database.getDslContext()
                    .select(field(name("id"), String.class),
                            field(name("template"), String.class),
                            field(name("world"), String.class),
                            field(name("state"), String.class),
                            field(name("created_at"), Long.class))
                    .from(table(name("island_instances")))
                    .where(field(name("realm"), Integer.class).eq(Core.getCurrentRealm().getId()))
                    .and(field(name("server"), String.class).eq(Core.getCurrentRealm().getServer().getName()))
                    .fetch();

            for (var result : results) {
                UUID id = UUID.fromString(result.get(field(name("id"), String.class)));
                String template = result.get(field(name("template"), String.class));
                String world = result.get(field(name("world"), String.class));
                String state = result.get(field(name("state"), String.class));
                long createdAt = result.get(field(name("created_at"), Long.class));

                records.add(new IslandInstanceRecord(id, template, world, state, createdAt));
            }
        } catch (Exception ex) {
            log.error("Failed to load island instances", ex).submit();
        }

        return records;
    }

}
