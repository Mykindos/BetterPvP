package me.mykindos.betterpvp.progression.tree.fishing.event;

import lombok.Getter;
import me.mykindos.betterpvp.progression.tree.fishing.model.FishingLoot;
import org.bukkit.entity.Player;

@Getter
public class PlayerStopFishingEvent extends ProgressionFishingEvent {

    private final FishingLoot loot;
    private final FishingResult reason;

    public PlayerStopFishingEvent(Player player, FishingLoot loot, FishingResult reason) {
        super(player);
        this.loot = loot;
        this.reason = reason;
    }

    public enum FishingResult {
        CATCH,
        BAD_ROD,
        EARLY_REEL
    }

}
