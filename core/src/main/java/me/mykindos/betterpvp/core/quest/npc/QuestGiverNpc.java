package me.mykindos.betterpvp.core.quest.npc;

import me.mykindos.betterpvp.core.scene.SceneObjectFactory;
import me.mykindos.betterpvp.core.scene.npc.NPC;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Entity;

/**
 * A minimal named quest-giver NPC. Interaction is handled by
 * {@link QuestGiverListener} via its binding, so {@code act} stays a no-op.
 */
public class QuestGiverNpc extends NPC {

    private final String displayName;

    public QuestGiverNpc(SceneObjectFactory factory, String displayName) {
        super(factory);
        this.displayName = displayName;
    }

    @Override
    protected void onInit() {
        Entity entity = getEntity();
        entity.customName(Component.text(displayName));
        entity.setCustomNameVisible(true);
    }
}
