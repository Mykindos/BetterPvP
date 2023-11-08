package me.mykindos.betterpvp.progression.tree.fishing.fish;

import lombok.Data;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.progression.tree.fishing.model.FishingLoot;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

/**
 * Represents a fish that has a minimum and maximum weight with no
 * custom implementation.
 */
@Data
public class SimpleFishType implements FishType {

    private static final Random RANDOM = new Random();

    private final String name;
    private int minWeight;
    private int maxWeight;
    private int frequency;

    @Override
    public void loadConfig(@NotNull ExtendedYamlConfiguration config) {
        final String key = name.toLowerCase().replace(" ", "_");
        this.frequency = config.getOrSaveInt("fishing.loot." + key + ".frequency", 1);
        this.minWeight = config.getOrSaveInt("fishing.loot." + key + ".minWeight", 1);
        this.maxWeight = config.getOrSaveInt("fishing.loot." + key + ".maxWeight", 1);
    }

    @Override
    public FishingLoot generateLoot() {
        final int weight = RANDOM.ints(minWeight, maxWeight + 1)
                .findFirst()
                .orElse(minWeight);
        return new Fish(this, weight);
    }
}
