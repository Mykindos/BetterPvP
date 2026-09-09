package me.mykindos.betterpvp.clans.world.island;

import me.mykindos.betterpvp.core.world.site.VoyageTiming;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoutingIslandAllocatorTest {

    @Mock
    private IslandHostRouter router;

    @Mock
    private LocalIslandAllocator localAllocator;

    private static IslandTemplate template() {
        return new IslandTemplate("solo", Component.text("Solo"), "islands/solo", Material.GRASS_BLOCK, VoyageTiming.DEFAULT);
    }

    @Test
    @DisplayName("a locally-hosted template delegates to the local allocator")
    void localTemplateDelegatesToLocalAllocator() throws ExecutionException, InterruptedException {
        final IslandTemplate template = template();
        final IslandHandle handle = new IslandHandle(UUID.randomUUID(), template.getKey(), "current-server", "islands/solo/abc12345");

        when(router.isLocal(template)).thenReturn(true);
        when(localAllocator.allocate(template)).thenReturn(CompletableFuture.completedFuture(handle));

        final RoutingIslandAllocator allocator = new RoutingIslandAllocator(router, localAllocator);
        final IslandHandle result = allocator.allocate(template).get();

        assertEquals(handle, result);
        verify(localAllocator).allocate(template);
    }

    @Test
    @DisplayName("a remotely-hosted template never touches the local allocator and fails cleanly")
    void remoteTemplateFailsCleanlyWithoutTouchingLocalAllocator() {
        final IslandTemplate template = template();

        when(router.isLocal(template)).thenReturn(false);
        when(router.hostFor(template)).thenReturn("other-server");

        final RoutingIslandAllocator allocator = new RoutingIslandAllocator(router, localAllocator);

        final ExecutionException thrown = assertThrows(ExecutionException.class, () -> allocator.allocate(template).get());
        assertEquals(UnsupportedOperationException.class, thrown.getCause().getClass());
        verify(localAllocator, never()).allocate(template);
    }
}
