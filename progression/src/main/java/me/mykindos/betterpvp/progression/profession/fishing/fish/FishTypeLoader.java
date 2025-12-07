package me.mykindos.betterpvp.progression.profession.fishing.fish;

import com.google.common.base.Preconditions;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.progression.profession.fishing.model.FishingConfigLoader;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public class FishTypeLoader implements FishingConfigLoader<FishType> {

    private final ItemFactory itemFactory;

    public FishTypeLoader(ItemFactory itemFactory) {
        this.itemFactory = itemFactory;
    }

    @Override
    public String getTypeKey() {
        return "fish";
    }

    @Override
    public @NotNull FishType read(ConfigurationSection section) {
        final String key = section.getName();
        Preconditions.checkNotNull(key, "Fish name cannot be null");
        return new SimpleFishType(key, itemFactory);
    }

}
