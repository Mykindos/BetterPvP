package me.mykindos.betterpvp.core.framework.net;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The directory for a network of one. Everything published is everything there is, and every question about other
 * servers has the same answer: there are none.
 * <p>
 * Reservations still count, because two parties on this server can still race each other, and a caller should not
 * have to know which directory it is talking to.
 */
@Singleton
public class LocalSiteDirectory implements SiteDirectory {

    private final Map<UUID, RemoteInstance> instances = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> reserved = new ConcurrentHashMap<>();
    private final Map<UUID, RemoteInstance> expected = new ConcurrentHashMap<>();

    @Override
    public void publish(@NotNull Collection<RemoteInstance> published) {
        instances.keySet().retainAll(published.stream().map(RemoteInstance::getId).toList());
        published.forEach(instance -> instances.put(instance.getId(), instance));
    }

    @Override
    public @NotNull CompletableFuture<List<RemoteInstance>> lookup(@NotNull String siteId, long ownerId) {
        final List<RemoteInstance> found = new ArrayList<>();
        for (RemoteInstance instance : instances.values()) {
            if (instance.getSiteId().equals(siteId) && instance.getOwnerId() == ownerId) {
                found.add(instance);
            }
        }
        return CompletableFuture.completedFuture(found);
    }

    @Override
    public @NotNull CompletableFuture<Boolean> reserve(@NotNull UUID instance, int seats, int capacity) {
        synchronized (reserved) {
            final int taken = occupantsOf(instance) + reserved.getOrDefault(instance, 0);
            if (capacity > 0 && taken + seats > capacity) {
                return CompletableFuture.completedFuture(false);
            }

            reserved.merge(instance, seats, Integer::sum);
            return CompletableFuture.completedFuture(true);
        }
    }

    @Override
    public void release(@NotNull UUID instance, int seats) {
        synchronized (reserved) {
            reserved.computeIfPresent(instance, (key, held) -> held - seats <= 0 ? null : held - seats);
        }
    }

    @Override
    public @NotNull CompletableFuture<String> stickyHost(@NotNull String siteId, long ownerId,
                                                         @NotNull String candidate) {
        return CompletableFuture.completedFuture(candidate);
    }

    @Override
    public @NotNull CompletableFuture<List<String>> serversByLoad() {
        return CompletableFuture.completedFuture(List.of(Core.getCurrentRealm().getServer().getName()));
    }

    @Override
    public void expect(@NotNull UUID player, @NotNull RemoteInstance instance) {
        expected.put(player, instance);
    }

    @Override
    public @NotNull CompletableFuture<Optional<RemoteInstance>> claimArrival(@NotNull UUID player) {
        return CompletableFuture.completedFuture(Optional.ofNullable(expected.remove(player)));
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public boolean isShared() {
        return false;
    }

    private int occupantsOf(@NotNull UUID instance) {
        final RemoteInstance known = instances.get(instance);
        return known == null ? 0 : known.getOccupants();
    }
}
