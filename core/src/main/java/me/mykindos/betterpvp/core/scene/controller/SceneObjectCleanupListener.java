package me.mykindos.betterpvp.core.scene.controller;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import me.mykindos.betterpvp.core.framework.events.ServerStartEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Removes leftover scene-object entities.
 * <p>
 * Scene objects are marked with {@link CoreNamespaceKeys#SCENE_OBJECT} in their PDC and are non-persistent, so a live
 * body is never read back from disk. Any marked entity that is (one saved by another plugin, or left by a crash) is a
 * straggler, and is removed before new objects are registered. Chunks are swept as their entities load, which covers
 * worlds that open long after startup, and everything already loaded is swept once at startup.
 */
@BPvPListener
@Singleton
public class SceneObjectCleanupListener implements Listener {

    // Lowest, so it runs before world content installs anything on the same event and cannot sweep up fresh bodies.
    @EventHandler(priority = EventPriority.LOWEST)
    public void onStartup(ServerStartEvent event) {
        for (World world : Bukkit.getWorlds()) {
            world.getEntities().forEach(this::removeIfLeftover);
        }
    }

    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        event.getEntities().forEach(this::removeIfLeftover);
    }

    private void removeIfLeftover(@NotNull Entity entity) {
        if (entity.getPersistentDataContainer().has(CoreNamespaceKeys.SCENE_OBJECT)) {
            entity.remove();
        }
    }
}
