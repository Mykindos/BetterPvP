package me.mykindos.betterpvp.core.scene.controller;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import me.mykindos.betterpvp.core.framework.events.ServerStartEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Removes any leftover scene-object entities on server startup.
 * <p>
 * Replaces the old {@code NPCListener}. Because scene objects are marked with
 * {@link CoreNamespaceKeys#SCENE_OBJECT} in their PDC (and are non-persistent),
 * any entity carrying that key that survived a crash or reload must be cleaned up
 * before new objects are registered.
 */
@BPvPListener
@Singleton
public class SceneObjectCleanupListener implements Listener {

    @EventHandler
    public void onStartup(ServerStartEvent event) {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (!entity.getPersistentDataContainer().has(CoreNamespaceKeys.SCENE_OBJECT)) {
                    continue;
                }
                entity.remove();
            }
        }
    }

}
