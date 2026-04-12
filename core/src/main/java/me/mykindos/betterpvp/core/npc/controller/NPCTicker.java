package me.mykindos.betterpvp.core.npc.controller;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.npc.NPCRegistry;
import me.mykindos.betterpvp.core.npc.model.NPC;
import org.bukkit.event.Listener;

/**
 * Drives per-tick behaviour for all registered NPCs.
 * <p>
 * Iterates {@link NPCRegistry} every server tick and calls {@link NPC#tick()} on each entry.
 * NPCs with no attached behaviours incur only the cost of an empty list iteration.
 */
@BPvPListener
@Singleton
public class NPCTicker implements Listener {

    private final NPCRegistry registry;

    @Inject
    private NPCTicker(NPCRegistry registry) {
        this.registry = registry;
    }

    @UpdateEvent
    public void onUpdate() {
        for (NPC npc : registry.getNPCs()) {
            npc.tick();
        }
    }

}
