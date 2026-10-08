package me.mykindos.betterpvp.core.scene.mob.listener;

import com.github.retrooper.packetevents.PacketEvents;
import me.mykindos.betterpvp.core.scene.SceneObjectRegistry;
import me.mykindos.betterpvp.core.scene.mob.MobFixture;
import me.mykindos.betterpvp.core.scene.mob.MobFixture.TestMob;
import org.bukkit.entity.Mob;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Constructor;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class MobLifecycleListenerTest {

    private final MobFixture fixture = new MobFixture();
    private final SceneObjectRegistry registry = construct(SceneObjectRegistry.class);
    private final MobLifecycleListener listener = construct(MobLifecycleListener.class, registry);

    @AfterEach
    void close() {
        fixture.close();
    }

    private static <T> T construct(Class<T> type, Object... arguments) {
        try (MockedStatic<PacketEvents> ignored = mockStatic(PacketEvents.class, RETURNS_DEEP_STUBS)) {
            final Class<?>[] parameters = new Class<?>[arguments.length];
            for (int i = 0; i < arguments.length; i++) {
                parameters[i] = arguments[i].getClass();
            }
            final Constructor<T> constructor = type.getDeclaredConstructor(parameters);
            constructor.setAccessible(true);
            return constructor.newInstance(arguments);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    /** A registered chunk-managed mob whose body fires the remove event the first time it is removed, as Paper does. */
    private TestMob registeredMob() {
        final TestMob mob = fixture.spawnChunkManaged(null, created -> { });
        final Mob body = fixture.body;
        final AtomicBoolean removed = new AtomicBoolean();
        doAnswer(invocation -> {
            if (!removed.getAndSet(true)) {
                listener.onRemove(removal(body));
            }
            return null;
        }).when(body).remove();
        registry.register(mob);
        return mob;
    }

    private static EntityRemoveEvent removal(Mob body) {
        final EntityRemoveEvent event = mock(EntityRemoveEvent.class);
        when(event.getEntity()).thenReturn(body);
        return event;
    }

    @Test
    void ac2_aChunkManagedMobDespawnedByItsChunkUnloadingStaysRegistered() {
        final TestMob mob = registeredMob();

        mob.dematerialize();

        assertFalse(mob.isMaterialized());
        assertTrue(mob.isRegistered());
    }

    @Test
    void ac2_aMobWhoseBodyIsRemovedOutrightIsRemoved() {
        final TestMob mob = registeredMob();

        listener.onRemove(removal(fixture.body));

        assertFalse(mob.isRegistered());
    }
}
