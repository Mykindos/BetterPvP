package me.mykindos.betterpvp.progression.tree.fishing.event;

import lombok.Getter;
import me.mykindos.betterpvp.progression.tree.fishing.model.Bait;
import org.bukkit.entity.Player;

@Getter
public class PlayerThrowBaitEvent extends ProgressionFishingEvent {

    private final Bait bait;

    public PlayerThrowBaitEvent(Player player, Bait bait) {
        super(player);
        this.bait = bait;
    }
}
