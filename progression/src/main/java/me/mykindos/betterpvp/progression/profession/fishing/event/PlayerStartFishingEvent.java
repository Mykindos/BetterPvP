package me.mykindos.betterpvp.progression.profession.fishing.event;

import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.progression.profession.fishing.model.FishingLoot;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;

@Getter
@Setter
public class PlayerStartFishingEvent extends ProgressionFishingEvent {

    private final FishingLoot boundLoot;
    private final FishHook hook;

    public PlayerStartFishingEvent(Player player, FishingLoot boundLoot, FishHook hook) {
        super(player);
        this.boundLoot = boundLoot;
        this.hook = hook;
    }
}
