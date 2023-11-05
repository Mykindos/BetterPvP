package me.mykindos.betterpvp.progression.tree.fishing.model;

import me.mykindos.betterpvp.progression.tree.fishing.event.PlayerCaughtFishEvent;
import org.bukkit.event.player.PlayerFishEvent;

public interface FishingLoot {

    /**
     * Get the type of this loot
     * @return The type of this loot
     */
    FishingLootType getType();

    /**
     * Called whenever this loot is caught
     * @param event The event
     */
    void processCatch(PlayerCaughtFishEvent event);

}
