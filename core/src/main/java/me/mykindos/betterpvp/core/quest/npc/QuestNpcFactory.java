package me.mykindos.betterpvp.core.quest.npc;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.scene.SceneObject;
import me.mykindos.betterpvp.core.scene.SceneObjectFactory;
import me.mykindos.betterpvp.core.scene.SceneObjectRegistry;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

/**
 * Spawns generic quest-giver NPCs. Backed by a visible, AI-less villager so it
 * stands still and shows a nameplate. Also command-spawnable via {@code /npc}.
 */
@Singleton
public class QuestNpcFactory extends SceneObjectFactory {

    @Inject
    public QuestNpcFactory(SceneObjectRegistry registry) {
        super("quest", registry);
    }

    @Override
    public String[] getTypes() {
        return new String[]{"giver"};
    }

    @Override
    public SceneObject spawnDefault(@NotNull Location location, @NotNull String type) {
        return spawn(new QuestGiverNpc(this, "NPC"), backingEntity(location));
    }
}
