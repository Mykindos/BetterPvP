package me.mykindos.betterpvp.core.world.travel;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.display.title.TitleComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.Set;

/**
 * The single entry point for sending a player to a {@link Destination}. Runs every registered {@link TravelGuard} and
 * hands off to {@link DepartureController} for the departure ceremony. The destination itself performs the actual
 * relocation once the ceremony completes.
 */
@Singleton
@CustomLog
public class TravelService {

    private final ClientManager clientManager;
    private final DepartureController departureController;
    private final Set<TravelGuard> guards;

    @Inject
    public TravelService(ClientManager clientManager, DepartureController departureController,
                          Set<TravelGuard> guards) {
        this.clientManager = clientManager;
        this.departureController = departureController;
        this.guards = guards;
    }

    public void travel(@NotNull Player traveller, @NotNull Destination destination) {
        if (!destination.isReady()) {
            UtilMessage.simpleMessage(traveller, "Travel", Component.text("That destination is not available right now.", NamedTextColor.RED));
            return;
        }

        for (TravelGuard guard : guards) {
            final Optional<Component> veto = guard.veto(traveller, destination);
            if (veto.isPresent()) {
                UtilMessage.simpleMessage(traveller, "Travel", veto.get());
                return;
            }
        }

        departureController.begin(traveller, destination, () -> completeTravel(traveller, destination));
    }

    /**
     * Travels with no departure hold. The guards still apply, but the destination receives the player at once.
     * <p>
     * For a destination that already makes the player wait on the way. Holding them still for three seconds first
     * only delays the part that is the wait.
     */
    public void travelNow(@NotNull Player traveller, @NotNull Destination destination) {
        if (!vet(traveller, destination)) {
            return;
        }

        completeTravel(traveller, destination);
    }

    /** @return {@code false} if the destination or a guard refused, having already told the traveller why */
    private boolean vet(@NotNull Player traveller, @NotNull Destination destination) {
        if (!destination.isReady()) {
            UtilMessage.message(traveller, "clans.prefix.travel", "clans.travel.unavailable");
            return false;
        }

        for (TravelGuard guard : guards) {
            final Optional<Component> veto = guard.veto(traveller, destination);
            if (veto.isPresent()) {
                UtilMessage.simpleMessage(traveller, "Travel", veto.get());
                return false;
            }
        }
        return true;
    }

    private void completeTravel(@NotNull Player traveller, @NotNull Destination destination) {
        destination.receive(traveller).whenComplete((arrived, error) -> {
            if (error != null || !Boolean.TRUE.equals(arrived)) {
                log.warn("Travel to {} failed for {}", destination.key(), traveller.getName()).submit();
                UtilMessage.simpleMessage(traveller, "Travel", Component.text("Something went wrong on arrival, please try again.", NamedTextColor.RED));
                return;
            }

            // The journey has started rather than finished, and runs its own cues when it actually ends.
            if (!destination.announcesArrival()) {
                return;
            }

            new SoundEffect(Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 1.3f).play(traveller);
            final Gamer gamer = clientManager.search().online(traveller).getGamer();
            gamer.getTitleQueue().add(5, new TitleComponent(0.1, 1.5, 0.3, false,
                    gmr -> destination.displayName(),
                    gmr -> Component.text("Welcome", NamedTextColor.GRAY)));
        });
    }
}
