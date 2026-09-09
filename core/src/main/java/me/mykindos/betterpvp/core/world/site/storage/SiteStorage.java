package me.mykindos.betterpvp.core.world.site.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Where a site's record is kept: everything about an instance that its world does not hold by itself.
 * <p>
 * A record is bytes as far as this is concerned. Only the module that wrote one knows what is in it, which is what
 * lets the store be swapped for one somewhere else entirely without anything above it changing. Local files and an
 * object store differ in latency and in nothing else, so every call answers a future.
 */
public interface SiteStorage {

    /** Shared so a record written by one caller reads back the same way for another. */
    ObjectMapper MAPPER = new ObjectMapper();

    @NotNull CompletableFuture<Optional<byte[]>> read(@NotNull SiteKey key);

    @NotNull CompletableFuture<Void> write(@NotNull SiteKey key, byte @NotNull [] data);

    @NotNull CompletableFuture<Void> delete(@NotNull SiteKey key);

    /** Whether anything has ever been written for this site, without reading it. */
    @NotNull CompletableFuture<Boolean> exists(@NotNull SiteKey key);

    /** Releases whatever the store holds. */
    default void close() {
    }

    /** Reads a record back as the type that wrote it, or empty when there is nothing to read. */
    default <T> @NotNull CompletableFuture<Optional<T>> read(@NotNull SiteKey key, @NotNull Class<T> type) {
        return read(key).thenApply(bytes -> bytes.flatMap(data -> {
            try {
                return Optional.of(MAPPER.readValue(data, type));
            } catch (Exception unreadable) {
                throw new IllegalStateException("Unreadable site record for " + key, unreadable);
            }
        }));
    }

    default @NotNull CompletableFuture<Void> write(@NotNull SiteKey key, @NotNull Object record) {
        try {
            return write(key, MAPPER.writeValueAsBytes(record));
        } catch (Exception unwritable) {
            return CompletableFuture.failedFuture(unwritable);
        }
    }
}
