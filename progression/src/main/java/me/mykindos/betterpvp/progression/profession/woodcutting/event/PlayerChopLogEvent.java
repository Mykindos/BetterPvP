package me.mykindos.betterpvp.progression.profession.woodcutting.event;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;
import org.bukkit.entity.Player;

@Getter
public class PlayerChopLogEvent extends ProgressionWoodcuttingEvent {
    private final Material logType;

    /**
     * This is the number by which the player's xp will be multiplied by.
     * NOT THE ACTUAL XP
     */
    @Setter
    private double experienceBonusModifier = 1.0;

    public PlayerChopLogEvent(Player player, Material logType) {
        super(player);
        this.logType = logType;
    }
}
