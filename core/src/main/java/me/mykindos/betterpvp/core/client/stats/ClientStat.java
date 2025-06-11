package me.mykindos.betterpvp.core.client.stats;

import lombok.Getter;

import java.util.Set;

@Getter
public enum ClientStat implements IClientStat {

    //generic
    DEATHS("Deaths",
            "Number of deaths"),
    MOB_KILLS("Mob Kills",
            "Number of non-player entities killed"),

    //champions


    //clans
    SET_CORE("Set Core", "Number of times you set your clan core"),
    TELEPORT_CORE("Teleport to Core", "Number of times you teleported to your clan core"),

    //events
    DREADBEARD_KILLS("Kill Dreadbeard", "Number of times you killed Dreadbeard"),
    SKELETON_KING_KILLS("Kill the Skeleton King", "Number of times you killed the Skeleton King")

    ;

    private final String name;
    private final String[] description;
    private CompositeStat compositeStat;

    ClientStat(String name, String... description) {
        this.name = name;
        this.description = description;
        this.compositeStat = null;
    }

    ClientStat(String name, Set<IStat> compositeStats, String... description) {
        this.name = name;
        this.description = description;
        this.compositeStat = new CompositeStat(getStatName(), compositeStats);
    }

    @Override
    public Double getStat(StatContainer statContainer, String period) {
        if (compositeStat == null) {
            return statContainer.getProperty(period,this.name());
        }

        return compositeStat.getStat(statContainer, period);
    }

    @Override
    public String getStatName() {
        return name();
    }

    /**
     * Whether or not this stat is directly savable to the database
     *
     * @return {@code true} if it is, {@code false} otherwise
     */
    @Override
    public boolean isSavable() {
        return compositeStat == null;
    }

    @Override
    public boolean containsStat(String statName) {
        if (compositeStat == null) {
            return getStatName().equals(statName);
        }
        return compositeStat.containsStat(statName);
    }
}
