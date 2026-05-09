package me.mykindos.betterpvp.progression.profession.fishing.event;

import lombok.Getter;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;
import me.mykindos.betterpvp.core.loot.LootBundle;
import org.bukkit.Location;
import org.bukkit.entity.Player;

@Getter
public class PlayerFishingTreasureDropEvent extends CustomCancellableEvent {

    private final Player player;
    private final Location location;
    private final LootBundle bundle;

    public PlayerFishingTreasureDropEvent(Player player, Location location, LootBundle bundle) {
        this.player = player;
        this.location = location;
        this.bundle = bundle;
    }
}
