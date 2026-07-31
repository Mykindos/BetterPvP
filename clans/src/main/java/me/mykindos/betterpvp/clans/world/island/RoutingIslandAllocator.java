package me.mykindos.betterpvp.clans.world.island;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Dispatches allocation per {@link IslandTemplate} via {@link IslandHostRouter}: a locally-hosted template goes
 * straight to {@link LocalIslandAllocator}, and a remotely-hosted one fails cleanly for now. A remote implementation
 * lands later as an additional injected collaborator, replacing only the failure branch below.
 */
@CustomLog
@Singleton
public class RoutingIslandAllocator implements IslandAllocator {

    private final IslandHostRouter router;
    private final LocalIslandAllocator localAllocator;

    @Inject
    public RoutingIslandAllocator(@NotNull IslandHostRouter router, @NotNull LocalIslandAllocator localAllocator) {
        this.router = router;
        this.localAllocator = localAllocator;
    }

    @Override
    public @NotNull CompletableFuture<IslandHandle> allocate(@NotNull IslandTemplate template) {
        if (router.isLocal(template)) {
            return localAllocator.allocate(template);
        }

        final String host = router.hostFor(template);
        log.warn("Cannot allocate island template {} - it is hosted by '{}' and remote allocation is not implemented yet", template.getKey(), host).submit();
        return CompletableFuture.failedFuture(new UnsupportedOperationException(
                "Island template '" + template.getKey() + "' is hosted by '" + host + "', which remote allocation does not support yet"));
    }

}
