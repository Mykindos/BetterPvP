package me.mykindos.betterpvp.core.block;

import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a custom block type with specific behaviors and properties.
 */
public abstract class SmartBlock {

    private final String id;
    private final String name;

    protected SmartBlock(@NotNull String id, @NotNull String name) {
        this.id = id;
        this.name = name;
    }

    /**
     * Handles a click action on the block instance by a player.
     * @param blockInstance the instance of the block being clicked
     * @param player the player who clicked the block
     * @param action the action performed by the player (e.g., right-click, left-click)
     * @return true if the action was handled, false otherwise
     */
    public boolean handleClick(@NotNull SmartBlockInstance blockInstance, @NotNull Player player, @NotNull Action action) {
        // ignore
        return false;
    }

    /**
     * @return the name of the block
     */
    public String getName() {
        return name;
    }

    /**
     * @return the namespaced key of the block
     */
    public String getKey() {
        return id;
    }

}
