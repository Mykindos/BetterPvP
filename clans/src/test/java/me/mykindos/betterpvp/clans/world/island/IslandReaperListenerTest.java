package me.mykindos.betterpvp.clans.world.island;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IslandReaperListenerTest {

    private static final long GRACE_PERIOD_MS = 120_000L;

    private static IslandInstance instance() {
        final IslandTemplate template = new IslandTemplate("solo", Component.text("Solo"), "islands/solo", Material.GRASS_BLOCK);
        return new IslandInstance(UUID.randomUUID(), template, "islands/solo/abc12345");
    }

    @Test
    @DisplayName("does not reap an instance that still has occupants, no matter how long it has been vacant")
    void neverReapsOccupiedInstance() {
        final IslandInstance instance = instance();
        instance.setState(IslandInstanceState.READY);
        instance.addOccupant(UUID.randomUUID());
        instance.setLastVacatedAt(0L);

        assertFalse(IslandReaperListener.shouldReap(instance, GRACE_PERIOD_MS * 10, GRACE_PERIOD_MS));
    }

    @Test
    @DisplayName("does not reap an empty instance that is not READY")
    void neverReapsNonReadyInstance() {
        final IslandInstance instance = instance();
        instance.setState(IslandInstanceState.POOLED);
        instance.setLastVacatedAt(0L);

        assertFalse(IslandReaperListener.shouldReap(instance, GRACE_PERIOD_MS * 10, GRACE_PERIOD_MS));
    }

    @Test
    @DisplayName("does not reap an empty READY instance before the grace period elapses")
    void doesNotReapBeforeGracePeriod() {
        final IslandInstance instance = instance();
        instance.setState(IslandInstanceState.READY);
        instance.setLastVacatedAt(1_000L);

        assertFalse(IslandReaperListener.shouldReap(instance, 1_000L + GRACE_PERIOD_MS - 1, GRACE_PERIOD_MS));
    }

    @Test
    @DisplayName("reaps an empty READY instance once the grace period has elapsed")
    void reapsAfterGracePeriod() {
        final IslandInstance instance = instance();
        instance.setState(IslandInstanceState.READY);
        instance.setLastVacatedAt(1_000L);

        assertTrue(IslandReaperListener.shouldReap(instance, 1_000L + GRACE_PERIOD_MS, GRACE_PERIOD_MS));
    }
}
