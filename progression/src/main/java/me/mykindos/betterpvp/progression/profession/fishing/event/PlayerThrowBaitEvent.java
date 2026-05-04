package me.mykindos.betterpvp.progression.profession.fishing.event;

import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.progression.profession.fishing.model.Bait;
import org.bukkit.entity.Player;

@Getter
public class PlayerThrowBaitEvent extends ProgressionFishingEvent {

    private final Bait bait;

    /**
     * When false the bait item must not be removed from the player's inventory.
     * Listeners that prevent consumption set this to false; the bait spawner honors it.
     */
    @Setter
    private boolean consumeBait = true;

    public PlayerThrowBaitEvent(Player player, Bait bait) {
        super(player);
        this.bait = bait;
    }
}
