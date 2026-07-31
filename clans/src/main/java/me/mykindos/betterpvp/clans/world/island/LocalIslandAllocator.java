package me.mykindos.betterpvp.clans.world.island;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Allocates an {@link IslandInstance} on this server via {@link IslandInstanceManager}, warm pool included, and
 * wraps the result in a handle naming the current server.
 */
@Singleton
public class LocalIslandAllocator implements IslandAllocator {

    private final IslandInstanceManager instanceManager;

    @Inject
    public LocalIslandAllocator(@NotNull IslandInstanceManager instanceManager) {
        this.instanceManager = instanceManager;
    }

    @Override
    public @NotNull CompletableFuture<IslandHandle> allocate(@NotNull IslandTemplate template) {
        return instanceManager.allocate(template).thenApply(instance -> new IslandHandle(
                instance.getId(), template.getKey(), Core.getCurrentRealm().getServer().getName(), instance.getWorldName()));
    }

}
