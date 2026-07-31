package me.mykindos.betterpvp.clans.world.island;

import com.google.inject.Singleton;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which players currently have an island allocation in flight.
 * <p>
 * Provisioning clones a world folder, which takes seconds. For that whole window the traveller is not departing and
 * not yet an occupant, so nothing else would stop them starting a second allocation — which would clone another world
 * folder and strand the first instance with an occupant who never arrives.
 */
@Singleton
public class IslandAllocationTracker {

    private final Set<UUID> allocating = ConcurrentHashMap.newKeySet();

    /**
     * @return {@code true} if this player may start an allocation, {@code false} if one is already in flight
     */
    public boolean begin(@NotNull Player player) {
        return allocating.add(player.getUniqueId());
    }

    public void finish(@NotNull Player player) {
        allocating.remove(player.getUniqueId());
    }

    public boolean isAllocating(@NotNull Player player) {
        return allocating.contains(player.getUniqueId());
    }
}
