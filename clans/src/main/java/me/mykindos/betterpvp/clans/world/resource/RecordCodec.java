package me.mykindos.betterpvp.clans.world.resource;

import org.jetbrains.annotations.NotNull;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * The per-payload strategy a {@link ResourceCacheStore} delegates to: how to derive a record's stable key (its filename
 * and mirror key) and how to serialise/deserialise its body. The store owns everything else — atomic writes, the
 * in-memory mirror, versioning — and never needs to understand the payload, so a record body must be self-describing
 * (it stores enough to rebuild its own key on load).
 *
 * @param <T> the record type this codec encodes
 */
public interface RecordCodec<T> {

    /** A stable, deterministic identity for {@code record}, reused across restarts to locate and overwrite its file. */
    @NotNull String key(@NotNull T record);

    /** Writes {@code record}'s body (the store has already written its own version header). */
    void write(@NotNull DataOutputStream out, @NotNull T record) throws IOException;

    /** Reads a record body previously written by {@link #write}. */
    @NotNull T read(@NotNull DataInputStream in) throws IOException;
}
