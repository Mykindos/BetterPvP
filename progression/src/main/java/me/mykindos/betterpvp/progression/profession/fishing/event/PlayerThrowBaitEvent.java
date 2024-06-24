package me.mykindos.betterpvp.progression.profession.fishing.event;

import lombok.Getter;
import me.mykindos.betterpvp.progression.profession.fishing.model.Bait;
import org.bukkit.entity.Player;

@Getter
public class PlayerThrowBaitEvent extends ProgressionFishingEvent {

    private final Bait bait;

    public PlayerThrowBaitEvent(Player player, Bait bait) {
        super(player);
        this.bait = bait;
    }
}
