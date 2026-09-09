package me.mykindos.betterpvp.core.world.travel.guard;

import me.mykindos.betterpvp.core.world.travel.DepartureController;
import me.mykindos.betterpvp.core.world.travel.Destination;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepartingTravelGuardTest {

    @Mock
    private DepartureController departureController;

    @Mock
    private Player traveller;

    @Mock
    private Destination destination;

    @Test
    @DisplayName("vetoes travel while a departure is already in progress for this player")
    void vetoesWhileDeparting() {
        when(departureController.isDeparting(traveller)).thenReturn(true);

        final Optional<Component> veto = new DepartingTravelGuard(departureController).veto(traveller, destination);

        assertTrue(veto.isPresent());
    }

    @Test
    @DisplayName("passes when no departure is in progress for this player")
    void passesWhenNotDeparting() {
        when(departureController.isDeparting(traveller)).thenReturn(false);

        final Optional<Component> veto = new DepartingTravelGuard(departureController).veto(traveller, destination);

        assertTrue(veto.isEmpty());
    }
}
