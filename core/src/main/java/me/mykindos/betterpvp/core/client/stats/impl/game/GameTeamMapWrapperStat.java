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
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.stats.StatBuilder;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.impl.IBuildableStat;
import me.mykindos.betterpvp.core.client.stats.impl.IStat;
import me.mykindos.betterpvp.core.client.stats.impl.IWrapperStat;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.util.Map;
import java.util.Objects;

/**
 * A stat that wraps around another {@link IStat} that adds game, team, and map context
 * if the stat is in a Champions Game
 */
@SuperBuilder
@Getter
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor
@CustomLog
public class GameTeamMapWrapperStat extends GameTeamMapStat implements IWrapperStat {

    public static final String TYPE = "GAME_WRAPPER";

    private static final StatBuilder statBuilder = JavaPlugin.getPlugin(Core.class).getInjector().getInstance(StatBuilder.class);

    public static GameTeamMapWrapperStat fromData(String type, JSONObject object) {
        GameTeamMapWrapperStat.GameTeamMapWrapperStatBuilder<?, ?> builder = builder();
        if (!Strings.isNullOrEmpty(type)) {
            type = object.getString("type");
        }
        Preconditions.checkArgument(type.equals(TYPE));
        builder.gameId(object.getLong("gameId"));
        builder.gameName(object.getString("gameName"));
        builder.mapName(object.getString("mapName"));
        builder.teamName(object.getString("teamName"));
        JSONObject wrappedData = object.getJSONObject("wrappedStat");
        builder.wrappedStat(statBuilder.getStatForStatData(wrappedData.getString("statType"), wrappedData));
        return builder.build();
    }

    @NotNull
    private IStat wrappedStat;

    private boolean filterGameIdStat(Map.Entry<IStat, Long> entry) {
        final GameTeamMapWrapperStat stat = (GameTeamMapWrapperStat) entry.getKey();
        return Objects.requireNonNull(gameId).equals(stat.gameId) && wrappedStat.containsStat(stat.wrappedStat);
    }

    private boolean filterGameFullStat(Map.Entry<IStat, Long> entry) {
        final GameTeamMapWrapperStat stat = (GameTeamMapWrapperStat) entry.getKey();
        return gameName.equals(stat.gameName) && wrappedStat.containsStat(stat.wrappedStat);
    }

    private boolean filterGameTeamStat(Map.Entry<IStat, Long> entry) {
        final GameTeamMapWrapperStat stat = (GameTeamMapWrapperStat) entry.getKey();
        return wrappedStat.containsStat(stat) && gameName.equals(stat.gameName) && teamName.equals(stat.teamName);
    }

    private boolean filterGameMapStat(Map.Entry<IStat, Long> entry) {
        final GameTeamMapWrapperStat stat = (GameTeamMapWrapperStat) entry.getKey();
        return wrappedStat.containsStat(stat.wrappedStat) && gameName.equals(stat.gameName) && mapName.equals(stat.mapName);
    }

    private boolean filterTeamOnlyStat(Map.Entry<IStat, Long> entry) {
        final GameTeamMapWrapperStat stat = (GameTeamMapWrapperStat) entry.getKey();
        return teamName.equals(stat.teamName) && wrappedStat.containsStat(stat.wrappedStat);
    }

    private boolean filterWrapperOnlyStat(Map.Entry<IStat, Long> entry) {
        final GameTeamMapWrapperStat stat = (GameTeamMapWrapperStat) entry.getKey();
        return wrappedStat.containsStat(stat.wrappedStat);
    }

    private boolean filterAllStat(Map.Entry<IStat, Long> entry) {
        final GameTeamMapWrapperStat stat = (GameTeamMapWrapperStat) entry.getKey();
        return gameName.equals(stat.gameName) && mapName.equals(stat.mapName) && teamName.equals(stat.teamName) && wrappedStat.containsStat(stat.wrappedStat);
    }

    public boolean wrappedStatContainsOther(IStat other) {
        return wrappedStat.containsStat(other);
    }

    //todo figure out wtf we do with this
    //todo what happens if the wrapped stat is not savable?
    /**
     * Get the stat represented by this object from the statContainer
     *
     * @param statContainer
     * @param periodKey
     * @return
     */
    @Override
    public Long getStat(StatContainer statContainer, String periodKey) {
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
            return getFilteredStat(statContainer, periodKey, this::filterWrapperOnlyStat);
        }
        //all fields are filled, action, game, team, map
        //we still need to filter, because the sub stats could be generic
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
                .put("wrappedStat", (wrappedStat.getJsonData() == null ? new JSONObject() : wrappedStat.getJsonData())
                        .putOnce("statType", wrappedStat.getStatType())
                );
    }

    @Override
    public boolean containsStat(final IStat otherStat) {
        if (!(otherStat instanceof GameTeamMapWrapperStat other)) return false;
        if (!Strings.isNullOrEmpty(gameName) && !gameName.equals(other.gameName)) return false;
        if (!Strings.isNullOrEmpty(teamName) && !teamName.equals(other.teamName)) return false;
        if (!Strings.isNullOrEmpty(mapName) && !mapName.equals(other.mapName)) return false;

        return wrappedStat.containsStat(other.wrappedStat);
    }

    /**
     * <p>Get the generic stat that includes this stat.</p>
     * <p>{@link IStat#containsStat(IStat)} of the generic should be {@code true} for this stat</p>
     *
     * @return the generic stat
     */
    @Override
    public @NotNull IStat getGenericStat() {
        return GameTeamMapWrapperStat.builder().wrappedStat(wrappedStat.getGenericStat()).build();
    }

    @Override
    public @NotNull IBuildableStat copyFromStatData(@NotNull String statType, JSONObject data) {
        final GameTeamMapWrapperStat other = fromData(statType, data);
        this.gameName = other.gameName;
        this.teamName = other.teamName;
        this.mapName = other.mapName;
        this.gameId = other.gameId;
        this.wrappedStat = other.wrappedStat;
        return this;
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
        return UtilFormat.cleanString(wrappedStat.getSimpleName());
    }

}
