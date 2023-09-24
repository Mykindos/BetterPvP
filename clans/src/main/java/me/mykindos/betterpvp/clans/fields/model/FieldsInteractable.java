package me.mykindos.betterpvp.clans.fields.model;

import me.mykindos.betterpvp.clans.clans.events.TerritoryInteractEvent;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a block on fields you can interact with. Place, break, or click.
 */
public interface FieldsInteractable {

    /**
     * The name of the ore.
     * @return the name
     */
    String getName();

    /**
     * Processes an interaction with the block.
     * @param event the event
     * @param block the block
     */
    boolean processInteraction(TerritoryInteractEvent event, FieldsBlock block);

    /**
     * The ore type.
     */
    @NotNull BlockData getType();

    /**
     * Checks if the block data matches the block type.
     * @param data the block data
     * @return true if it matches
     */
    default boolean matches(Block block) {
        return block.getType().equals(getType().getMaterial());
    }

    /**
     * The replacement block when the ore is mined.
     */
    @NotNull BlockData getReplacement();


    /**
     * The delay before the ore respawns, in seconds.
     */
    double getRespawnDelay();

    /**
     * Sets the respawn delay.
     * @param delay the delay
     */
    void setRespawnDelay(double delay);

    /**
     * Reloads the ore from the config.
     */
    default void loadConfig(ExtendedYamlConfiguration config) {

    }

}
