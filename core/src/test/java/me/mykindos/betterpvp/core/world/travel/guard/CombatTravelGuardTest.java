package me.mykindos.betterpvp.core.world.travel.guard;

import me.mykindos.betterpvp.core.world.travel.Destination;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.utilities.search.SearchEngineBase;
import net.kyori.adventure.text.Component;
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
class CombatTravelGuardTest {

    @Mock
    private ClientManager clientManager;

    @Mock
    private Player traveller;

    @Mock
    private Destination destination;

    @SuppressWarnings("unchecked")
    private SearchEngineBase<Client> search() {
        final SearchEngineBase<Client> search = mock(SearchEngineBase.class);
        when(clientManager.search()).thenReturn(search);
        return search;
    }

    @Test
    @DisplayName("vetoes travel while the player is flagged in combat")
    void vetoesWhileInCombat() {
        final SearchEngineBase<Client> search = search();
        final Client client = mock(Client.class);
        final Gamer gamer = mock(Gamer.class);
        when(search.online(traveller)).thenReturn(client);
        when(client.getGamer()).thenReturn(gamer);
        when(gamer.isInCombat()).thenReturn(true);

        final Optional<Component> veto = new CombatTravelGuard(clientManager).veto(traveller, destination);

        assertTrue(veto.isPresent());
    }

    @Test
    @DisplayName("passes when the player is not in combat")
    void passesWhenNotInCombat() {
        final SearchEngineBase<Client> search = search();
        final Client client = mock(Client.class);
        final Gamer gamer = mock(Gamer.class);
        when(search.online(traveller)).thenReturn(client);
        when(client.getGamer()).thenReturn(gamer);
        when(gamer.isInCombat()).thenReturn(false);

        final Optional<Component> veto = new CombatTravelGuard(clientManager).veto(traveller, destination);

        assertTrue(veto.isEmpty());
    }
}
