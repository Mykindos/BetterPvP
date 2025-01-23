package me.mykindos.betterpvp.core.npc.controller;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import me.mykindos.betterpvp.core.framework.events.ServerStartEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.npc.NPCRegistry;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@BPvPListener
@Singleton
public class NPCListener implements Listener {

    @Inject
    private NPCRegistry registry;

    @EventHandler
    public void onStartup(ServerStartEvent event) {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (!entity.getPersistentDataContainer().has(CoreNamespaceKeys.NPC)) {
                    continue;
                }

                entity.remove();
            }
        }
    }

}
