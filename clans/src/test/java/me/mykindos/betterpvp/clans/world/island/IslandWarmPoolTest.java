package me.mykindos.betterpvp.clans.world.island;

import me.mykindos.betterpvp.core.world.site.VoyageTiming;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IslandWarmPoolTest {

    @Mock
    private IslandWorldProvisioner provisioner;

    @Mock
    private IslandTemplateRegistry templateRegistry;

    @Mock
    private IslandInstanceManager instanceManager;

    @Mock
    private IslandInstanceRepository repository;

    @Mock
    private IslandBootRecovery bootRecovery;

    @Mock
    private IslandHostRouter router;

    private IslandWarmPool warmPool;

    private static IslandTemplate template(String key) {
        return new IslandTemplate(key, Component.text(key), "islands/" + key, Material.GRASS_BLOCK, VoyageTiming.DEFAULT);
    }

    @BeforeEach
    void setUp() throws NoSuchFieldException, IllegalAccessException {
        warmPool = new IslandWarmPool(provisioner, templateRegistry, instanceManager, repository, bootRecovery, router);
        final Field sizeField = IslandWarmPool.class.getDeclaredField("sizePerTemplate");
        sizeField.setAccessible(true);
        sizeField.set(warmPool, 2);
    }

    private static IslandInstance pooledInstance(IslandTemplate template) {
        final IslandInstance instance = new IslandInstance(UUID.randomUUID(), template, "islands/" + template.getKey() + "/abc12345");
        instance.setState(IslandInstanceState.POOLED);
        return instance;
    }

    @Test
    @DisplayName("claim hands a pooled instance to exactly one of two concurrent claimers")
    void claimHandsInstanceToExactlyOneConcurrentClaimer() throws Exception {
        final IslandTemplate template = template("solo");
        final Field poolsField = IslandWarmPool.class.getDeclaredField("pools");
        poolsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        final java.util.Map<String, java.util.Deque<IslandInstance>> pools =
                (java.util.Map<String, java.util.Deque<IslandInstance>>) poolsField.get(warmPool);
        final java.util.Deque<IslandInstance> deque = new java.util.concurrent.ConcurrentLinkedDeque<>();
        final IslandInstance instance = pooledInstance(template);
        deque.add(instance);
        pools.put(template.getKey(), deque);

        final ExecutorService pool = Executors.newFixedThreadPool(2);
        final CountDownLatch ready = new CountDownLatch(2);
        final CountDownLatch go = new CountDownLatch(1);
        final AtomicInteger winners = new AtomicInteger();

        try {
            for (int i = 0; i < 2; i++) {
                pool.submit(() -> {
                    ready.countDown();
                    try {
                        go.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    if (warmPool.claim(template).isPresent()) {
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

    @Test
    @DisplayName("claim on an empty pool returns empty")
    void claimOnEmptyPoolReturnsEmpty() {
        final IslandTemplate template = template("solo");
        assertTrue(warmPool.claim(template).isEmpty());
    }

    @Test
    @DisplayName("refill provisions for local templates and skips non-local ones")
    void refillSkipsNonLocalTemplates() {
        final IslandTemplate local = template("solo");
        final IslandTemplate remote = template("duo");
        when(templateRegistry.all()).thenReturn(List.of(local, remote));
        when(router.isLocal(local)).thenReturn(true);
        when(router.isLocal(remote)).thenReturn(false);

        when(provisioner.provision(eq(local), any(UUID.class))).thenReturn(new CompletableFuture<>());

        warmPool.refill();

        verify(provisioner).provision(eq(local), any(UUID.class));
        verify(provisioner, never()).provision(eq(remote), any(UUID.class));
    }
}
