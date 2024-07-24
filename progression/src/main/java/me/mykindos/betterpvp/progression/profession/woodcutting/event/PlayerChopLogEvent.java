package me.mykindos.betterpvp.progression.profession.woodcutting.event;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

@Getter
public class PlayerChopLogEvent extends ProgressionWoodcuttingEvent {
    private final Material logType;
    private final Block choppedLogBlock;

    /**
     * This is the number by which the player's xp will be multiplied by.
     * NOT THE ACTUAL XP
     */
    @Setter
    private double experienceBonusModifier = 1.0;

    @Setter
    private int amountChopped = 0;

    @Setter
    private int additionalLogsDropped = 0;

    public PlayerChopLogEvent(Player player, Material logType, Block choppedLogBlock) {
        super(player);
        this.logType = logType;
        this.choppedLogBlock = choppedLogBlock;
    }
}
