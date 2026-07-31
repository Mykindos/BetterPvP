package me.mykindos.betterpvp.clans.world.island;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IslandAllocationTrackerTest {

    private static Player playerWith(UUID id) {
        final Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(id);
        return player;
    }

    @Test
    @DisplayName("begin returns true once, then false until finish is called")
    void beginThenFinish() {
        final IslandAllocationTracker tracker = new IslandAllocationTracker();
        final Player player = playerWith(UUID.randomUUID());

        assertTrue(tracker.begin(player));
        assertFalse(tracker.begin(player));
        assertTrue(tracker.isAllocating(player));

        tracker.finish(player);

        assertFalse(tracker.isAllocating(player));
        assertTrue(tracker.begin(player));
    }

    @Test
    @DisplayName("begin is independent per player")
    void beginIsPerPlayer() {
        final IslandAllocationTracker tracker = new IslandAllocationTracker();
        final Player a = playerWith(UUID.randomUUID());
        final Player b = playerWith(UUID.randomUUID());

        assertTrue(tracker.begin(a));
        assertTrue(tracker.begin(b));
    }

    @Test
    @DisplayName("N threads racing begin for the same player yield exactly one winner")
    void concurrentBeginHasExactlyOneWinner() throws InterruptedException {
        final IslandAllocationTracker tracker = new IslandAllocationTracker();
        final Player player = playerWith(UUID.randomUUID());

        final int threads = 32;
        final ExecutorService pool = Executors.newFixedThreadPool(threads);
        final CountDownLatch ready = new CountDownLatch(threads);
        final CountDownLatch go = new CountDownLatch(1);
        final AtomicInteger winners = new AtomicInteger();

        try {
            for (int i = 0; i < threads; i++) {
                pool.submit(() -> {
                    ready.countDown();
                    try {
                        go.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    if (tracker.begin(player)) {
                        winners.incrementAndGet();
                    }
                });
            }

            ready.await();
            go.countDown();
            pool.shutdown();
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }

        assertEquals(1, winners.get());
    }
}
