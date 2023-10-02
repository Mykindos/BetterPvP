package me.mykindos.betterpvp.progression.tree.fishing.bait.speed;

import me.mykindos.betterpvp.progression.tree.fishing.model.BaitType;
import me.mykindos.betterpvp.progression.tree.fishing.model.FishingConfigLoader;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public class SpeedBaitLoader implements FishingConfigLoader<BaitType> {
    @Override
    public String getTypeKey() {
        return "speed";
    }

    @Override
    public @NotNull BaitType read(ConfigurationSection section) {
        return new SpeedBaitType(section.getName());
    }
}
