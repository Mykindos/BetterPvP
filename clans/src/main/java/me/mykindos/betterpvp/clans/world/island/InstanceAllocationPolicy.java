package me.mykindos.betterpvp.clans.world.island;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Decides whether a player may be routed to an already-provisioned {@link IslandInstance} rather than getting a
 * freshly provisioned one. Swappable so occupancy can move from solo to shared/pooled without touching
 * {@link IslandInstanceManager}.
 */
public interface InstanceAllocationPolicy {

    /**
     * @return whether {@code player} may join {@code instance} alongside its current occupants
     */
    boolean canAccept(@NotNull IslandInstance instance, @NotNull Player player);

    /**
     * @return the maximum number of occupants an instance under this policy may hold
     */
    int capacity();

}
