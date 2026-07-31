package me.mykindos.betterpvp.clans.world.travel.guard;

import me.mykindos.betterpvp.clans.world.island.IslandInstance;
import me.mykindos.betterpvp.clans.world.island.IslandInstanceManager;
import me.mykindos.betterpvp.clans.world.island.IslandOffer;
import me.mykindos.betterpvp.clans.world.travel.Destination;
import net.kyori.adventure.text.Component;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlreadyOnIslandTravelGuardTest {

    @Mock
    private IslandInstanceManager instanceManager;

    @Mock
    private Player traveller;

    // Mocked rather than constructed: IslandOffer's real constructor builds an ItemView, which forces
    // ItemProvider's static initializer to create a live ItemStack - that needs a running Bukkit server. Mocking
    // bypasses the constructor entirely while still being `instanceof IslandOffer` for the guard under test.
    private static IslandOffer islandOffer() {
        return mock(IslandOffer.class);
    }

    @Test
    @DisplayName("vetoes travel to an island offer while already standing on an instance")
    void vetoesWhenAlreadyOnIsland() {
        final World world = mock(World.class);
        when(world.getName()).thenReturn("islands/solo/abc12345");
        when(traveller.getWorld()).thenReturn(world);
        when(instanceManager.byWorld("islands/solo/abc12345")).thenReturn(Optional.of(mock(IslandInstance.class)));

        final Optional<Component> veto = new AlreadyOnIslandTravelGuard(instanceManager).veto(traveller, islandOffer());

        assertTrue(veto.isPresent());
    }

    @Test
    @DisplayName("passes when the destination is not an island offer, regardless of current world")
    void passesWhenDestinationIsNotAnIslandOffer() {
        final Destination other = mock(Destination.class);

        final Optional<Component> veto = new AlreadyOnIslandTravelGuard(instanceManager).veto(traveller, other);

        assertTrue(veto.isEmpty());
    }

    @Test
    @DisplayName("passes for an island offer when the player is not currently on any island")
    void passesWhenNotOnAnyIsland() {
        final World world = mock(World.class);
        when(world.getName()).thenReturn("world");
        when(traveller.getWorld()).thenReturn(world);
        when(instanceManager.byWorld("world")).thenReturn(Optional.empty());

        final Optional<Component> veto = new AlreadyOnIslandTravelGuard(instanceManager).veto(traveller, islandOffer());

        assertTrue(veto.isEmpty());
    }
}
