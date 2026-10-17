package me.mykindos.betterpvp.core.world.site;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilWorld;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameRules;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * The disk and Bukkit side of a site: names an instance's world, opens it whether that means cloning a template or
 * loading a folder already on disk, and unloads or deletes it afterwards.
 */
@CustomLog
@Singleton
public class SiteWorlds {

    /** What every cloned instance world's name begins with, whatever site it came from. */
    public static final String WORLD_ROOT = "sites/";

    /**
     * Names the template a world folder was built from, written inside the folder itself.
     * <p>
     * An owner who can change what their world is built from needs the old answer to compare against, and keeping it
     * beside the folder means it cannot drift from what is actually on disk.
     */
    private static final String TEMPLATE_MARKER = ".site-template";

    private final Core core;

    @Inject
    public SiteWorlds(@NotNull Core core) {
        this.core = core;
    }

    /**
     * The world-name prefix every clone of one site shares. Content that should appear on all of them selects worlds
     * by this rather than by name, since an instance's full name is only decided when it is created.
     */
    public static @NotNull String worldNamePrefix(@NotNull String siteId) {
        return WORLD_ROOT + siteId + "/";
    }

    /**
     * The world name an instance takes. An adopted site always resolves to the same world, an owned site to one world
     * per owner, and a cloned site to a fresh name per instance.
     */
    public @NotNull String worldNameFor(@NotNull Site site, @NotNull SiteKey key, @NotNull UUID instanceId) {
        final WorldSource source = site.getWorldSource();
        return switch (source.getKind()) {
            case ADOPT -> source.getValue();
            case OWN -> source.getValue() + key.getOwnerId();
            case CLONE -> worldNamePrefix(site.getId()) + instanceId.toString().substring(0, 8);
        };
    }

    /**
     * Makes a world available and loaded. A folder already on disk is loaded as it stands, which is what adopting a
     * hand-built world and waking a dormant one both come down to. A missing folder is cloned from the site's
     * template. An owned site with no template generates one instead, since nothing says an owner's world has to
     * start from anything in particular.
     */
    public @NotNull CompletableFuture<World> open(@NotNull WorldSource source, @NotNull String worldName) {
        return open(source, worldName, null);
    }

    /**
     * As {@link #open(WorldSource, String)}, with the template the owner chose rather than the one the site was
     * configured with. An owned folder already on disk that was built from a different template is rebuilt from this
     * one, which is the whole of what changing it comes down to.
     * <p>
     * A null template means the owner has not chosen, which is deliberately not the same as choosing the site's
     * default: a folder already on disk is left exactly as it is. Otherwise a record that had not finished loading
     * would read as "no choice" and rebuild somebody's world out from under them.
     */
    public @NotNull CompletableFuture<World> open(@NotNull WorldSource source, @NotNull String worldName,
                                                  @Nullable String template) {
        final World loaded = Bukkit.getWorld(worldName);
        if (loaded != null) {
            return CompletableFuture.completedFuture(loaded);
        }

        if (isOnDisk(worldName)) {
            if (source.getKind() != WorldSource.Kind.OWN || template == null || template.equals(builtFrom(worldName))) {
                return load(worldName, false);
            }

            log.info("World '{}' was built from '{}' and is now '{}' - rebuilding", worldName, builtFrom(worldName), template).submit();
            return destroy(worldName).thenCompose(unused -> rebuild(template, worldName));
        }

        if (source.getKind() == WorldSource.Kind.ADOPT) {
            return CompletableFuture.failedFuture(new IllegalStateException(
                    "World '" + worldName + "' is adopted but does not exist"));
        }

        final String seed = source.getKind() == WorldSource.Kind.CLONE
                ? source.getValue()
                : Objects.requireNonNullElse(template, source.getTemplate());

        // An owned site with nothing to build from generates a world instead, since nothing says an owner's world
        // has to start from anything in particular.
        return seed == null ? load(worldName, true) : rebuild(seed, worldName);
    }

    /** The template is copied to the world's import folder, and Paper moves it into storage as the world loads. */
    private @NotNull CompletableFuture<World> rebuild(@NotNull String template, @NotNull String worldName) {
        return copyTemplate(template, worldName, importFolder(worldName))
                .thenCompose(unused -> load(worldName, true))
                .thenApply(world -> {
                    markBuiltFrom(world, template);
                    return world;
                });
    }

    private static @NotNull File storageFolder(@NotNull String worldName) {
        return UtilWorld.getStorageFolder(worldName).toFile();
    }

    private static @NotNull File importFolder(@NotNull String worldName) {
        return UtilWorld.getImportFolder(worldName);
    }

    private static boolean isOnDisk(@NotNull String worldName) {
        return storageFolder(worldName).isDirectory() || new File(importFolder(worldName), "level.dat").isFile();
    }

    /** The template a world on disk was built from, or null for one made before it was worth recording. */
    private @Nullable String builtFrom(@NotNull String worldName) {
        final String stored = readMarker(storageFolder(worldName));
        return stored != null ? stored : readMarker(importFolder(worldName));
    }

    private @Nullable String readMarker(@NotNull File folder) {
        final File marker = new File(folder, TEMPLATE_MARKER);
        if (!marker.isFile()) {
            return null;
        }

        try {
            return Files.readString(marker.toPath()).trim();
        } catch (IOException unreadable) {
            log.warn("Could not read the template marker in {}", folder, unreadable).submit();
            return null;
        }
    }

    private void markBuiltFrom(@NotNull World world, @NotNull String template) {
        try {
            Files.writeString(world.getWorldPath().resolve(TEMPLATE_MARKER), template);
        } catch (IOException unwritable) {
            log.warn("Could not record the template for {}", world.getName(), unwritable).submit();
        }
    }

    /** Unloads a world, saving it and leaving its folder alone so it can be opened again later. */
    public @NotNull CompletableFuture<Void> unload(@NotNull String worldName) {
        final CompletableFuture<Void> future = new CompletableFuture<>();

        UtilServer.runTask(core, () -> {
            final World world = Bukkit.getWorld(worldName);
            if (world == null) {
                future.complete(null);
                return;
            }

            evacuate(world);
            if (!Bukkit.unloadWorld(world, true)) {
                future.completeExceptionally(new IllegalStateException("Could not unload world: " + worldName));
                return;
            }

            future.complete(null);
        });

        return future;
    }

    /**
     * Unloads a world and deletes its folder. Anything keyed to the world is told first, so state referring to it is
     * never left behind pointing at nothing.
     */
    public @NotNull CompletableFuture<Void> destroy(@NotNull String worldName) {
        final CompletableFuture<Void> future = new CompletableFuture<>();

        UtilServer.runTask(core, () -> {
            final World world = Bukkit.getWorld(worldName);
            if (world != null) {
                evacuate(world);
                if (!Bukkit.unloadWorld(world, false)) {
                    log.warn("Refusing to delete site world {} - it is still loaded", worldName).submit();
                    future.completeExceptionally(new IllegalStateException("Could not unload world: " + worldName));
                    return;
                }
            }

            // Only once the world is definitely going. A failed unload above leaves it alive, and it must keep its
            // harvest state rather than being silently reset.
            release(worldName, future);
        });

        return future;
    }

    /**
     * Deletes a world folder nothing is tracking, announcing it first exactly as {@link #destroy} does. Used to sweep
     * folders a crash left behind before any instance was recorded for them.
     */
    public @NotNull CompletableFuture<Void> discard(@NotNull String worldName) {
        final CompletableFuture<Void> future = new CompletableFuture<>();
        UtilServer.runTask(core, () -> release(worldName, future));
        return future;
    }

    /** Announces the world as gone, then deletes its folders off-thread. Must be called on the main thread. */
    private void release(@NotNull String worldName, @NotNull CompletableFuture<Void> future) {
        UtilServer.callEvent(new SiteWorldReleasedEvent(worldName));
        deleteFolders(worldName).whenComplete((unused, ex) -> {
            if (ex != null) {
                future.completeExceptionally(ex);
            } else {
                future.complete(null);
            }
        });
    }

    /** Every cloned instance world currently on disk, whether or not anything is tracking it. */
    public @NotNull CompletableFuture<List<String>> clonedWorldsOnDisk() {
        final CompletableFuture<List<String>> future = new CompletableFuture<>();
        final List<File> roots = List.of(storageFolder(WORLD_ROOT), importFolder(WORLD_ROOT));

        UtilServer.runTaskAsync(core, () -> {
            final Set<String> worlds = new LinkedHashSet<>();
            for (File root : roots) {
                final File[] siteDirs = root.listFiles(File::isDirectory);
                if (siteDirs == null) {
                    continue;
                }
                for (File siteDir : siteDirs) {
                    final File[] instanceDirs = siteDir.listFiles(File::isDirectory);
                    if (instanceDirs == null) {
                        continue;
                    }
                    for (File instanceDir : instanceDirs) {
                        worlds.add(worldNamePrefix(siteDir.getName()) + instanceDir.getName());
                    }
                }
            }

            future.complete(new ArrayList<>(worlds));
        });

        return future;
    }

    private @NotNull CompletableFuture<Void> copyTemplate(@NotNull String templateFolder, @NotNull String worldName,
                                                          @NotNull File target) {
        final CompletableFuture<Void> future = new CompletableFuture<>();

        UtilServer.runTaskAsync(core, () -> {
            try {
                final File template = new File(Bukkit.getWorldContainer(), templateFolder);
                if (!template.isDirectory()) {
                    future.completeExceptionally(new IllegalStateException(
                            "Site template folder does not exist: " + template.getAbsolutePath()));
                    return;
                }

                FileUtils.copyDirectory(template, target);
                forceDelete(new File(target, "session.lock"));
                forceDelete(new File(target, "uid.dat"));
                log.info("Cloned site template '{}' into world '{}'", templateFolder, worldName).submit();
                future.complete(null);
            } catch (Exception ex) {
                future.completeExceptionally(ex);
            }
        });

        return future;
    }

    /**
     * Creates the Bukkit world for a folder. {@code discardOnFailure} is set only when this call is what put the
     * folder there, so a hand-built world that fails to load is never deleted out from under us.
     */
    private @NotNull CompletableFuture<World> load(@NotNull String worldName, boolean discardOnFailure) {
        final CompletableFuture<World> future = new CompletableFuture<>();

        UtilServer.runTask(core, () -> {
            try {
                final World world = UtilWorld.createWorld(WorldCreator.name(worldName));
                if (world == null) {
                    if (discardOnFailure) {
                        discardQuietly(worldName);
                    }
                    future.completeExceptionally(new IllegalStateException("Failed to create world: " + worldName));
                    return;
                }

                world.setGameRule(GameRules.PVP, true);
                future.complete(world);
            } catch (Exception ex) {
                if (discardOnFailure) {
                    discardQuietly(worldName);
                }
                future.completeExceptionally(ex);
            }
        });

        return future;
    }

    /** Sends everyone still inside to the default world, since Bukkit refuses to unload an occupied one. */
    private void evacuate(@NotNull World world) {
        final World fallback = Bukkit.getWorlds().getFirst();
        world.getPlayers().forEach(player -> player.teleport(fallback.getSpawnLocation()));
    }

    private @NotNull CompletableFuture<Void> deleteFolders(@NotNull String worldName) {
        final CompletableFuture<Void> future = new CompletableFuture<>();
        final File storage = storageFolder(worldName);
        final File imported = importFolder(worldName);

        UtilServer.runTaskAsync(core, () -> {
            try {
                forceDelete(storage);
                forceDelete(imported);
                future.complete(null);
            } catch (Exception ex) {
                future.completeExceptionally(ex);
            }
        });

        return future;
    }

    /** Removes the folders left behind by a clone whose world could not be created. */
    private void discardQuietly(@NotNull String worldName) {
        final File storage = storageFolder(worldName);
        final File imported = importFolder(worldName);
        UtilServer.runTaskAsync(core, () -> {
            try {
                forceDelete(storage);
                forceDelete(imported);
            } catch (Exception ex) {
                log.warn("Could not discard orphaned site folders for {}", worldName, ex).submit();
            }
        });
    }

    private void forceDelete(@NotNull File file) throws IOException {
        if (file.exists()) {
            FileUtils.forceDelete(file);
        }
    }
}
