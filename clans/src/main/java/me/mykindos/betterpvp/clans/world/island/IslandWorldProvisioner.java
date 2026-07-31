package me.mykindos.betterpvp.clans.world.island;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.world.resource.BlockBatchStore;
import me.mykindos.betterpvp.clans.world.resource.BlockReplacementStore;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Provisions and tears down the Bukkit world backing an {@link IslandInstance} by cloning an {@link IslandTemplate}'s
 * world folder. The template folder is only ever touched on disk — it is never loaded as a Bukkit world.
 */
@CustomLog
@Singleton
public class IslandWorldProvisioner {

    private final Clans clans;
    private final BlockBatchStore blockBatchStore;
    private final BlockReplacementStore blockReplacementStore;

    @Inject
    public IslandWorldProvisioner(@NotNull Clans clans, @NotNull BlockBatchStore blockBatchStore,
                                   @NotNull BlockReplacementStore blockReplacementStore) {
        this.clans = clans;
        this.blockBatchStore = blockBatchStore;
        this.blockReplacementStore = blockReplacementStore;
    }

    /**
     * Clones {@code template}'s world folder and creates the Bukkit world from it. The copy runs off-thread; world
     * creation is hopped back onto the main thread since {@link Bukkit#createWorld(WorldCreator)} requires it.
     */
    public @NotNull CompletableFuture<World> provision(@NotNull IslandTemplate template, @NotNull UUID instanceId) {
        final CompletableFuture<World> future = new CompletableFuture<>();
        final String worldName = "islands/" + template.getKey() + "/" + instanceId.toString().substring(0, 8);

        UtilServer.runTaskAsync(clans, () -> {
            try {
                final File templateFolder = new File(Bukkit.getWorldContainer(), template.getTemplateFolder());
                if (!templateFolder.isDirectory()) {
                    future.completeExceptionally(new IllegalStateException(
                            "Island template folder does not exist: " + templateFolder.getAbsolutePath()));
                    return;
                }

                final File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
                FileUtils.copyDirectory(templateFolder, worldFolder);

                final File sessionLock = new File(worldFolder, "session.lock");
                if (sessionLock.exists()) {
                    FileUtils.forceDelete(sessionLock);
                }
                final File uidFile = new File(worldFolder, "uid.dat");
                if (uidFile.exists()) {
                    FileUtils.forceDelete(uidFile);
                }

                UtilServer.runTask(clans, () -> {
                    try {
                        final World world = Bukkit.createWorld(WorldCreator.name(worldName));
                        if (world == null) {
                            discardFolder(worldFolder);
                            future.completeExceptionally(new IllegalStateException("Failed to create world: " + worldName));
                            return;
                        }

                        world.setPVP(true);
                        world.setKeepSpawnInMemory(false);
                        future.complete(world);
                    } catch (Exception ex) {
                        discardFolder(worldFolder);
                        future.completeExceptionally(ex);
                    }
                });
            } catch (Exception ex) {
                future.completeExceptionally(ex);
            }
        });

        return future;
    }

    /** Unloads {@code worldName} on the main thread, then deletes its folder off-thread. Purges the resource-node cache first, so a destroyed island's mid-respawn ore records and tree fell frames never outlive its world folder. */
    public @NotNull CompletableFuture<Void> destroy(@NotNull String worldName) {
        final CompletableFuture<Void> future = new CompletableFuture<>();

        UtilServer.runTask(clans, () -> {
            final World world = Bukkit.getWorld(worldName);
            final File worldFolder = world != null ? world.getWorldFolder() : new File(Bukkit.getWorldContainer(), worldName);

            if (world != null) {
                // Unload refuses while anyone is still inside, so clear the world before asking.
                final World fallback = Bukkit.getWorlds().getFirst();
                world.getPlayers().forEach(player -> player.teleport(fallback.getSpawnLocation()));

                if (!Bukkit.unloadWorld(world, false)) {
                    log.warn("Refusing to delete island world {} — it is still loaded", worldName).submit();
                    future.completeExceptionally(new IllegalStateException("Could not unload world: " + worldName));
                    return;
                }
            }

            UtilServer.runTaskAsync(clans, () -> {
                try {
                    // Only once the world is definitely going: a failed unload above leaves the island alive, and it
                    // must keep its harvest state rather than being silently reset.
                    purgeResourceCache(worldName);
                    if (worldFolder.exists()) {
                        FileUtils.forceDelete(worldFolder);
                    }
                    future.complete(null);
                } catch (Exception ex) {
                    future.completeExceptionally(ex);
                }
            });
        });

        return future;
    }

    /**
     * Drops every resource-node cache record (mid-respawn ore blocks, in-flight tree fell frames) keyed to
     * {@code worldName}. Exposed for {@link IslandBootRecovery}'s orphan sweep, which deletes leftover island folders
     * directly rather than through {@link #destroy}.
     */
    public void purgeResourceCache(@NotNull String worldName) {
        final int batches = blockBatchStore.purgeWorld(worldName);
        final int blocks = blockReplacementStore.purgeWorld(worldName);
        if (batches > 0 || blocks > 0) {
            log.info("Purged resource-node cache for world '{}': {} block batch(es), {} block record(s)",
                    worldName, batches, blocks).submit();
        }
    }

    /** Removes a world folder left behind by a clone whose world could not be created. */
    private void discardFolder(@NotNull File worldFolder) {
        UtilServer.runTaskAsync(clans, () -> {
            try {
                if (worldFolder.exists()) {
                    FileUtils.forceDelete(worldFolder);
                }
            } catch (Exception ex) {
                log.warn("Could not discard orphaned island folder {}", worldFolder, ex).submit();
            }
        });
    }

}
