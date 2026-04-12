package me.mykindos.betterpvp.core.scene.npc;

import me.mykindos.betterpvp.core.scene.SceneObjectFactory;
import me.mykindos.betterpvp.core.scene.SceneObjectRegistry;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

/**
 * Abstract factory for command-spawnable NPC types.
 * <p>
 * Extends {@link SceneObjectFactory} to narrow the spawn contract to {@link NPC}
 * and provide the NPC-specific {@link #spawnNPC(NPC, Entity)} helper that combines
 * {@link NPC#init(Entity)} + registry registration in one call.
 */
public abstract class NPCFactory extends SceneObjectFactory {

    protected NPCFactory(@NotNull String name, @NotNull SceneObjectRegistry registry) {
        super(name, registry);
    }

    /**
     * Initializes {@code npc} with {@code entity} and registers it.
     * Prefer this over calling {@link #spawn(me.mykindos.betterpvp.core.scene.SceneObject, Entity)}
     * directly in NPC factory subclasses, as it communicates intent clearly.
     */
    public <T extends NPC> T spawnNPC(@NotNull T npc, @NotNull Entity entity) {
        return spawn(npc, entity);
    }

}
