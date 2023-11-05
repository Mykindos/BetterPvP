package me.mykindos.betterpvp.progression.tree.fishing.event;

import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.progression.tree.fishing.model.FishingLoot;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;

@Getter
public class PlayerCaughtFishEvent extends ProgressionFishingEvent {
    @Setter
    private FishingLoot loot;
    final FishHook hook;
    final Entity caught;
    @Setter
    private boolean ignoresWeight;

    public PlayerCaughtFishEvent(Player player, FishingLoot loot, FishHook hook, Entity caught) {
        super(player);
        this.loot = loot;
        this.hook = hook;
        this.caught = caught;
    }
}