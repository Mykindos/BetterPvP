package me.mykindos.betterpvp.progression.tree.fishing.event;

import lombok.Getter;
import me.mykindos.betterpvp.progression.tree.fishing.model.FishingLoot;
import org.bukkit.entity.Player;

@Getter
public class PlayerStartFishingEvent extends ProgressionFishingEvent {

    private final FishingLoot boundLoot;

    public PlayerStartFishingEvent(Player player, FishingLoot boundLoot) {
        super(player);
        this.boundLoot = boundLoot;
    }

}
