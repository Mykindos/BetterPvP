package me.mykindos.betterpvp.progression.profession.fishing.event;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Event fired when a player is about to receive a treasure drop from fishing.
 * Allows modifying the chance of the treasure drop.
 */
@Getter
@Setter
public class FishingTreasureChanceDropTableEvent extends ProgressionFishingEvent {

    private final Location location;
    private double treasureChance;
    private String lootTableId = "fishing_treasure_chance";

    public FishingTreasureChanceDropTableEvent(Player player, Location location, double treasureChance) {
        super(player);
        this.location = location;
        this.treasureChance = treasureChance;
    }
}
