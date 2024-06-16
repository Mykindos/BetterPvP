package me.mykindos.betterpvp.core.stats;

import lombok.Getter;

@Getter
public enum LeaderboardCategory {

    CLANS("Clans"),

    CHAMPIONS("Champions"),

    PROFESSION("Professions"),

    DUNGEONS("Dungeons");

    private final String name;

    LeaderboardCategory(String name) {
        this.name = name;
    }
}
