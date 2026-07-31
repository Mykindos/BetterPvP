package me.mykindos.betterpvp.clans.world.travel.guard;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.world.island.IslandInstanceManager;
import me.mykindos.betterpvp.clans.world.island.IslandOffer;
import me.mykindos.betterpvp.clans.world.travel.Destination;
import me.mykindos.betterpvp.clans.world.travel.TravelGuard;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Refuses travel to a discovery island offer while the player is already standing on one — they must leave their
 * current island (see {@code /leaveisland}) before a new instance is allocated for them.
 */
@Singleton
public class AlreadyOnIslandTravelGuard implements TravelGuard {

    private final IslandInstanceManager instanceManager;

    @Inject
    public AlreadyOnIslandTravelGuard(@NotNull IslandInstanceManager instanceManager) {
        this.instanceManager = instanceManager;
    }

    @Override
    public @NotNull Optional<Component> veto(@NotNull Player traveller, @NotNull Destination destination) {
        if (!(destination instanceof IslandOffer)) {
            return Optional.empty();
        }

        if (instanceManager.byWorld(traveller.getWorld().getName()).isPresent()) {
            return Optional.of(Component.text("You must leave your current island before travelling to another one.", NamedTextColor.RED));
        }

        return Optional.empty();
    }
}
