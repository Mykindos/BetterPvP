package me.mykindos.betterpvp.clans.world.island;

import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.server.Realm;
import me.mykindos.betterpvp.core.server.Season;
import me.mykindos.betterpvp.core.server.Server;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoutingTravelTransportTest {

    private static final String CURRENT_SERVER = "current-server";

    @Mock
    private LocalTravelTransport localTransport;

    @Mock
    private Player traveller;

    private Realm originalRealm;

    @BeforeEach
    void stubCurrentServer() {
        originalRealm = Core.getCurrentRealm();
        Core.setCurrentRealm(new Realm(1, new Server(1, CURRENT_SERVER), new Season(1, "test-season", LocalDate.now())));
    }

    @AfterEach
    void restoreCurrentServer() {
        Core.setCurrentRealm(originalRealm);
    }

    @Test
    @DisplayName("a handle naming this server delegates to the local transport")
    void handleForCurrentServerDelegatesToLocalTransport() throws ExecutionException, InterruptedException {
        final IslandHandle handle = new IslandHandle(UUID.randomUUID(), "solo", CURRENT_SERVER, "islands/solo/abc12345");
        when(localTransport.deliver(traveller, handle)).thenReturn(CompletableFuture.completedFuture(true));

        final RoutingTravelTransport transport = new RoutingTravelTransport(localTransport);

        assertTrue(transport.deliver(traveller, handle).get());
        verify(localTransport).deliver(traveller, handle);
    }

    @Test
    @DisplayName("a handle naming another server never touches the local transport and fails cleanly")
    void handleForForeignServerFailsCleanlyWithoutTouchingLocalTransport() {
        final IslandHandle handle = new IslandHandle(UUID.randomUUID(), "solo", "other-server", "islands/solo/abc12345");

        final RoutingTravelTransport transport = new RoutingTravelTransport(localTransport);

        final ExecutionException thrown = assertThrows(ExecutionException.class, () -> transport.deliver(traveller, handle).get());
        assertEquals(UnsupportedOperationException.class, thrown.getCause().getClass());
        verify(localTransport, never()).deliver(traveller, handle);
    }
}
