package me.mykindos.betterpvp.progression.tree.fishing.rod;

import lombok.Getter;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.progression.tree.fishing.fish.Fish;
import me.mykindos.betterpvp.progression.tree.fishing.model.FishingLoot;
import me.mykindos.betterpvp.progression.tree.fishing.model.FishingRodType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.checkerframework.common.value.qual.IntRange;
import org.jetbrains.annotations.NotNull;

/**
 * Represents fishing rods that have a max weight they can carry,
 * meaning only fish of below the maximum weight can be caught.
 */
@Getter
public enum SimpleFishingRod implements FishingRodType {
    WOODEN(1, "Wooden Rod", 400),
    ALUMINUM(2, "Aluminum Rod", 1000),
    STEEL(3, "Steel Rod", 1500),
    TITANIUM(4, "Titanium Rod", 2000),
    ;

    private final int id;
    private final @NotNull String name;
    private @IntRange(from = 1, to = Integer.MAX_VALUE) int maxWeight;

    SimpleFishingRod(final int id, final @NotNull String name, final int defMaxWeight) {
        this.id = id;
        this.name = name;
        this.maxWeight = defMaxWeight;
    }

    @Override
    public boolean canReel(@NotNull FishingLoot loot) {
        return !(loot instanceof Fish fish) || fish.getWeight() <= maxWeight;
    }

    @Override
    public Component[] getDescription() {
        return new Component[] {
                UtilMessage.deserialize("<gray>Max Weight: <alt2>" + maxWeight + "</alt2>").decoration(TextDecoration.ITALIC, false),
        };
    }

    @Override
    public void loadConfig(ExtendedYamlConfiguration config) {
        final String key = this.name.toLowerCase();
        this.maxWeight = config.getOrSaveInt("fishing.rods." + key + ".maxWeight", maxWeight);
    }
}
