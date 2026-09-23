package me.mykindos.betterpvp.core.world.site;

import lombok.Value;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Where live site instances are recorded so they survive a restart. Records are scoped to the server writing them.
 */
public interface SiteInstanceStore {

    /** A stored instance, before it is resolved against a site. */
    @Value
    class Record {
        @NotNull UUID id;
        @NotNull SiteKey key;
        @NotNull String world;
        long createdAt;
    }

    void save(@NotNull SiteInstance instance);

    void updateState(@NotNull UUID id, @NotNull SiteInstance.State state);

    void delete(@NotNull UUID id);

    /** Every record this server wrote, blocking on the read. */
    @NotNull List<Record> loadAll();
}
