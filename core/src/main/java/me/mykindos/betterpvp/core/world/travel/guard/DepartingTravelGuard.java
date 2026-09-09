package me.mykindos.betterpvp.core.world.travel.guard;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.world.travel.DepartureController;
import me.mykindos.betterpvp.core.world.travel.Destination;
import me.mykindos.betterpvp.core.world.travel.TravelGuard;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Refuses travel while the player already has a departure in progress.
 */
@Singleton
public class DepartingTravelGuard implements TravelGuard {

    private final DepartureController departureController;

    @Inject
    public DepartingTravelGuard(DepartureController departureController) {
        this.departureController = departureController;
    }

    @Override
    public @NotNull Optional<Component> veto(@NotNull Player traveller, @NotNull Destination destination) {
        if (departureController.isDeparting(traveller)) {
            return Optional.of(Component.text("You are already departing somewhere.", NamedTextColor.RED));
        }
        return Optional.empty();
    }
}
