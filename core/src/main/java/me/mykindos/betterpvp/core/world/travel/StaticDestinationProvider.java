package me.mykindos.betterpvp.core.world.travel;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A {@link DestinationProvider} that always offers the same fixed list of destinations, regardless of who is asking.
 * Used for places that exist once and are shared by every player.
 */
public class StaticDestinationProvider implements DestinationProvider {

    private final List<Destination> destinations;

    public StaticDestinationProvider(@NotNull List<Destination> destinations) {
        this.destinations = destinations;
    }

    @Override
    public @NotNull List<Destination> destinationsFor(@NotNull Player player) {
        return destinations;
    }
}
