package me.mykindos.betterpvp.core.npc;

import lombok.Getter;
import me.mykindos.betterpvp.core.npc.model.NPC;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

/**
 * Factory for creating NPCs
 */
@Getter
public abstract class NPCFactory {

    private final String name;
    protected final NPCRegistry registry;

    protected NPCFactory(String name, NPCRegistry registry) {
        this.name = name;
        this.registry = registry;
    }

    /**
     * Spawns a default NPC
     *
     * @param location Location to spawn the NPC
     * @param name     Name of the NPC
     * @return The NPC
     */
    public abstract NPC spawnDefault(final @NotNull Location location, @NotNull String name);

}
