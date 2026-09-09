package me.mykindos.betterpvp.core.world.site.storage;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Site records as files on this machine's disk, one per site and owner.
 * <p>
 * A record is written to a neighbouring file and then moved into place, so a server killed mid-write leaves the
 * previous record intact rather than a half-written one. Every call is off the main thread, since a store somewhere
 * else would be too and callers should not be written twice.
 */
@Singleton
@CustomLog
public class LocalSiteStorage implements SiteStorage {

    /** What this store is named in config. */
    public static final String NAME = "local";

    private static final String EXTENSION = ".json";
    private static final String PENDING_EXTENSION = ".writing";

    private final Core core;
    private final Path root;

    @Inject
    public LocalSiteStorage(@NotNull Core core) {
        this.core = core;
        this.root = new File(core.getDataFolder(), "sites").toPath();
    }

    @Override
    public @NotNull CompletableFuture<Optional<byte[]>> read(@NotNull SiteKey key) {
        return async(() -> {
            final Path file = fileFor(key);
            return Files.isRegularFile(file) ? Optional.of(Files.readAllBytes(file)) : Optional.empty();
        });
    }

    @Override
    public @NotNull CompletableFuture<Void> write(@NotNull SiteKey key, byte @NotNull [] data) {
        return async(() -> {
            final Path file = fileFor(key);
            Files.createDirectories(file.getParent());

            final Path pending = file.resolveSibling(file.getFileName() + PENDING_EXTENSION);
            Files.write(pending, data);
            Files.move(pending, file, StandardCopyOption.REPLACE_EXISTING);
            return (Void) null;
        });
    }

    @Override
    public @NotNull CompletableFuture<Void> delete(@NotNull SiteKey key) {
        return async(() -> {
            Files.deleteIfExists(fileFor(key));
            return (Void) null;
        });
    }

    @Override
    public @NotNull CompletableFuture<Boolean> exists(@NotNull SiteKey key) {
        return async(() -> Files.isRegularFile(fileFor(key)));
    }

    private @NotNull Path fileFor(@NotNull SiteKey key) {
        return root.resolve(key.getSiteId()).resolve(key.getOwnerId() + EXTENSION);
    }

    private <T> @NotNull CompletableFuture<T> async(@NotNull Work<T> work) {
        final CompletableFuture<T> future = new CompletableFuture<>();

        UtilServer.runTaskAsync(core, () -> {
            try {
                future.complete(work.run());
            } catch (Exception ex) {
                future.completeExceptionally(ex);
            }
        });

        return future;
    }

    @FunctionalInterface
    private interface Work<T> {
        T run() throws Exception;
    }
}
