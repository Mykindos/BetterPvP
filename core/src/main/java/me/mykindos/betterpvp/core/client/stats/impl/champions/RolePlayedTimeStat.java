package me.mykindos.betterpvp.core.client.stats.impl.champions;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.impl.IStat;

@Builder
//todo make it like skill stat
@EqualsAndHashCode
@AllArgsConstructor(access = AccessLevel.PROTECTED)
//@NoArgsConstructor
public class RolePlayedTimeStat implements IStat {
    /*public static String PREFIX = "GAME_GAME_MAP_PLAYED_TIME";

    private static StringBuilderParser<MapPlayedTimeStat.MapPlayedTimeStatBuilder> parser = new StringBuilderParser<>(
            List.of(
                    MapPlayedTimeStat::parsePrefix,
                    MapPlayedTimeStat::parseGameName,
                    MapPlayedTimeStat::parseMapName
            )
    );

    public static MapPlayedTimeStat fromString(String string) {
        return parser.parse(MapPlayedTimeStat.builder(), string).build();
    }

    /**
     * Get the stat represented by this object from the statContainer
     *
     * @param statContainer
     * @param period
     * @return
     */
    @Override
    public Double getStat(StatContainer statContainer, String period) {
        return 0.0;
    }

    @Override
    public String getStatName() {
        return "";
    }

    /**
     * Whether this stat is directly savable to the database
     *
     * @return {@code true} if it is, {@code false} otherwise
     */
    @Override
    public boolean isSavable() {
        return true;
    }

    /**
     * Whether this stat contains this statName
     *
     * @param statName
     * @return
     */
    @Override
    public boolean containsStat(String statName) {
        return statName.equals(getStatName());
    }
}
