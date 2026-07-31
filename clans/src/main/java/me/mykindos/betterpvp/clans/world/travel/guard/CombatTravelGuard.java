package me.mykindos.betterpvp.clans.world.travel.guard;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.world.travel.Destination;
import me.mykindos.betterpvp.clans.world.travel.TravelGuard;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Refuses travel while the player is flagged in combat.
 */
@Singleton
public class CombatTravelGuard implements TravelGuard {

    private final ClientManager clientManager;

    @Inject
    public CombatTravelGuard(ClientManager clientManager) {
        this.clientManager = clientManager;
    }

    @Override
    public @NotNull Optional<Component> veto(@NotNull Player traveller, @NotNull Destination destination) {
        final Gamer gamer = clientManager.search().online(traveller).getGamer();
        if (gamer.isInCombat()) {
            return Optional.of(Component.text("You cannot travel while in combat.", NamedTextColor.RED));
        }
        return Optional.empty();
    }
}
