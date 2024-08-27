package me.mykindos.betterpvp.progression.profession.woodcutting.event;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.Player;

@Getter
public class PlayerUsesTreeFellerEvent extends ProgressionWoodcuttingEvent {

    /**
     * Specifically for Enchanted Lumberfall; this is the location where that perk will activate
     */
    private final Location locationToActivatePerk;

    public PlayerUsesTreeFellerEvent(Player player, Location locationToActivatePerk) {
        super(player);
        this.locationToActivatePerk = locationToActivatePerk;
    }
}
