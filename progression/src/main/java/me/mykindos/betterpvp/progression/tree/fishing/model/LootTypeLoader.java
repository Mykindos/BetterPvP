package me.mykindos.betterpvp.progression.tree.fishing.model;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public interface LootTypeLoader<T extends FishingLootType> {

    /**
     * The config key for this loot type
     * @return The config key
     */
    String getTypeKey();

    @NotNull T read(ConfigurationSection section);

}
