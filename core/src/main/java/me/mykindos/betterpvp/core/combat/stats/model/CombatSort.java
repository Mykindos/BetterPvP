package me.mykindos.betterpvp.core.combat.stats.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.mykindos.betterpvp.core.stats.sort.SortType;

import java.util.function.Function;

@AllArgsConstructor
public enum CombatSort implements SortType {

    RATING("Rating", CombatData::getRating),
    KILLS("Kills", CombatData::getKills),
    KDR("KDR", CombatData::getKillDeathRatio),
    KILLSTREAK("Killstreak", CombatData::getKillStreak),
    HIGHEST_KILLSTREAK("Highest Killstreak", CombatData::getHighestKillStreak),
    DEATHS("Deaths", CombatData::getDeaths);

    @Getter
    private final String name;
    private Function<CombatData, Number> function;

    public String getValue(CombatData data){
        final Number result = function.apply(data);
        if (result instanceof Double dub) {
            return String.format("%,.2f", dub);
        } else if (result instanceof Float fl) {
            return String.format("%,.2f", fl);
        } else if (result instanceof Integer in) {
            return String.format("%,d", in);
        } else if (result instanceof Long lo) {
            return String.format("%,d", lo);
        } else {
            return result.toString();
        }
    }

}
