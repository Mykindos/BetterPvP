package me.mykindos.betterpvp.progression.profession.fishing.event;

import lombok.Getter;
import me.mykindos.betterpvp.core.loot.LootBundle;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

@Getter
public class PlayerStopFishingEvent extends ProgressionFishingEvent {

    @Nullable
    private final LootBundle bundle;
    private final FishingResult reason;

    public PlayerStopFishingEvent(Player player, @Nullable LootBundle bundle, FishingResult reason) {
        super(player);
        this.bundle = bundle;
        this.reason = reason;
    }

    public enum FishingResult {
        CATCH,
        BAD_ROD,
        EARLY_REEL
    }

}
