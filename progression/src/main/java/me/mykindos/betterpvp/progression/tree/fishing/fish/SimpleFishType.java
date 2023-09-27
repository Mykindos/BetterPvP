package me.mykindos.betterpvp.progression.tree.fishing.fish;

import lombok.Data;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.progression.tree.fishing.model.FishType;

/**
 * Represents a fish that has a minimum and maximum weight with no
 * custom implementation.
 */
@Data
public class SimpleFishType implements FishType {

    private final String name;
    private int minWeight;
    private int maxWeight;
    private int frequency;

    @Override
    public void loadConfig(ExtendedYamlConfiguration config) {
        final String key = name.toLowerCase().replace(" ", "_");
        this.frequency = config.getInt("fishing.fish." + key + ".frequency");
        this.minWeight = config.getInt("fishing.fish." + key + ".minWeight");
        this.maxWeight = config.getInt("fishing.fish." + key + ".maxWeight");
    }
}
