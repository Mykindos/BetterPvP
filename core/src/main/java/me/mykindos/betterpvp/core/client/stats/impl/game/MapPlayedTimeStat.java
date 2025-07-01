package me.mykindos.betterpvp.core.client.stats.impl.game;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.impl.IBuildableStat;
import me.mykindos.betterpvp.core.client.stats.impl.StringBuilderParser;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

@Builder
@Getter
@EqualsAndHashCode
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor
public class MapPlayedTimeStat implements IBuildableStat {
    public static String PREFIX = "GAME_GAME_MAP_PLAYED_TIME";

    private static StringBuilderParser<MapPlayedTimeStatBuilder> parser = new StringBuilderParser<>(
            List.of(
                    MapPlayedTimeStat::parsePrefix,
                    MapPlayedTimeStat::parseGameName,
                    MapPlayedTimeStat::parseMapName
            )
    );

    public static MapPlayedTimeStat fromString(String string) {
        return parser.parse(MapPlayedTimeStat.builder(), string).build();
    }

    @NotNull
    private String gameName;
    @NotNull
    @Builder.Default
    private String mapName = "";

    private static MapPlayedTimeStatBuilder parsePrefix(MapPlayedTimeStatBuilder builder, String input) {
        Preconditions.checkArgument(input.equals(PREFIX));
        return builder;
    }

    private static MapPlayedTimeStatBuilder parseGameName(MapPlayedTimeStatBuilder builder, String input) {
        return builder.gameName(input);
    }

    private static MapPlayedTimeStatBuilder parseMapName(MapPlayedTimeStatBuilder builder, String input) {
        return builder.mapName(input);
    }

    private Double getGameStat(StatContainer statContainer, String period) {
        return statContainer.getStats().getStatsOfPeriod(period).entrySet().stream()
                .filter(entry ->
                        entry.getKey().startsWith(PREFIX + StringBuilderParser.INTRA_SEQUENCE_DELIMITER + gameName)
                ).mapToDouble(Map.Entry::getValue)
                .sum();
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
        if (Strings.isNullOrEmpty(mapName)) {
            return getGameStat(statContainer, period);
        }
        return statContainer.getProperty(period, getStatName());
    }

    @Override
    public String getStatName() {
        return parser.asString(
                List.of(
                        PREFIX,
                        gameName,
                        mapName
                )
        );
    }

    /**
     * Whether this stat is directly savable to the database
     *
     * @return {@code true} if it is, {@code false} otherwise
     */
    @Override
    public boolean isSavable() {
        return !Strings.isNullOrEmpty(mapName);
    }

    /**
     * Whether this stat contains this statName
     *
     * @param statName
     * @return
     */
    @Override
    public boolean containsStat(String statName) {
        return false;
    }

    @Override
    public IBuildableStat copyFromStatname(@NotNull String statName) {
        MapPlayedTimeStat other = fromString(statName);
        this.gameName = other.gameName;
        this.mapName = other.mapName;
        return this;
    }

    @Override
    public String getPrefix() {
        return PREFIX;
    }
}
