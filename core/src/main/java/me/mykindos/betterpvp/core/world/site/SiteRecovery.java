package me.mykindos.betterpvp.core.world.site;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Decides on boot which of the previous run's instances are worth keeping, reading them from the
 * {@link SiteInstanceStore}.
 */
@CustomLog
@Singleton
public class SiteRecovery {

    private final SiteInstanceStore store;
    private final SiteRegistry registry;
    private final SiteWorlds worlds;

    @Inject
    public SiteRecovery(@NotNull SiteInstanceStore store, @NotNull SiteRegistry registry, @NotNull SiteWorlds worlds) {
        this.store = store;
        this.registry = registry;
        this.worlds = worlds;
    }

    /**
     * Reconciles the previous run against what is worth keeping. An instance whose world is a clone is thrown away,
     * since nobody is inside one after a restart and its contents are disposable. An instance whose world is adopted
     * or owned comes back dormant, so a clan camp is still there tomorrow. Cloned folders on disk that no record
     * claims are swept, which is what a crash mid-provision leaves behind.
     *
     * @return the instances that survived, all of them dormant
     */
    public @NotNull CompletableFuture<List<SiteInstance>> recover() {
        final List<SiteInstanceStore.Record> previous = store.loadAll();
        final List<SiteInstance> survivors = new ArrayList<>();
        final List<CompletableFuture<Void>> destroyed = new ArrayList<>();
        final Set<String> claimed = new HashSet<>();

        for (SiteInstanceStore.Record record : previous) {
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
            store.updateState(record.getId(), SiteInstance.State.DORMANT);
        }

        return CompletableFuture.allOf(destroyed.toArray(new CompletableFuture[0]))
                .thenCompose(unused -> sweepOrphans(claimed))
                .thenApply(orphans -> {
                    log.info("Site recovery: kept {} dormant instance(s), destroyed {}, swept {} orphaned folder(s)",
                            survivors.size(), destroyed.size(), orphans).submit();
                    return survivors;
                });
    }

    private @NotNull CompletableFuture<Void> discard(@NotNull SiteInstanceStore.Record record) {
        return worlds.destroy(record.getWorld())
                .handle((unused, ex) -> {
                    if (ex != null) {
                        log.warn("Failed to destroy leftover site world {}", record.getWorld(), ex).submit();
                    }
                    store.delete(record.getId());
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
