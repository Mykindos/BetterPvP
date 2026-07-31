package me.mykindos.betterpvp.clans.world.travel.guard;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.world.island.IslandAllocationTracker;
import me.mykindos.betterpvp.clans.world.travel.Destination;
import me.mykindos.betterpvp.clans.world.travel.TravelGuard;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Refuses travel while the player already has an island being provisioned. Cloning a world folder takes seconds, and
 * for that window the traveller is neither departing nor an occupant, so without this a second voyage could be started
 * before the first one lands.
 */
@Singleton
public class AllocatingTravelGuard implements TravelGuard {

    private final IslandAllocationTracker allocationTracker;

    @Inject
    public AllocatingTravelGuard(@NotNull IslandAllocationTracker allocationTracker) {
        this.allocationTracker = allocationTracker;
    }

    @Override
    public @NotNull Optional<Component> veto(@NotNull Player traveller, @NotNull Destination destination) {
        if (!allocationTracker.isAllocating(traveller)) {
            return Optional.empty();
        }
        return Optional.of(Component.text("Your ship is still being prepared.", NamedTextColor.RED));
    }
}
