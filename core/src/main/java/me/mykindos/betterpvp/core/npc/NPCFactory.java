package me.mykindos.betterpvp.core.npc;

import lombok.Getter;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Factory for creating NPCs
 */
@Getter
public abstract class NPCFactory {

    protected final @NotNull NPCRegistry registry;

    protected NPCFactory(@NotNull NPCRegistry registry) {
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

    /**
     * Spawns an NPC with options
     *
     * @param location Location to spawn the NPC
     * @param name     Name of the NPC
     * @param options  Options to apply to the NPC. Only options that are supported by the NPC type are applied
     * @return The NPC
     */
    public abstract NPC spawnNPC(final @NotNull Location location, @NotNull String name, @NotNull Collection<@NotNull Enum<?>> options);

}
