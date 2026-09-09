package me.mykindos.betterpvp.core.world.travel;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Supplies the {@link Destination}s a player is offered by a navigator at the moment they interact with it. Unlike a
 * fixed destination list, a provider may work its offers out per player and per interaction, so a fixed set of
 * places and a pool of freshly made instances can sit side by side behind the same interface.
 */
@FunctionalInterface
public interface DestinationProvider {

    /**
     * @param player the player about to see the navigator's offerings
     * @return the destinations to present to {@code player} right now
     */
    @NotNull List<Destination> destinationsFor(@NotNull Player player);
}
