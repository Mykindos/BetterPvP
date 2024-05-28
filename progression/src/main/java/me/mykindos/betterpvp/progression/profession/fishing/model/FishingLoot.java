package me.mykindos.betterpvp.progression.profession.fishing.model;

import me.mykindos.betterpvp.progression.profession.fishing.event.PlayerCaughtFishEvent;

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
