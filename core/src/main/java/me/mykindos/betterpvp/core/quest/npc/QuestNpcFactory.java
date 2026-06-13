package me.mykindos.betterpvp.core.quest.npc;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.scene.SceneObject;
import me.mykindos.betterpvp.core.scene.SceneObjectFactory;
import me.mykindos.betterpvp.core.scene.SceneObjectRegistry;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Villager;
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

    /** A still, invulnerable, persistent-off villager to back a quest-giver NPC. */
    public static Entity backingEntity(@NotNull Location location) {
        return backingEntity(location, false);
    }

    /**
     * Backing villager for a quest NPC. Pass {@code ai=true} for a moving
     * companion (it then has a pathfinder); {@code false} for a static giver.
     */
    public static Entity backingEntity(@NotNull Location location, boolean ai) {
        return location.getWorld().spawn(location, Villager.class, villager -> {
            villager.setAI(ai);
            villager.setInvulnerable(true);
            villager.setCollidable(false);
            villager.setPersistent(false);
            villager.setSilent(true);
        });
    }
}
