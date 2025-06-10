package me.mykindos.betterpvp.core.client.stats;

import lombok.Getter;

@Getter
public enum ClientStat implements IClientStat {

    DEATHS("Deaths",
            "Number of deaths"),
    MOB_KILLS("Mob Kills",
            "Number of non-player entities killed");

    private final String name;
    private final String[] description;

    ClientStat(String name, String... description) {
        this.name = name;
        this.description = description;
    }
}
