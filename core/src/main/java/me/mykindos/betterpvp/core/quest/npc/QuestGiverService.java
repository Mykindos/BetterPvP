package me.mykindos.betterpvp.core.quest.npc;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.quest.QuestManager;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which scene objects are quest NPCs (by their {@code quest_npcs.id}) so "Talk to NPC"
 * objectives can match them. Keyed by the live
 * {@link me.mykindos.betterpvp.core.scene.SceneObject} id — bindings are applied by whoever
 * spawns the NPC (the scene loader today) and live as long as that NPC does, mirroring how scene
 * objects are recreated each load. The NPC carries no content: a quest declares its own entry NPC
 * via its root {@code trigger.npc_interact} objective and auto-starts when that interaction is recorded.
 */
@Singleton
public class QuestGiverService {

    private final QuestManager questManager;
    private final Map<Integer, QuestGiverBinding> bindings = new ConcurrentHashMap<>();

    @Inject
    public QuestGiverService(QuestManager questManager) {
        this.questManager = questManager;
    }

    public void bind(int sceneObjectId, QuestGiverBinding binding) {
        bindings.put(sceneObjectId, binding);
    }

    public void unbind(int sceneObjectId) {
        bindings.remove(sceneObjectId);
    }

    public Optional<QuestGiverBinding> get(int sceneObjectId) {
        return Optional.ofNullable(bindings.get(sceneObjectId));
    }

    /** Fire the interact trigger for this NPC, if it is a quest NPC. Called on interaction. */
    public void trigger(Player player, int sceneObjectId) {
        QuestGiverBinding binding = bindings.get(sceneObjectId);
        if (binding == null) return;

        // Advance any active "Talk to NPC" objectives that target this NPC, and auto-start any quest
        // whose root stage opens on this NPC (handled inside recordEvent). Mirrors
        // QuestListener#matchValue: an objective with no npc param is a wildcard.
        final String npcId = binding.getNpcId();
        questManager.recordEvent(player, "trigger.npc_interact", data -> {
            String configured = data.getString("npc");
            return configured == null || configured.isBlank() || configured.equalsIgnoreCase(npcId);
        }, 1);
    }
}
