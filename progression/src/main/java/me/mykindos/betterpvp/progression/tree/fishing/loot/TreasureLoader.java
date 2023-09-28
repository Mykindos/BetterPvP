package me.mykindos.betterpvp.progression.tree.fishing.loot;

import me.mykindos.betterpvp.progression.tree.fishing.model.LootTypeLoader;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public class TreasureLoader implements LootTypeLoader<TreasureType> {
    @Override
    public String getTypeKey() {
        return "treasure";
    }

    @Override
    public @NotNull TreasureType read(ConfigurationSection section) {
        return new TreasureType(section.getName());
    }
}
