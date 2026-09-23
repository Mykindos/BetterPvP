package me.mykindos.betterpvp.core.world.site;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** Where each player's {@link Residence} records are kept, one per {@link Kind}. */
public interface ResidencyStore {

    /** Which of a player's two records is meant. */
    enum Kind {
        /** The instance they were last in, and the spot they were standing on. */
        RESIDENCE,
        /** The last site that will have them back. */
        ANCHOR
    }

    /**
     * Both of a player's records, blocking on the read.
     * <p>
     * Called while a connection is still being configured, which is off the main thread and is the only moment the
     * answer is needed before the player exists. Anything on the main thread reads the copy {@link Residency} keeps.
     */
    @NotNull Map<Kind, Residence> load(long client);

    /** Writes one record, replacing whatever that player had under the same kind. */
    @NotNull CompletableFuture<Void> save(long client, @NotNull Kind kind, @NotNull Residence residence);

    @NotNull CompletableFuture<Void> clear(long client, @NotNull Kind kind);
}
