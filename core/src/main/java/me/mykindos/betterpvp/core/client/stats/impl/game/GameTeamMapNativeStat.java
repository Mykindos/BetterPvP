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
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.util.Map;
import java.util.Objects;

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
    public static final String TYPE = "GAME_NATIVE";

    public static GameTeamMapNativeStat fromData(@NotNull String type, JSONObject object) {
        GameTeamMapNativeStatBuilder<?, ?> builder = builder();
        Preconditions.checkArgument(type.equals(TYPE));
        builder.action(Action.valueOf(object.getString("action")));
        builder.gameId(object.getLong("gameId"));
        builder.gameName(object.getString("gameName"));
        builder.mapName(object.getString("mapName"));
        builder.teamName(object.getString("teamName"));
        return builder.build();
    }

    @NotNull
    private Action action;

    private boolean filterGameIdStat(Map.Entry<IStat, Long> entry) {
        final GameTeamMapNativeStat stat = (GameTeamMapNativeStat) entry.getKey();
        return Objects.requireNonNull(gameId).equals(stat.gameId) && action.equals(stat.action);
    }

    private boolean filterGameFullStat(Map.Entry<IStat, Long> entry) {
        final GameTeamMapNativeStat stat = (GameTeamMapNativeStat) entry.getKey();
        return gameName.equals(stat.gameName) && action.equals(stat.action);
    }

    private boolean filterGameTeamStat(Map.Entry<IStat, Long> entry) {
        final GameTeamMapNativeStat stat = (GameTeamMapNativeStat) entry.getKey();
        return action.equals(stat.action) && gameName.equals(stat.gameName) && teamName.equals(stat.teamName);
    }

    private boolean filterGameMapStat(Map.Entry<IStat, Long> entry) {
        final GameTeamMapNativeStat stat = (GameTeamMapNativeStat) entry.getKey();
        return action.equals(stat.action) && gameName.equals(stat.gameName) && mapName.equals(stat.mapName);
    }

    private boolean filterTeamOnlyStat(Map.Entry<IStat, Long> entry) {
        final GameTeamMapNativeStat stat = (GameTeamMapNativeStat) entry.getKey();
        return teamName.equals(stat.teamName) && action.equals(stat.action);
    }

    private boolean filterActionOnlyStat(Map.Entry<IStat, Long> entry) {
        final GameTeamMapNativeStat stat = (GameTeamMapNativeStat) entry.getKey();
        return action.equals(stat.action);
    }

    private boolean filterAllStat(Map.Entry<IStat, Long> entry) {
        final GameTeamMapNativeStat stat = (GameTeamMapNativeStat) entry.getKey();
        return gameName.equals(stat.gameName) && mapName.equals(stat.mapName) && teamName.equals(stat.teamName) && action.equals(stat.action);
    }

    /**
     * Get the stat represented by this object from the statContainer
     *
     * @param statContainer
     * @param periodKey
     * @return
     */
    @Override
    public Long getStat(StatContainer statContainer, String periodKey) {
        //if gameId is specified, it is a specific game
        if (gameId != null) {
            return getFilteredStat(statContainer, periodKey, this::filterGameIdStat);
        }
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
        //all are specified, do stat "normally"
        return getFilteredStat(statContainer, periodKey, this::filterAllStat);
    }

    @Override
    public @NotNull String getStatType() {
        return TYPE;
    }

    /**
     * Get the jsonb data in string format for this object
     *
     * @return
     */
    @Override
    public @Nullable JSONObject getJsonData() {
        return Objects.requireNonNull(super.getJsonData())
                .putOnce("action", action.name());
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

    @Override
    public boolean containsStat(IStat otherStat) {
        if (!(otherStat instanceof GameTeamMapNativeStat other)) return false;
        if (!action.equals(other.action)) return false;
        //TODO check the logic here
        if (!Strings.isNullOrEmpty(gameName) && !gameName.equals(other.gameName)) return false;
        if (!Strings.isNullOrEmpty(teamName) && !teamName.equals(other.teamName)) return false;
        if (!Strings.isNullOrEmpty(mapName) && !mapName.equals(other.mapName)) return false;
        if (gameId != null && !gameId.equals(other.gameId)) return false;
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
    public @NotNull IBuildableStat copyFromStatData(@NotNull String statType, JSONObject data) {
        GameTeamMapNativeStat other = fromData(statType, data);
        this.action = other.action;
        this.gameName = other.gameName;
        this.teamName = other.teamName;
        this.mapName = other.mapName;
        this.gameId = other.gameId;
        return this;
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
