package me.mykindos.betterpvp.core.client.stats.impl.game;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
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
import me.mykindos.betterpvp.core.client.stats.impl.StringBuilderParser;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * A stat that wraps around another {@link IStat} that adds game, team, and map context
 * if the stat is in a Champions Game
 */
@SuperBuilder
@Getter
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor
public class GameTeamMapWrapperStat extends GameTeamMapStat implements IWrapperStat {

    public static final String PREFIX = "GAME_WRAPPER";

    private static final StatBuilder statBuilder = JavaPlugin.getPlugin(Core.class).getInjector().getInstance(StatBuilder.class);

    private static final StringBuilderParser<GameTeamMapWrapperStatBuilder<?, ?>> parser = new StringBuilderParser<>(
            "#",
            "##",
            List.of(
                    GameTeamMapWrapperStat::parsePrefix,
                    GameTeamMapWrapperStat::parseGameName,
                    GameTeamMapWrapperStat::parseTeamName,
                    GameTeamMapWrapperStat::parseMapName
            ),
            List.of(
                    GameTeamMapWrapperStat::parseWrappedStat
            )
    );

    /**
     * Constructs the given String into a Stat
     * @param string the stringified stat
     * @return this stat
     * @throws IllegalArgumentException if this string does not represent this Stat
     */
    public static GameTeamMapWrapperStat fromString(String string) {
        return parser.parse(GameTeamMapWrapperStat.builder(), string).build();
    }

    @NotNull
    private IStat wrappedStat;

    private static GameTeamMapWrapperStatBuilder<?, ?> parsePrefix(GameTeamMapWrapperStatBuilder<?, ?> builder, String input) {
        Preconditions.checkArgument(input.equals(PREFIX));
        return builder;
    }
    private static GameTeamMapWrapperStatBuilder<?, ?> parseWrappedStat(GameTeamMapWrapperStatBuilder<?, ?> builder, String input) {
        final IStat wrappedStat = statBuilder.getStatForStatName(input);
        if (wrappedStat instanceof GameTeamMapStat) throw new IllegalArgumentException("Wrapped stat cannot also be a GameTeamMapStat");
        return builder.wrappedStat(wrappedStat);
    }

    private static GameTeamMapWrapperStatBuilder<?, ?> parseGameName(GameTeamMapWrapperStatBuilder<?, ?> builder, String input) {
        return builder.gameName(input);
    }

    private static GameTeamMapWrapperStatBuilder<?, ?> parseMapName(GameTeamMapWrapperStatBuilder<?, ?> builder, String input) {
        return builder.mapName(input);
    }

    private static GameTeamMapWrapperStatBuilder<?, ?> parseTeamName(GameTeamMapWrapperStatBuilder<?, ?> builder, String input) {
        return builder.teamName(input);
    }

    private boolean filterGameFullStat(Map.Entry<IStat, Double> entry) {
        final GameTeamMapWrapperStat stat = (GameTeamMapWrapperStat) entry.getKey();
        return gameName.equals(stat.gameName) && wrappedStat.containsStat(stat);
    }

    private boolean filterGameTeamStat(Map.Entry<IStat, Double> entry) {
        final GameTeamMapWrapperStat stat = (GameTeamMapWrapperStat) entry.getKey();
        return wrappedStat.containsStat(stat) && gameName.equals(stat.gameName) && teamName.equals(stat.teamName);
    }

    private boolean filterGameMapStat(Map.Entry<IStat, Double> entry) {
        final GameTeamMapWrapperStat stat = (GameTeamMapWrapperStat) entry.getKey();
        return wrappedStat.containsStat(stat) && gameName.equals(stat.gameName) && mapName.equals(stat.mapName);
    }

    private boolean filterTeamOnlyStat(Map.Entry<IStat, Double> entry) {
        final GameTeamMapWrapperStat stat = (GameTeamMapWrapperStat) entry.getKey();
        return teamName.equals(stat.teamName) && wrappedStat.containsStat(stat);
    }

    private boolean filterWrapperOnlyStat(Map.Entry<IStat, Double> entry) {
        final GameTeamMapWrapperStat stat = (GameTeamMapWrapperStat) entry.getKey();
        return wrappedStat.containsStat(stat);
    }

    private boolean filterAllStat(Map.Entry<IStat, Double> entry) {
        final GameTeamMapWrapperStat stat = (GameTeamMapWrapperStat) entry.getKey();
        return gameName.equals(stat.gameName) && mapName.equals(stat.mapName) && teamName.equals(stat.teamName) && wrappedStat.containsStat(stat);
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
    public Double getStat(StatContainer statContainer, String periodKey) {
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
    public String getStatName() {
        return parser.asString(
                List.of(
                        PREFIX,
                        gameName,
                        teamName,
                        mapName
                ),
                List.of(
                        wrappedStat.getStatName()
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
        return !Strings.isNullOrEmpty(gameName) &&
                !Strings.isNullOrEmpty(teamName) &&
                !Strings.isNullOrEmpty(mapName) &&
                wrappedStat.isSavable();
    }

    /**
     * Whether this stat contains this statName
     *
     * @param statName
     * @return
     */
    @Override
    //TODO, this might not be true anymore
    public boolean containsStat(final String statName) {
        try {
            final GameTeamMapWrapperStat other = fromString(statName);
            //all filled fields must equal all the other filled fields
            //TODO check the logic here
            if (!Strings.isNullOrEmpty(gameName) && !gameName.equals(other.gameName)) return false;
            if (!Strings.isNullOrEmpty(teamName) && !teamName.equals(other.teamName)) return false;
            if (!Strings.isNullOrEmpty(mapName) && !mapName.equals(other.mapName)) return false;

            return wrappedStat.containsStat(other.wrappedStat);
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    @Override
    public boolean containsStat(final IStat otherStat) {
        if (!(otherStat instanceof GameTeamMapWrapperStat other)) return false;
        if (!Strings.isNullOrEmpty(gameName) && !gameName.equals(other.gameName)) return false;
        if (!Strings.isNullOrEmpty(teamName) && !teamName.equals(other.teamName)) return false;
        if (!Strings.isNullOrEmpty(mapName) && !mapName.equals(other.mapName)) return false;

        return wrappedStat.containsStat(other.wrappedStat);
    }

    @Override
    public @NotNull IBuildableStat copyFromStatname(@NotNull String statName) {
        final GameTeamMapWrapperStat other = fromString(statName);
        this.gameName = other.gameName;
        this.teamName = other.teamName;
        this.mapName = other.mapName;
        this.wrappedStat = other.wrappedStat;
        return this;
    }

    @Override
    public String getPrefix() {
        return PREFIX;
    }

}
