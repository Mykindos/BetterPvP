package me.mykindos.betterpvp.progression.profession.woodcutting.event;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

@Getter
public class PlayerUsesTreeFellerEvent extends ProgressionWoodcuttingEvent {

    /**
     * Specifically for Enchanted Lumberfall; this is the location where that perk will activate
     */
    private final Location locationToActivatePerk;

    private final Location initialChoppedLogLocation;

    private final Material initialChoppedLogType;

    public PlayerUsesTreeFellerEvent(@NotNull Player player,
                                     @Nullable Location locationToActivatePerk,
                                     @NotNull Location initialChoppedLogLocation,
                                     @NotNull Material initialChoppedLogType) {
        super(player);
        this.locationToActivatePerk = locationToActivatePerk;
        this.initialChoppedLogLocation = initialChoppedLogLocation;
        this.initialChoppedLogType = initialChoppedLogType;
    }
}
