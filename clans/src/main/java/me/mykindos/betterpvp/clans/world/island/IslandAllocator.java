package me.mykindos.betterpvp.clans.world.island;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Decides where an {@link IslandInstance} for a template comes from, provisioning one or claiming it from the warm
 * pool. Which server a place lives on is answered by {@code Placement} in core, not here.
 */
public interface IslandAllocator {

    @NotNull CompletableFuture<IslandHandle> allocate(@NotNull IslandTemplate template);

}
