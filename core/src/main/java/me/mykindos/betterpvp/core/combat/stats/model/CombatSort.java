package me.mykindos.betterpvp.core.combat.stats.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.mykindos.betterpvp.core.stats.sort.SortType;

@AllArgsConstructor
@Getter
public enum CombatSort implements SortType {

    RATING("Rating"),
    KILLS("Kills"),
    DEATHS("Deaths"),
    KDR("KDR"),
    KILLSTREAK("Killstreak"),
    HIGHEST_KILLSTREAK("Highest Killstreak");

    private final String name;

}
