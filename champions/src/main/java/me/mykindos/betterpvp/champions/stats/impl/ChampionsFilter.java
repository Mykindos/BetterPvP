package me.mykindos.betterpvp.champions.stats.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.mykindos.betterpvp.champions.stats.repository.ChampionsCombatData;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.stats.filter.FilterType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum ChampionsFilter implements FilterType {

    GLOBAL("Global", null),
    NONE("No Role", null),
    BRUTE("Brute", Role.BRUTE),
    RANGER("Ranger", Role.RANGER),
    MAGE("Mage", Role.MAGE),
    ASSASSIN("Assassin", Role.ASSASSIN),
    KNIGHT("Knight", Role.KNIGHT),
    WARLOCK("Warlock", Role.WARLOCK);

    private final @NotNull String name;
    private final @Nullable Role role;

    public static ChampionsFilter fromRole(Role role) {
        return Arrays.stream(values())
                .filter(filter -> filter.role == role && filter != GLOBAL)
                .findFirst()
                .orElse(NONE);
    }

    @Override
    public boolean accepts(Object entry) {
        return this != GLOBAL && entry instanceof ChampionsCombatData data && data.getRole() == role;
    }
}
