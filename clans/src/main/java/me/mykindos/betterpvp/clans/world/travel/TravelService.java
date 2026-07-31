package me.mykindos.betterpvp.clans.world.travel;

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
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.Set;

/**
 * The single entry point for sending a player to a {@link Destination}. Runs every registered {@link TravelGuard},
 * records where the traveller came from, and hands off to {@link DepartureController} for the departure ceremony.
 * The destination itself performs the actual relocation once the ceremony completes.
 */
@Singleton
@CustomLog
public class TravelService {

    private final ClientManager clientManager;
    private final DepartureController departureController;
    private final TravelHistory travelHistory;
    private final Set<TravelGuard> guards;

    @Inject
    public TravelService(ClientManager clientManager, DepartureController departureController,
                          TravelHistory travelHistory, Set<TravelGuard> guards) {
        this.clientManager = clientManager;
        this.departureController = departureController;
        this.travelHistory = travelHistory;
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

        travelHistory.recordOrigin(traveller);
        departureController.begin(traveller, destination, () -> completeVoyage(traveller, destination));
    }

    private void completeVoyage(@NotNull Player traveller, @NotNull Destination destination) {
        destination.receive(traveller).whenComplete((arrived, error) -> {
            if (error != null || !Boolean.TRUE.equals(arrived)) {
                log.warn("Travel to {} failed for {}", destination.key(), traveller.getName()).submit();
                UtilMessage.simpleMessage(traveller, "Travel", Component.text("Something went wrong on arrival — please try again.", NamedTextColor.RED));
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
