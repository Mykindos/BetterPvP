package me.mykindos.betterpvp.progression.tree.fishing.loot;

import me.mykindos.betterpvp.progression.tree.fishing.model.FishingConfigLoader;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public class SwimmerLoader implements FishingConfigLoader<SwimmerType> {
    @Override
    public String getTypeKey() {
        return "entity";
    }

    @Override
    public @NotNull SwimmerType read(ConfigurationSection section) {
        return new SwimmerType(section.getName());
    }
}
