package me.mykindos.betterpvp.core.world.site;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.Value;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.repository.IRepository;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;

/**
 * What survives a restart. Every live instance is mirrored to the {@code site_instances} table, scoped to both the
 * current realm and the current server, and {@link #recover()} decides on boot which of the previous run's rows are
 * worth keeping.
 */
@CustomLog
@Singleton
public class SiteStore implements IRepository<SiteInstance> {

    /** A row as it comes back off the table, before it is resolved against a site. */
    @Value
    public static class Record {
        @NotNull UUID id;
        @NotNull SiteKey key;
        @NotNull String world;
        long createdAt;
    }

    private final Database database;
    private final SiteRegistry registry;
    private final SiteWorlds worlds;

    @Inject
    public SiteStore(@NotNull Database database, @NotNull SiteRegistry registry, @NotNull SiteWorlds worlds) {
        this.database = database;
        this.registry = registry;
        this.worlds = worlds;
    }

    @Override
    public void save(SiteInstance instance) {
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

    public void delete(@NotNull UUID id) {
        database.getAsyncDslContext().executeAsyncVoid(ctx -> ctx.deleteFrom(table(name("site_instances")))
                        .where(field(name("id"), String.class).eq(id.toString()))
                        .execute())
                .exceptionally(ex -> {
                    log.error("Failed to delete site instance {}", id, ex).submit();
                    return null;
                });
    }

    /** Every persisted row for the current realm and server. Sites are resolved by the caller. */
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

    /**
     * Reconciles the previous run against what is worth keeping. An instance whose world is a clone is thrown away,
     * since nobody is inside one after a restart and its contents are disposable. An instance whose world is adopted
     * or owned comes back dormant, so a clan camp is still there tomorrow. Cloned folders on disk that no row claims
     * are swept, which is what a crash mid-provision leaves behind.
     *
     * @return the instances that survived, all of them dormant
     */
    public @NotNull CompletableFuture<List<SiteInstance>> recover() {
        final List<Record> previous = loadAll();
        final List<SiteInstance> survivors = new ArrayList<>();
        final List<CompletableFuture<Void>> destroyed = new ArrayList<>();
        final Set<String> claimed = new HashSet<>();

        for (Record record : previous) {
            final Optional<Site> site = registry.get(record.getKey().getSiteId());
            if (site.isEmpty()) {
                log.warn("Site instance {} refers to unknown site '{}' - discarding", record.getId(), record.getKey()).submit();
                destroyed.add(discard(record));
                continue;
            }

            if (site.get().getWorldSource().getKind() == WorldSource.Kind.CLONE) {
                claimed.add(record.getWorld());
                destroyed.add(discard(record));
                continue;
            }

            claimed.add(record.getWorld());
            survivors.add(new SiteInstance(record.getId(), record.getKey(), record.getWorld(), SiteInstance.State.DORMANT));
            updateState(record.getId(), SiteInstance.State.DORMANT);
        }

        return CompletableFuture.allOf(destroyed.toArray(new CompletableFuture[0]))
                .thenCompose(unused -> sweepOrphans(claimed))
                .thenApply(orphans -> {
                    log.info("Site recovery: kept {} dormant instance(s), destroyed {}, swept {} orphaned folder(s)",
                            survivors.size(), destroyed.size(), orphans).submit();
                    return survivors;
                });
    }

    private @NotNull CompletableFuture<Void> discard(@NotNull Record record) {
        return worlds.destroy(record.getWorld())
                .handle((unused, ex) -> {
                    if (ex != null) {
                        log.warn("Failed to destroy leftover site world {}", record.getWorld(), ex).submit();
                    }
                    delete(record.getId());
                    return null;
                });
    }

    private @NotNull CompletableFuture<Integer> sweepOrphans(@NotNull Set<String> claimed) {
        return worlds.clonedWorldsOnDisk().thenCompose(onDisk -> {
            final List<String> orphans = onDisk.stream().filter(world -> !claimed.contains(world)).toList();
            final List<CompletableFuture<Void>> deletions = orphans.stream()
                    .map(world -> worlds.discard(world).exceptionally(ex -> {
                        log.warn("Failed to delete orphaned site folder {}", world, ex).submit();
                        return null;
                    }))
                    .toList();

            return CompletableFuture.allOf(deletions.toArray(new CompletableFuture[0])).thenApply(unused -> orphans.size());
        });
    }
}
