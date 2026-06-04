package me.mykindos.betterpvp.clans.world.resource;

import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A crash-surviving key→record store: one tiny binary file per record under a fixed directory, written the moment a
 * record changes and removed the moment it is dropped. Per-record files mean every mutation is an O(1) synchronous
 * write (no whole-file rewrite, no debounce window, so a hard crash loses at most the record being written) and the set
 * self-prunes by deletion. Writes go to a {@code .tmp} sibling atomically renamed into place, so a crash mid-write can
 * never leave a half-parsed record. An in-memory mirror loaded once at construction answers reads without touching disk.
 * <p>
 * The store is payload-agnostic: a {@link RecordCodec} supplies each record's key and body serialisation, so the same
 * machinery backs single-block respawn records ({@link BlockReplacementStore}) and batches of blocks
 * ({@link BlockBatchStore}) alike. The store is deliberately decoupled from the world (no per-block PDC), so deleting
 * the folder is a full reset and a rebuilt scene can never silently desync.
 *
 * @param <T> the record type, encoded by the supplied {@link RecordCodec}
 */
@CustomLog
public class ResourceCacheStore<T> {

    /** Bumped if the on-disk envelope ever changes; mismatched files are ignored on load. */
    private static final byte FORMAT_VERSION = 1;
    private static final String EXTENSION = ".bin";
    private static final String TEMP_EXTENSION = ".tmp";

    private final File directory;
    private final RecordCodec<T> codec;
    private final Map<String, T> records = new ConcurrentHashMap<>();

    public ResourceCacheStore(@NotNull File directory, @NotNull RecordCodec<T> codec) {
        this.directory = directory;
        this.codec = codec;
        load();
    }

    /** Stores (or overwrites) a record, keyed by {@link RecordCodec#key}. */
    public void put(@NotNull T record) {
        final String key = codec.key(record);
        records.put(key, record);
        write(key, record);
    }

    /** The record under {@code key}, or empty if none is held. */
    public @NotNull Optional<T> get(@NotNull String key) {
        return Optional.ofNullable(records.get(key));
    }

    /** A live view of every held record — callers layer their own queries (e.g. spatial filters) over this. */
    public @NotNull Collection<T> values() {
        return records.values();
    }

    /** Drops the record under {@code key} and deletes its file. */
    public void remove(@NotNull String key) {
        if (records.remove(key) == null) {
            return;
        }
        final File file = fileFor(key);
        if (file.exists() && !file.delete()) {
            log.warn("Could not delete resource cache record {}", file).submit();
        }
    }

    private void write(@NotNull String key, @NotNull T record) {
        final File destination = fileFor(key);
        final File temp = new File(directory, destination.getName() + TEMP_EXTENSION);
        directory.mkdirs();
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(temp)))) {
            out.writeByte(FORMAT_VERSION);
            codec.write(out, record);
        } catch (IOException exception) {
            log.error("Failed to write resource cache record {}", destination, exception).submit();
            return;
        }
        try {
            Files.move(temp.toPath(), destination.toPath(),
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException notAtomic) {
            try {
                Files.move(temp.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException exception) {
                log.error("Failed to commit resource cache record {}", destination, exception).submit();
            }
        } catch (IOException exception) {
            log.error("Failed to commit resource cache record {}", destination, exception).submit();
        }
    }

    private void load() {
        final File[] files = directory.listFiles((dir, name) -> name.endsWith(EXTENSION));
        if (files == null) {
            return;
        }
        for (File file : files) {
            try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
                if (in.readByte() != FORMAT_VERSION) {
                    continue;
                }
                final T record = codec.read(in);
                records.put(codec.key(record), record);
            } catch (IOException | RuntimeException exception) {
                log.warn("Skipping unreadable resource cache record {}", file).submit();
            }
        }
    }

    private @NotNull File fileFor(@NotNull String key) {
        // Deterministic, filesystem-safe, collision-resistant name so overwrites and removals locate the same file
        // across restarts without an index. The hash disambiguates keys that sanitise to the same string.
        final String safe = key.replaceAll("[^A-Za-z0-9_.-]", "_");
        return new File(directory, safe + "_" + Integer.toHexString(key.hashCode()) + EXTENSION);
    }
}
