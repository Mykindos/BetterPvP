package me.mykindos.betterpvp.progression.tree.fishing.rod;

import lombok.Getter;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.progression.tree.fishing.model.Fish;
import me.mykindos.betterpvp.progression.tree.fishing.model.FishingRodType;
import org.checkerframework.common.value.qual.IntRange;
import org.jetbrains.annotations.NotNull;

/**
 * Represents fishing rods that have a max weight they can carry,
 * meaning only fish of below the maximum weight can be caught.
 */
@Getter
public enum SimpleFishingRod implements FishingRodType {
    WOODEN("Wooden Fishing Rod", 400),
    ALUMINUM("Aluminum Fishing Rod", 1000),
    STEEL("Steel Fishing Rod", 1500),
    TITANIUM("Titanium Fishing Rod", 2000),
    ;

    private final @NotNull String name;
    private @IntRange(from = 1, to = Integer.MAX_VALUE) int maxWeight;

    SimpleFishingRod(final @NotNull String name, final int defMaxWeight) {
        this.name = name;
        this.maxWeight = defMaxWeight;
    }

    @Override
    public boolean canReel(@NotNull Fish fish) {
        return fish.getWeight() <= maxWeight;
    }

    @Override
    public void loadConfig(ExtendedYamlConfiguration config) {
        final String key = this.name.toLowerCase();
        this.maxWeight = config.getInt("fishing.rods." + key + ".maxWeight");
    }
}
