package me.mykindos.betterpvp.clans.world.island;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.core.framework.events.ServerStartEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Discards every island instance left over from the previous run. Island instances are ephemeral: after a restart no
 * live instance is meaningful and nobody can still be inside one, so on {@link ServerStartEvent} every persisted row
 * for the current realm is destroyed, and the {@code islands/} folder is swept for orphaned world folders that were
 * never fully recorded (a crash mid-provision leaves these behind).
 */
@CustomLog
@BPvPListener
@Singleton
public class IslandBootRecovery implements Listener {

    private final Clans clans;
    private final IslandInstanceRepository repository;
    private final IslandWorldProvisioner provisioner;
    private final CompletableFuture<Void> recovered = new CompletableFuture<>();

    @Inject
    public IslandBootRecovery(@NotNull Clans clans, @NotNull IslandInstanceRepository repository, @NotNull IslandWorldProvisioner provisioner) {
        this.clans = clans;
        this.repository = repository;
        this.provisioner = provisioner;
    }

    /**
     * Completes once leftover instances from the previous run are destroyed and the {@code islands/} folder has
     * been swept for orphans. {@link IslandWarmPool} chains its initial refill onto this so it never provisions
     * into a world folder the sweep is still deciding whether to delete.
     */
    public @NotNull CompletableFuture<Void> whenRecovered() {
        return recovered;
    }

    @EventHandler
    public void onServerStart(ServerStartEvent event) {
        final List<IslandInstanceRecord> leftover = repository.loadAll();
        final Set<String> knownWorlds = leftover.stream().map(IslandInstanceRecord::getWorld).collect(Collectors.toSet());

        final List<CompletableFuture<Void>> destroyFutures = new ArrayList<>();
        for (IslandInstanceRecord record : leftover) {
            destroyFutures.add(provisioner.destroy(record.getWorld())
                    .handle((unused, ex) -> {
                        if (ex != null) {
                            log.warn("Failed to destroy leftover island world {}", record.getWorld(), ex).submit();
                        }
                        repository.delete(record.getId());
                        return null;
                    }));
        }

        CompletableFuture.allOf(destroyFutures.toArray(new CompletableFuture[0]))
                .thenRun(() -> sweepOrphans(knownWorlds, leftover.size()));
    }

    private void sweepOrphans(@NotNull Set<String> knownWorlds, int recoveredCount) {
        UtilServer.runTaskAsync(clans, () -> {
            final File islandsRoot = new File(Bukkit.getWorldContainer(), "islands");
            int orphans = 0;

            final File[] templateDirs = islandsRoot.listFiles(File::isDirectory);
            if (templateDirs != null) {
                for (File templateDir : templateDirs) {
                    final File[] instanceDirs = templateDir.listFiles(File::isDirectory);
                    if (instanceDirs == null) {
                        continue;
                    }

                    for (File instanceDir : instanceDirs) {
                        final String worldName = "islands/" + templateDir.getName() + "/" + instanceDir.getName();
                        if (knownWorlds.contains(worldName)) {
                            continue;
                        }

                        try {
                            provisioner.purgeResourceCache(worldName);
                            FileUtils.forceDelete(instanceDir);
                            orphans++;
                        } catch (IOException ex) {
                            log.warn("Failed to delete orphaned island folder {}", instanceDir, ex).submit();
                        }
                    }
                }
            }

            log.info("Island boot recovery: destroyed {} leftover instance(s), removed {} orphaned folder(s)",
                    recoveredCount, orphans).submit();
            recovered.complete(null);
        });
    }

}
