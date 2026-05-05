package me.mykindos.betterpvp.progression.profession.fishing.event;

import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.loot.LootBundle;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;

@Getter
@Setter
public class PlayerStartFishingEvent extends ProgressionFishingEvent {

    private final LootBundle bundle;
    private final FishHook hook;

    public PlayerStartFishingEvent(Player player, LootBundle bundle, FishHook hook) {
        super(player);
        this.bundle = bundle;
        this.hook = hook;
    }
}
