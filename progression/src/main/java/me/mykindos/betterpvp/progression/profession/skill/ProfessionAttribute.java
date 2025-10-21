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
    FISH_INCREASED_STACK_SIZE("Maximum stack size (fish)", ""),

    // Woodcutting
    CHOP_SPEED("Chop speed", "%"),
    WOODCUTTING_INCREASED_EXPERIENCE("Increased experience", "%"),
    BARK_CHANCE("Chance to get bark when stipping logs", "%"),
    SAPLING_GROWTH_SPEED("Sapling growth speed", "%"),
    WOODCUTTING_TREASURE_CHANCE("Treasure chance", "%"),
    WOODCUTTING_DOUBLE_TREASURE_CHANCE("Double treasure chance", "%"),
    LOG_INCREASED_STACK_SIZE("Maximum stack size (log)", ""),
    TREEFELLER_COOLDOWN_REDUCTION("Reduced Treefeller cooldown", "%"),
    WOODCUTTING_DURABILITY_REDUCTION("Reduced durability loss", "%");

    private final String name;
    private final String operation;
}
