package me.mykindos.betterpvp.core.client.stats.impl.game;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
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
@CustomLog
public class DOMGameStat extends TeamMapStat implements IBuildableStat{
    public static final String PREFIX = "GAME_DOM";
    //todo formatter

    private static StringBuilderParser<DOMGameStatBuilder<?, ?>> parser = new StringBuilderParser<>(
            List.of(
                    DOMGameStat::parsePrefix,
                    DOMGameStat::parseAction,
                    DOMGameStat::parseTeamName,
                    DOMGameStat::parseMapName
            )
    );

    public static DOMGameStat fromString(String string) {
        return parser.parse(DOMGameStat.builder(), string).build();
    }

    @NotNull
    private Action action;

    private static DOMGameStatBuilder<?, ?> parsePrefix(DOMGameStatBuilder<?, ?> builder, String input) {
        Preconditions.checkArgument(input.equals(PREFIX));
        return builder;
    }

    private static DOMGameStatBuilder<?, ?> parseAction(DOMGameStatBuilder<?, ?> builder, String input) {
        return builder.action(Action.valueOf(input));
    }

    private static DOMGameStatBuilder<?, ?> parseMapName(DOMGameStatBuilder<?, ?> builder, String input) {
        return builder.mapName(input);
    }

    private static DOMGameStatBuilder<?, ?> parseTeamName(DOMGameStatBuilder<?, ?> builder, String input) {
        return builder.teamName(input);
    }

    private boolean filterActionOnlyStat(Map.Entry<String, Double> entry) {
        DOMGameStat stat = fromString(entry.getKey());
        return action == stat.action;
    }

    private boolean filterMapOnlyStat(Map.Entry<String, Double> entry) {
        DOMGameStat stat = fromString(entry.getKey());
        return action == stat.action && mapName.equals(stat.mapName);
    }

    private boolean filterTeamOnlyStat(Map.Entry<String, Double> entry) {
        DOMGameStat stat = fromString(entry.getKey());
        return action == stat.action && teamName.equals(stat.teamName);
    }

    /**
     * Get the stat represented by this object from the statContainer
     *
     * @param statContainer
     * @param periodKey
     * @return
     */
    @Override
    public Double getStat(StatContainer statContainer, String periodKey) {
        if (Strings.isNullOrEmpty(mapName) && Strings.isNullOrEmpty(teamName)) {
            return getFilteredStat(statContainer, periodKey, this::filterActionOnlyStat);
        }
        if (Strings.isNullOrEmpty(teamName)) {
            return getFilteredStat(statContainer, periodKey, this::filterMapOnlyStat);
        }

        if (Strings.isNullOrEmpty(mapName)) {
            return getFilteredStat(statContainer, periodKey, this::filterTeamOnlyStat);
        }
        return statContainer.getProperty(periodKey, getStatName());
    }

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
        DOMGameStat other = fromString(statName);
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
        POINTS_KILLS,
        POINTS_GEMS,
        GEMS_PICKED_UP, //todo
        CONTROL_POINT_CAPTURED,
        CONTROL_POINT_TIME_CAPTURING,
        CONTROL_POINT_TIME_CONTESTED
    }
}
