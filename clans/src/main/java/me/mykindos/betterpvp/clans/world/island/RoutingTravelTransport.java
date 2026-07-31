package me.mykindos.betterpvp.clans.world.island;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Dispatches delivery per {@link IslandHandle} on whichever server it names: a handle naming the current server
 * goes straight to {@link LocalTravelTransport}, and a handle naming another server fails cleanly for now. A remote
 * implementation lands later as an additional injected collaborator, replacing only the failure branch below.
 */
@CustomLog
@Singleton
public class RoutingTravelTransport implements TravelTransport {

    private final LocalTravelTransport localTransport;

    @Inject
    public RoutingTravelTransport(@NotNull LocalTravelTransport localTransport) {
        this.localTransport = localTransport;
    }

    @Override
    public @NotNull CompletableFuture<Boolean> deliver(@NotNull Player traveller, @NotNull IslandHandle handle) {
        final String currentServer = Core.getCurrentRealm().getServer().getName();
        if (handle.getServer().equals(currentServer)) {
            return localTransport.deliver(traveller, handle);
        }

        log.warn("Cannot deliver {} to island handle {} - it is hosted by '{}' and remote delivery is not implemented yet",
                traveller.getName(), handle.getInstanceId(), handle.getServer()).submit();
        return CompletableFuture.failedFuture(new UnsupportedOperationException(
                "Island handle '" + handle.getInstanceId() + "' is hosted by '" + handle.getServer() + "', which remote delivery does not support yet"));
    }

}
