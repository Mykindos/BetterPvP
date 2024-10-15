package me.mykindos.betterpvp.progression.profession.woodcutting.event;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@Getter
public class PlayerChopLogEvent extends ProgressionWoodcuttingEvent {
    private final Material logType;
    private final Block choppedLogBlock;

    /**
     * The tool that the player used to chop the log
     */
    private final ItemStack toolUsed;

    /**
     * This is the number by which the player's xp will be multiplied by.
     * NOT THE ACTUAL XP
     */
    @Setter
    private double experienceBonusModifier = 1.0;

    /**
     * Will be more than one if Tree Feller was activated
     */
    @Setter
    private int amountChopped = 0;

    @Setter
    private int additionalLogsDropped = 0;

    public PlayerChopLogEvent(Player player, Material logType, Block choppedLogBlock, ItemStack toolUsed) {
        super(player);
        this.logType = logType;
        this.choppedLogBlock = choppedLogBlock;
        this.toolUsed = toolUsed;
    }
}
