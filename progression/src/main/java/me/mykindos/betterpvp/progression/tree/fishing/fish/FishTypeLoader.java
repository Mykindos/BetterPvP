package me.mykindos.betterpvp.progression.tree.fishing.fish;

import com.google.common.base.Preconditions;
import me.mykindos.betterpvp.progression.tree.fishing.model.FishingConfigLoader;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public class FishTypeLoader implements FishingConfigLoader<FishType> {

    @Override
    public String getTypeKey() {
        return "fish";
    }

    @Override
    public @NotNull FishType read(ConfigurationSection section) {
        final String name = section.getString("name");
        Preconditions.checkNotNull(name, "Fish name cannot be null");
        return new SimpleFishType(name);
    }

}
