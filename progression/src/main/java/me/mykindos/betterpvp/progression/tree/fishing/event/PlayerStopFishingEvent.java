package me.mykindos.betterpvp.progression.tree.fishing.event;

import lombok.Getter;
import me.mykindos.betterpvp.progression.tree.fishing.model.FishingLoot;
import me.mykindos.betterpvp.progression.tree.fishing.model.FishingRodType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

@Getter
public class PlayerStopFishingEvent extends ProgressionFishingEvent {

    private final @Nullable FishingRodType rodType;
    private final FishingLoot loot;
    private final FishingResult reason;

    public PlayerStopFishingEvent(Player player, @Nullable FishingRodType rodType, FishingLoot loot, FishingResult reason) {
        super(player);
        this.rodType = rodType;
        this.loot = loot;
        this.reason = reason;
    }

    public enum FishingResult {
        CATCH,
        BAD_ROD,
        EARLY_REEL
    }

}
