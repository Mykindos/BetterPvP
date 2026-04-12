package me.mykindos.betterpvp.core.scene.controller;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.scene.SceneEntity;
import me.mykindos.betterpvp.core.scene.SceneObjectRegistry;
import org.bukkit.event.Listener;

/**
 * Drives per-tick behaviour for all registered {@link SceneEntity} instances.
 * <p>
 * Replaces the old {@code NPCTicker}, which was limited to NPC objects.
 * Any {@link SceneEntity} (NPC or prop) with attached behaviors is ticked here.
 * Entities with no behaviors incur only the cost of an empty list iteration.
 */
@BPvPListener
@Singleton
public class SceneTicker implements Listener {

    private final SceneObjectRegistry registry;

    @Inject
    private SceneTicker(SceneObjectRegistry registry) {
        this.registry = registry;
    }

    @UpdateEvent
    public void onUpdate() {
        for (SceneEntity entity : registry.getObjects(SceneEntity.class)) {
            entity.tick();
        }
    }

}
