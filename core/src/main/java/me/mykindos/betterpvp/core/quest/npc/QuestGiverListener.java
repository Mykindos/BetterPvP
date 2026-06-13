package me.mykindos.betterpvp.core.quest.npc;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.scene.SceneObjectInteractEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Triggers a quest-giver binding when its NPC is right-clicked. Listens on the shared
 * {@link SceneObjectInteractEvent}, so it fires for both real-entity and packet-only NPCs
 * without binding to entity interaction directly — any scene object can become a quest-giver.
 */
@Singleton
@BPvPListener
public class QuestGiverListener implements Listener {

    private final QuestGiverService questGiverService;

    @Inject
    public QuestGiverListener(QuestGiverService questGiverService) {
        this.questGiverService = questGiverService;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(SceneObjectInteractEvent event) {
        questGiverService.trigger(event.getPlayer(), event.getSceneObject().getId());
    }
}
