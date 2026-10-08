package me.mykindos.betterpvp.core.scene.mob.listener;

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.scene.SceneObjectRegistry;
import me.mykindos.betterpvp.core.scene.controller.SceneMaterializationController;
import me.mykindos.betterpvp.core.scene.mob.MobFixture;
import me.mykindos.betterpvp.core.scene.mob.MobFixture.TestMob;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Mob;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MobLifecycleListenerTest {

    private final MobFixture fixture = new MobFixture();
    private final SceneObjectRegistry registry = MobFixture.registry();
    private final SceneMaterializationController controller = new SceneMaterializationController(mock(Core.class), registry);
    private final MobLifecycleListener listener = listener(registry);
    private final Set<Mob> dead = new HashSet<>();
    private final List<Location> anchors = new ArrayList<>();

    MobLifecycleListenerTest() {
        when(fixture.world.getUID()).thenReturn(UUID.randomUUID());
        when(fixture.world.isChunkLoaded(anyInt(), anyInt())).thenReturn(true);
        when(fixture.world.getChunkAt(anyInt(), anyInt())).thenAnswer(invocation ->
                chunk(invocation.getArgument(0), invocation.getArgument(1)));
    }

    @AfterEach
    void close() {
        fixture.close();
    }

    private static MobLifecycleListener listener(SceneObjectRegistry registry) {
        try {
            final Constructor<MobLifecycleListener> constructor = MobLifecycleListener.class.getDeclaredConstructor(SceneObjectRegistry.class);
            constructor.setAccessible(true);
            return constructor.newInstance(registry);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private Chunk chunk(int x, int z) {
        final Chunk chunk = mock(Chunk.class);
        when(chunk.getX()).thenReturn(x);
        when(chunk.getZ()).thenReturn(z);
        when(chunk.getWorld()).thenReturn(fixture.world);
        when(chunk.isLoaded()).thenReturn(true);
        when(chunk.isEntitiesLoaded()).thenReturn(true);
        return chunk;
    }

    /** A body that dies and fires Paper's removal events the first time it is removed. */
    private Mob body() {
        final Mob body = fixture.newBody();
        when(body.isDead()).thenAnswer(invocation -> dead.contains(body));
        doAnswer(invocation -> {
            lose(body);
            return null;
        }).when(body).remove();
        return body;
    }

    /** The server takes {@code body} away, as a chunk unload, {@code /kill} or a cleanup does. */
    private void lose(Mob body) {
        if (!dead.add(body)) {
            return;
        }
        final EntityRemoveEvent removed = mock(EntityRemoveEvent.class);
        when(removed.getEntity()).thenReturn(body);
        listener.onRemove(removed);
        final EntityRemoveFromWorldEvent left = mock(EntityRemoveFromWorldEvent.class);
        when(left.getEntity()).thenReturn(body);
        controller.onEntityRemove(left);
    }

    /** A chunk-managed mob anchored at the fixture's position, registered and so spawned. */
    private TestMob chunkManaged() {
        final TestMob mob = fixture.create(null, created -> { });
        mob.configureMaterialization(fixture.position(), anchor -> {
            anchors.add(anchor);
            return body();
        });
        registry.register(mob);
        return mob;
    }

    private void assertRespawnedAtHome(TestMob mob, Mob first) {
        assertTrue(mob.isRegistered());
        assertTrue(mob.isMaterialized());
        assertNotSame(first, mob.getEntity());
        assertEquals(2, anchors.size());
        assertEquals(anchors.get(0), anchors.get(1));
    }

    @Test
    void ac2_aChunkManagedMobDespawnedByItsChunkUnloadingStaysRegisteredAndComesBack() {
        final TestMob mob = chunkManaged();
        final Mob first = fixture.body;
        final Chunk home = chunk(0, 0);
        final EntitiesUnloadEvent unload = mock(EntitiesUnloadEvent.class);
        when(unload.getChunk()).thenReturn(home);

        controller.onEntitiesUnload(unload);
        assertFalse(mob.isMaterialized());
        assertTrue(mob.isRegistered());

        final EntitiesLoadEvent load = mock(EntitiesLoadEvent.class);
        when(load.getChunk()).thenReturn(home);
        controller.onEntitiesLoad(load);
        assertRespawnedAtHome(mob, first);
    }

    @Test
    void ac2_aChunkManagedMobWhoseBodyIsUnloadedInAnotherChunkStaysRegisteredAndRespawns() {
        final TestMob mob = chunkManaged();
        final Mob first = fixture.body;
        fixture.moveBodyTo(fixture.at(40.5, 64, 40.5));
        final Chunk elsewhere = chunk(2, 2);
        final EntitiesUnloadEvent unload = mock(EntitiesUnloadEvent.class);
        when(unload.getChunk()).thenReturn(elsewhere);

        controller.onEntitiesUnload(unload);
        lose(first);

        assertRespawnedAtHome(mob, first);
    }

    @Test
    void ac2_aChunkManagedMobWhoseBodyIsRemovedOutrightStaysRegisteredAndRespawns() {
        final TestMob mob = chunkManaged();
        final Mob first = fixture.body;

        lose(first);

        assertRespawnedAtHome(mob, first);
    }

    @Test
    void ac2_onlyRemovingAChunkManagedMobEndsIt() {
        final TestMob mob = chunkManaged();

        mob.remove();

        assertFalse(mob.isRegistered());
        assertEquals(1, anchors.size());
    }

    @Test
    void anEagerMobWhoseBodyIsRemovedOutrightIsRemoved() {
        final TestMob mob = fixture.spawn(created -> { });
        final Mob body = fixture.body;
        registry.register(mob);
        assertSame(body, mob.getEntity());

        lose(body);

        assertFalse(mob.isRegistered());
    }
}
