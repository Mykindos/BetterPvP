package me.mykindos.betterpvp.clans.world.travel.guard;

import me.mykindos.betterpvp.clans.world.island.IslandAllocationTracker;
import me.mykindos.betterpvp.clans.world.travel.Destination;
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
class AllocatingTravelGuardTest {

    @Mock
    private IslandAllocationTracker allocationTracker;

    @Mock
    private Player traveller;

    @Mock
    private Destination destination;

    @Test
    @DisplayName("vetoes travel while an island allocation is in flight for this player")
    void vetoesWhileAllocating() {
        when(allocationTracker.isAllocating(traveller)).thenReturn(true);

        final Optional<Component> veto = new AllocatingTravelGuard(allocationTracker).veto(traveller, destination);

        assertTrue(veto.isPresent());
    }

    @Test
    @DisplayName("passes when no allocation is in flight for this player")
    void passesWhenNotAllocating() {
        when(allocationTracker.isAllocating(traveller)).thenReturn(false);

        final Optional<Component> veto = new AllocatingTravelGuard(allocationTracker).veto(traveller, destination);

        assertTrue(veto.isEmpty());
    }
}
