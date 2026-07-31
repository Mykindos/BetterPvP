package me.mykindos.betterpvp.clans.world.island;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Decides where an {@link IslandInstance} for a template comes from. A local implementation provisions (or claims
 * from the warm pool) on this server; a future remote implementation would ask whichever server
 * {@link IslandHostRouter} names to allocate one and hand back its {@link IslandHandle} instead. Callers never need
 * to know which happened.
 */
public interface IslandAllocator {

    @NotNull CompletableFuture<IslandHandle> allocate(@NotNull IslandTemplate template);

}
