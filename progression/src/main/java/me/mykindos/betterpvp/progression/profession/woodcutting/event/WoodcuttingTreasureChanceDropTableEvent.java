package me.mykindos.betterpvp.progression.profession.woodcutting.event;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Event fired when a player is about to receive a treasure drop from woodcutting.
 * Allows modifying the chance of the treasure drop and the loot table used.
 */
@Getter
@Setter
public class WoodcuttingTreasureChanceDropTableEvent extends ProgressionWoodcuttingEvent {

    private final Location location;
    private double treasureChance;
    private String lootTableId = "woodcutting_treasure_chance";

    public WoodcuttingTreasureChanceDropTableEvent(Player player, Location location, double treasureChance) {
        super(player);
        this.location = location;
        this.treasureChance = treasureChance;
    }
}
