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
import me.mykindos.betterpvp.core.client.stats.impl.IStat;
import me.mykindos.betterpvp.core.client.stats.impl.StringBuilderParser;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

@SuperBuilder
@Getter
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor
@CustomLog
/**
 * Represents stats only present in Game
 */
public class GameTeamMapNativeStat extends GameTeamMapStat implements IBuildableStat{
    public static final String PREFIX = "GAME_NATIVE";
    //todo formatter

    private static StringBuilderParser<GameTeamMapNativeStatBuilder<?, ?>> parser = new StringBuilderParser<>(
            List.of(
                    GameTeamMapNativeStat::parsePrefix,
                    GameTeamMapNativeStat::parseAction,
                        GameTeamMapNativeStat::parseGameName,
                    GameTeamMapNativeStat::parseTeamName,
                    GameTeamMapNativeStat::parseMapName
            )
    );

    public static GameTeamMapNativeStat fromString(String string) {
        return parser.parse(GameTeamMapNativeStat.builder(), string).build();
    }

    @NotNull
    private Action action;

    private static GameTeamMapNativeStatBuilder<?, ?> parsePrefix(GameTeamMapNativeStatBuilder<?, ?> builder, String input) {
        Preconditions.checkArgument(input.equals(PREFIX));
        return builder;
    }

    private static GameTeamMapNativeStatBuilder<?, ?> parseAction(GameTeamMapNativeStatBuilder<?, ?> builder, String input) {
        return builder.action(Action.valueOf(input));
    }

    private static GameTeamMapNativeStatBuilder<?, ?> parseGameName(GameTeamMapNativeStatBuilder<?, ?> builder, String input) {
        return builder.gameName(input);
    }

    private static GameTeamMapNativeStatBuilder<?, ?> parseMapName(GameTeamMapNativeStatBuilder<?, ?> builder, String input) {
        return builder.mapName(input);
    }

    private static GameTeamMapNativeStatBuilder<?, ?> parseTeamName(GameTeamMapNativeStatBuilder<?, ?> builder, String input) {
        return builder.teamName(input);
    }

    private boolean filterGameFullStat(Map.Entry<IStat, Double> entry) {
        final GameTeamMapNativeStat stat = (GameTeamMapNativeStat) entry.getKey();
        return gameName.equals(stat.gameName) && action.equals(stat.action);
    }

    private boolean filterGameTeamStat(Map.Entry<IStat, Double> entry) {
        final GameTeamMapNativeStat stat = (GameTeamMapNativeStat) entry.getKey();
        return action.equals(stat.action) && gameName.equals(stat.gameName) && teamName.equals(stat.teamName);
    }

    private boolean filterGameMapStat(Map.Entry<IStat, Double> entry) {
        final GameTeamMapNativeStat stat = (GameTeamMapNativeStat) entry.getKey();
        return action.equals(stat.action) && gameName.equals(stat.gameName) && mapName.equals(stat.mapName);
    }

    private boolean filterTeamOnlyStat(Map.Entry<IStat, Double> entry) {
        final GameTeamMapNativeStat stat = (GameTeamMapNativeStat) entry.getKey();
        return teamName.equals(stat.teamName) && action.equals(stat.action);
    }

    private boolean filterActionOnlyStat(Map.Entry<IStat, Double> entry) {
        final GameTeamMapNativeStat stat = (GameTeamMapNativeStat) entry.getKey();
        return action.equals(stat.action);
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
        if (!Strings.isNullOrEmpty(gameName)) {
            //have a game name
            if (Strings.isNullOrEmpty(mapName) && Strings.isNullOrEmpty(teamName)) {
                log.info("only game name").submit();
                //only game name
                return getFilteredStat(statContainer, periodKey, this::filterGameFullStat);
            }
            if (Strings.isNullOrEmpty(mapName)) {
                log.info("no map name").submit();
                //no map name
                return getFilteredStat(statContainer, periodKey, this::filterGameTeamStat);
            }
            if (Strings.isNullOrEmpty(teamName)) {
                log.info("no team name").submit();
                //no team name
                return getFilteredStat(statContainer, periodKey, this::filterGameMapStat);
            }
        }

        if (!Strings.isNullOrEmpty(teamName) && Strings.isNullOrEmpty(gameName) && Strings.isNullOrEmpty(mapName)) {
            log.info("map or game").submit();
            //no map or game but team
            return getFilteredStat(statContainer, periodKey, this::filterTeamOnlyStat);
        }

        //no map only stat because maps are tied to games

        if (Strings.isNullOrEmpty(gameName) && Strings.isNullOrEmpty(mapName) && Strings.isNullOrEmpty(teamName)) {
            //have action
            log.info("only action").submit();
            return getFilteredStat(statContainer, periodKey, this::filterActionOnlyStat);
        }
        //all are specified, do stat "normally"
        return statContainer.getProperty(periodKey, this);
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
     * Get the simple name of this stat, without qualifications (if present)
     * <p>
     * i.e. Time Played, Flags Captured
     *
     * @return the simple name
     */
    @Override
    public String getSimpleName() {
        return UtilFormat.cleanString(action.name());
    }

    /**
     * Whether this stat is directly savable to the database
     *
     * @return {@code true} if it is, {@code false} otherwise
     */
    @Override
    //todo
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
        try {
            final GameTeamMapNativeStat other = fromString(statName);
            //all filled fields must equal all the other filled fields
            if (!action.equals(other.action)) return false;
            //TODO check the logic here
            if (!Strings.isNullOrEmpty(gameName) && !gameName.equals(other.gameName)) return false;
            if (!Strings.isNullOrEmpty(teamName) && !teamName.equals(other.teamName)) return false;
            if (!Strings.isNullOrEmpty(mapName) && !mapName.equals(other.mapName)) return false;
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    @Override
    public boolean containsStat(IStat otherStat) {
        if (!(otherStat instanceof GameTeamMapNativeStat other)) return false;
        if (!action.equals(other.action)) return false;
        //TODO check the logic here
        if (!Strings.isNullOrEmpty(gameName) && !gameName.equals(other.gameName)) return false;
        if (!Strings.isNullOrEmpty(teamName) && !teamName.equals(other.teamName)) return false;
        if (!Strings.isNullOrEmpty(mapName) && !mapName.equals(other.mapName)) return false;
        return true;
    }

    /**
     * <p>Get the generic stat that includes this stat.</p>
     * <p>{@link IStat#containsStat(IStat)} of the generic should be {@code true} for this stat</p>
     *
     * @return the generic stat
     */
    @Override
    public @NotNull IStat getGenericStat() {
        return GameTeamMapNativeStat.builder().action(action).build();
    }

    @Override
    public @NotNull IBuildableStat copyFromStatname(@NotNull String statName) {
        GameTeamMapNativeStat other = fromString(statName);
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
        //DOM
        POINTS_KILLS,
        POINTS_GEMS,
        GEMS_PICKED_UP, //todo
        CONTROL_POINT_CAPTURED,
        CONTROL_POINT_TIME_CAPTURING,
        CONTROL_POINT_TIME_CONTESTED,
        //CTF
        FLAG_CAPTURES,
        FLAG_CARRIER_TIME,
        FLAG_CARRIER_KILLS,
        FLAG_CARRIER_DEATHS,
        FLAG_PICKUP,
        FLAG_DROP,
        SUDDEN_DEATH_KILLS,
        SUDDEN_DEATH_DEATHS,
        SUDDEN_DEATH_FLAG_CAPTURES,
        //generic
        GAME_TIME_PLAYED,
        SPECTATE_TIME,
        WIN,
        LOSS,
        MATCHES_PLAYED,

        RESTOCK//todo
    }
}
