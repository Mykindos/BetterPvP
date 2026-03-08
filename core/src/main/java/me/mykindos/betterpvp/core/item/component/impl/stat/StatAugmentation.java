package me.mykindos.betterpvp.core.item.component.impl.stat;

import lombok.Value;

@Value
public class StatAugmentation {

    StatType<?> type;
    int tier;
    double value;
    Operation operation;

    public <T> ItemStat<T> apply(ItemStat<T> stat) {
        StatType<T> statType = stat.getType();
        T currentModifier = stat.getRangeMinFlatModifier();
        T augmentationValue = switch (operation) {
            case ADD -> statType.fromDouble(value);
            case ADD_PERCENTAGE -> statType.multiply(stat.getValue(), value);
        };

        T newModifier = statType.add(currentModifier, augmentationValue);

        // Cap the modifier to ensure range min doesn't exceed range max
        // Maximum allowed modifier is: rangeMax - baseRangeMin
        T maxAllowedModifier = statType.subtract(stat.getRangeMax(), stat.getBaseRangeMin());
        newModifier = statType.min(newModifier, maxAllowedModifier);

        // Return the stat with the new modifier (value will be randomized separately)
        return stat.withRangeMinFlatModifier(newModifier);
    }

    public enum Operation {
        ADD, ADD_PERCENTAGE
    }

}
