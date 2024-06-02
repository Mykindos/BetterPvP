package me.mykindos.betterpvp.progression.profession.fishing.loot;

import me.mykindos.betterpvp.progression.profession.fishing.model.FishingConfigLoader;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public class TreasureLoader implements FishingConfigLoader<TreasureType> {
    @Override
    public String getTypeKey() {
        return "treasure";
    }

    @Override
    public @NotNull TreasureType read(ConfigurationSection section) {
        return new TreasureType(section.getName());
    }
}
