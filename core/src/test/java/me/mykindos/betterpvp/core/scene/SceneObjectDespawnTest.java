package me.mykindos.betterpvp.core.scene;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SceneObjectDespawnTest {

    private final List<Boolean> despawningWhenBodyRemoved = new ArrayList<>();
    private TestObject object;

    @BeforeEach
    void setUp() {
        object = new TestObject();
        object.configureMaterialization(mock(Location.class), anchor -> body());
        object.materialize();
    }

    @Test
    void aChunkUnloadIsReportedAsTheFrameworkDespawning() {
        object.dematerialize();

        assertEquals(List.of(true), despawningWhenBodyRemoved);
        assertFalse(object.isDespawning());
    }

    @Test
    void removalIsReportedAsTheFrameworkDespawning() {
        object.remove();

        assertEquals(List.of(true), despawningWhenBodyRemoved);
        assertFalse(object.isDespawning());
    }

    @Test
    void theFlagIsClearedEvenIfTeardownFails() {
        object.failOnDematerialize = true;

        try {
            object.dematerialize();
        } catch (IllegalStateException ignored) {
            // expected
        }

        assertFalse(object.isDespawning());
    }

    @Test
    void theFlagIsOffWhileTheObjectIsStanding() {
        assertTrue(object.isMaterialized());
        assertFalse(object.isDespawning());
    }

    @Test
    void aPersistentIdIsKept() {
        final UUID id = UUID.randomUUID();
        final TestObject fresh = new TestObject();
        fresh.setPersistentId(id);

        assertEquals(id, fresh.getPersistentId());
    }

    private Entity body() {
        final Entity entity = mock(Entity.class);
        when(entity.getPersistentDataContainer()).thenReturn(mock(PersistentDataContainer.class));
        when(entity.getUniqueId()).thenReturn(UUID.randomUUID());
        doAnswer(invocation -> {
            despawningWhenBodyRemoved.add(object.isDespawning());
            return null;
        }).when(entity).remove();
        return entity;
    }

    private static final class TestObject extends SceneObject {

        private boolean failOnDematerialize;

        @Override
        protected void onDematerialize() {
            if (failOnDematerialize) {
                throw new IllegalStateException("teardown failed");
            }
        }
    }
}
