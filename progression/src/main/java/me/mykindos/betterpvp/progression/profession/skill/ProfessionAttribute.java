package me.mykindos.betterpvp.progression.profession.skill;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ProfessionAttribute {

    // Fishing
    CATCH_SPEED("Catch speed", "%"),
    CATCH_SPEED_NEARBY("Catch speed (Nearby Players)", "%"),
    CATCH_WEIGHT("Catch size", "%"),
    BAIT_DURATION("Bait duration", "%"),
    BAIT_RADIUS("Bait radius", "%"),
    BAIT_NON_CONSUMPTION_CHANCE("Chance to not consume Baits", "%"),
    BAIT_MULTIPLIER("Bait multiplier", "%"),
    FISHING_TREASURE_CHANCE("Treasure chance", "%"),
    FISHING_DOUBLE_TREASURE_CHANCE("Double treasure chance", "%"),
    FISHING_DURABILITY_REDUCTION("Reduced durability loss", "%"),
    FISH_INCREASED_STACK_SIZE("Maximum stack size (fish)", "");

    private final String name;
    private final String operation;
}
