package me.mykindos.betterpvp.core.client.stats.impl.game;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.impl.IBuildableStat;
import me.mykindos.betterpvp.core.client.stats.impl.StringBuilderParser;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

@SuperBuilder
@Getter
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor
public class CTFGameStat extends TeamMapStat implements IBuildableStat {
    public static final String PREFIX = "GAME_CTF";
    //todo formatter

    private static StringBuilderParser<CTFGameStatBuilder<?, ?>> parser = new StringBuilderParser<>(
            List.of(
                    CTFGameStat::parsePrefix,
                    CTFGameStat::parseAction,
                    CTFGameStat::parseTeamName,
                    CTFGameStat::parseMapName
            )
    );

    public static CTFGameStat fromString(String string) {
        return parser.parse(CTFGameStat.builder(), string).build();
    }

    @NotNull
    private CTFGameStat.Action action;

    private static CTFGameStatBuilder<?, ?> parsePrefix(CTFGameStatBuilder<?, ?> builder, String input) {
        Preconditions.checkArgument(input.equals(PREFIX));
        return builder;
    }

    private static CTFGameStatBuilder<?, ?> parseAction(CTFGameStatBuilder<?, ?> builder, String input) {
        return builder.action(CTFGameStat.Action.valueOf(input));
    }

    private static CTFGameStatBuilder<?, ?> parseMapName(CTFGameStatBuilder<?, ?> builder, String input) {
        return builder.mapName(input);
    }

    private static CTFGameStatBuilder<?, ?> parseTeamName(CTFGameStatBuilder<?, ?> builder, String input) {
        return builder.teamName(input);
    }

    private Double getActionStat(StatContainer statContainer, String period) {
        return statContainer.getStats().getStatsOfPeriod(period).entrySet().stream()
                .filter(entry ->
                        entry.getKey().startsWith(PREFIX + StringBuilderParser.INTRA_SEQUENCE_DELIMITER + action.name())
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
            return getActionStat(statContainer, period);
        }
        return statContainer.getProperty(period, getStatName());
    }

    //todo teamname
    @Override
    public String getStatName() {
        return parser.asString(
                List.of(
                        PREFIX,
                        action.name(),
                        teamName,
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
        return statName.startsWith(getStatName());
    }

    @Override
    public IBuildableStat copyFromStatname(@NotNull String statName) {
        CTFGameStat other = fromString(statName);
        this.action = other.action;
        this.teamName = other.teamName;
        this.mapName = other.mapName;
        return this;
    }

    @Override
    public String getPrefix() {
        return PREFIX;
    }

    public enum Action {
        FLAG_CAPTURES,
        FLAG_CARRIER_TIME,
        FLAG_CARRIER_KILLS,
        FLAG_CARRIER_DEATHS,
        FLAG_PICKUP,
        FLAG_DROP,
        SUDDEN_DEATH_KILLS,
        SUDDEN_DEATH_DEATHS,
        SUDDEN_DEATH_FLAG_CAPTURES
    }
}
