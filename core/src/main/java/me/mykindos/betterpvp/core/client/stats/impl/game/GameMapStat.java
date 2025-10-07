package me.mykindos.betterpvp.core.client.stats.impl.game;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
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
public class GameMapStat extends TeamMapStat implements IBuildableStat {
    public static final String PREFIX = "GAME_MAP";
    //todo formatter

    private static StringBuilderParser<GameMapStatBuilder<?, ?>> parser = new StringBuilderParser<>(
            List.of(
                    GameMapStat::parsePrefix,
                    GameMapStat::parseAction,
                    GameMapStat::parseGameName,
                    GameMapStat::parseTeamName,
                    GameMapStat::parseMapName
            )
    );

    /**
     * Constructs the given String into a Stat
     * @param string the stringified stat
     * @return this stat
     * @throws IllegalArgumentException if this string does not represent this Stat
     */
    public static GameMapStat fromString(String string) {
        return parser.parse(GameMapStat.builder(), string).build();
    }

    @NotNull
    private Action action;
    @NotNull
    @Builder.Default
    private String gameName = "";

    private static GameMapStatBuilder<?, ?> parsePrefix(GameMapStatBuilder<?, ?> builder, String input) {
        Preconditions.checkArgument(input.equals(PREFIX));
        return builder;
    }

    private static GameMapStatBuilder<?, ?> parseAction(GameMapStatBuilder<?, ?> builder, String input) {
        return builder.action(Action.valueOf(input));
    }

    private static GameMapStatBuilder<?, ?> parseGameName(GameMapStatBuilder<?, ?> builder, String input) {
        return builder.gameName(input);
    }

    private static GameMapStatBuilder<?, ?> parseMapName(GameMapStatBuilder<?, ?> builder, String input) {
        return builder.mapName(input);
    }

    private static GameMapStatBuilder<?, ?> parseTeamName(GameMapStatBuilder<?, ?> builder, String input) {
        return builder.teamName(input);
    }

    private boolean filterGameFullStat(Map.Entry<String, Double> entry) {
        final GameMapStat stat = fromString(entry.getKey());
        return action == stat.action && gameName.equals(stat.gameName);
    }

    private boolean filterGameTeamStat(Map.Entry<String, Double> entry) {
        GameMapStat stat = fromString(entry.getKey());
        return action == stat.action && gameName.equals(stat.gameName) && teamName.equals(stat.teamName);
    }

    private boolean filterGameMapStat(Map.Entry<String, Double> entry) {
        GameMapStat stat = fromString(entry.getKey());
        return action == stat.action && gameName.equals(stat.gameName) && mapName.equals(stat.mapName);
    }

    private boolean filterTeamOnlyStat(Map.Entry<String, Double> entry) {
        GameMapStat stat = fromString(entry.getKey());
        return action == stat.action && teamName.equals(stat.teamName);
    }

    private boolean filterActionOnlyStat(Map.Entry<String, Double> entry) {
        GameMapStat stat = fromString(entry.getKey());
        return action == stat.action;
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
        //todo action composites stats
        if (!Strings.isNullOrEmpty(gameName)) {
            //have a game name
            if (Strings.isNullOrEmpty(mapName) && Strings.isNullOrEmpty(teamName)) {
                //only game name
                return getFilteredStat(statContainer, periodKey, this::filterGameFullStat);
            }
            if (Strings.isNullOrEmpty(mapName)) {
                //no map name
                return getFilteredStat(statContainer, periodKey, this::filterGameTeamStat);
            }
            if (Strings.isNullOrEmpty(teamName)) {
                //no team name
                return getFilteredStat(statContainer, periodKey, this::filterGameMapStat);
            }
        }

        if (!Strings.isNullOrEmpty(teamName) && Strings.isNullOrEmpty(gameName) && Strings.isNullOrEmpty(mapName)) {
            //no map or game but team
            return getFilteredStat(statContainer, periodKey, this::filterTeamOnlyStat);
        }

        //no map only stat because maps are tied to games

        if (Strings.isNullOrEmpty(gameName) && Strings.isNullOrEmpty(mapName) && Strings.isNullOrEmpty(teamName)) {
            //have action
            return getFilteredStat(statContainer, periodKey, this::filterActionOnlyStat);
        }
        //all fields are filled, action, game, team, map
        return statContainer.getProperty(periodKey, getStatName());
    }

    @Override
    public String getStatName() {
        return parser.asString(
                List.of(
                        PREFIX,
                        action.name(),
                        gameName,
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
    //todo on all changed stats
    public boolean containsStat(String statName) {
        return statName.startsWith(getStatName());
    }

    @Override
    public IBuildableStat copyFromStatname(@NotNull String statName) {
        GameMapStat other = fromString(statName);
        this.action = other.action;
        this.gameName = other.gameName;
        this.teamName = other.teamName;
        this.mapName = other.mapName;
        return this;
    }

    @Override
    public String getPrefix() {
        return PREFIX;
    }

    public enum Action {
        TIME_PLAYED,
        SPECTATE_TIME,
        WIN,
        LOSS,
        MATCHES_PLAYED,
        //todo
        RESTOCK
    }
}
