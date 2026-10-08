package me.mykindos.betterpvp.clans.world.camp.settler.recruit;

import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CampCoinsTest {

    private final ClientManager clientManager = mock(ClientManager.class, RETURNS_DEEP_STUBS);
    private final Gamer gamer = mock(Gamer.class);
    private final Player player = mock(Player.class);
    private final CampCoins coins = new CampCoins(clientManager);

    @BeforeEach
    void setUp() {
        when(clientManager.search().online(player).getGamer()).thenReturn(gamer);
        when(gamer.getBalance()).thenReturn(1_000);
    }

    @Test
    void ac25_takingNothingAlwaysSucceedsAndTakesNothing() {
        assertTrue(coins.take(player, 0));
        assertTrue(coins.take(player, -5));
        verify(gamer, never()).saveProperty(any(GamerProperty.class), any());
    }

    @Test
    void ac25_takingMoreThanTheyHaveFailsAndTakesNothing() {
        assertFalse(coins.take(player, 1_001));
        verify(gamer, never()).saveProperty(any(GamerProperty.class), any());
    }

    @Test
    void ac25_takingWhatTheyHaveTakesIt() {
        assertTrue(coins.take(player, 300));
        verify(gamer).saveProperty(GamerProperty.BALANCE, 700);
    }

    @Test
    void ac25_givingHandsCoinsBackAndNothingForNothing() {
        coins.give(player, 0);
        coins.give(player, -1);
        verify(gamer, never()).saveProperty(any(GamerProperty.class), any());

        coins.give(player, 250);
        verify(gamer).saveProperty(GamerProperty.BALANCE, 1_250);
    }
}
