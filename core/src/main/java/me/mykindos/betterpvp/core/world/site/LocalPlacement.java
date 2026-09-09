package me.mykindos.betterpvp.core.world.site;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Placement on a single server. Everything is here, so locating is a local lookup and sending is a teleport. A site
 * or handle naming another server is refused with a clear message rather than pretending, which is the whole of the
 * single-server story until a network implementation lands.
 */
@CustomLog
@Singleton
public class LocalPlacement implements Placement {

    private final SiteRegistry registry;
    private final SiteInstances instances;

    @Inject
    public LocalPlacement(@NotNull SiteRegistry registry, @NotNull SiteInstances instances) {
        this.registry = registry;
        this.instances = instances;
    }

    @Override
    public @NotNull String hostFor(@NotNull Site site) {
        final String host = site.getPolicy().getServer();
        return host == null ? currentServer() : host;
    }

    @Override
    public boolean isLocal(@NotNull Site site) {
        return hostFor(site).equals(currentServer());
    }

    @Override
    public @NotNull CompletableFuture<SiteHandle> locate(@NotNull SiteKey key, @NotNull Party party) {
        final Optional<Site> site = registry.get(key.getSiteId());
        if (site.isEmpty()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("No such site: " + key));
        }

        if (!isLocal(site.get())) {
            final String host = hostFor(site.get());
            log.warn("Cannot place a party on site {} - it is hosted by '{}' and remote placement is not implemented yet", key, host).submit();
            return CompletableFuture.failedFuture(new UnsupportedOperationException(
                    "Site '" + key + "' is hosted by '" + host + "', which this server cannot reach"));
        }

        return instances.locate(key, party)
                .thenApply(instance -> new SiteHandle(instance.getId(), key, currentServer(), instance.getWorldName()));
    }

    @Override
    public @NotNull CompletableFuture<Boolean> send(@NotNull Player traveller, @NotNull SiteHandle handle) {
        return sendAll(List.of(traveller), handle);
    }

    @Override
    public @NotNull CompletableFuture<Boolean> sendAll(@NotNull Collection<Player> travellers, @NotNull SiteHandle handle) {
        if (travellers.isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }

        if (!handle.getServer().equals(currentServer())) {
            log.warn("Cannot send {} traveller(s) to instance {} - it is on '{}' and remote delivery is not implemented yet",
                    travellers.size(), handle.getInstanceId(), handle.getServer()).submit();
            return CompletableFuture.failedFuture(new UnsupportedOperationException(
                    "Instance '" + handle.getInstanceId() + "' is on '" + handle.getServer() + "', which this server cannot reach"));
        }

        final World world = Bukkit.getWorld(handle.getWorld());
        if (world == null) {
            log.warn("Cannot send anybody to instance {} - world '{}' is not loaded", handle.getInstanceId(), handle.getWorld()).submit();
            return CompletableFuture.completedFuture(false);
        }

        final Location landing = landingIn(world, handle);
        final List<CompletableFuture<Boolean>> arrivals = travellers.stream()
                .map(traveller -> traveller.teleportAsync(landing))
                .toList();

        return CompletableFuture.allOf(arrivals.toArray(CompletableFuture[]::new)).thenApply(ignored -> true);
    }

    /** The spot a party is put down at, read off the site's own arrival markers. */
    private @NotNull Location landingIn(@NotNull World world, @NotNull SiteHandle handle) {
        return registry.get(handle.getKey().getSiteId())
                .map(site -> ArrivalPoints.choose(world, site.getArrivalMarker(), site.getArrival()))
                .orElseGet(world::getSpawnLocation);
    }

    private @NotNull String currentServer() {
        return Core.getCurrentRealm().getServer().getName();
    }
}
