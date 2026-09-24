package me.mykindos.betterpvp.core.world.site;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Where players are on the network right now. */
public interface PlayerWhereabouts {

    /** Where each of {@code players} is, leaving out anyone this cannot see online. */
    @NotNull CompletableFuture<Map<UUID, Whereabouts>> locate(@NotNull Collection<UUID> players);
}
