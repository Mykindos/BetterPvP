package me.mykindos.betterpvp.core.client.stats.impl.dungeons;

import com.google.common.base.Preconditions;
import joptsimple.internal.Strings;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
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
public class DungeonNativeStat extends DungeonStat implements IBuildableStat {
    public static final String PREFIX = "DUNGEON_NATIVE";

    private static StringBuilderParser<DungeonNativeStatBuilder<?, ?>> parser = new StringBuilderParser<>(
            "#",
            "##",
            List.of(
                    DungeonNativeStat::parsePrefix,
                    DungeonNativeStat::parseName
            ),
            List.of(
                    DungeonNativeStat::parseAction
            )
    );

    public static DungeonNativeStat fromString(String string) {
        return parser.parse(DungeonNativeStat.builder(), string).build();
    }

    private static DungeonNativeStatBuilder<?, ?> parsePrefix(DungeonNativeStatBuilder<?, ?> builder, String input) {
        Preconditions.checkArgument(input.equals(PREFIX));
        return builder;
    }

    private static DungeonNativeStatBuilder<?, ?> parseName(DungeonNativeStatBuilder<?, ?> builder, String input) {
        return builder.dungeonName(input);
    }

    private static DungeonNativeStatBuilder<?, ?> parseAction(DungeonNativeStatBuilder<?, ?> builder, String input) {
        return builder.action(Action.valueOf(input));
    }

    @NotNull
    private Action action;

    /**
     * Copies the stat represented by this statName into this object
     *
     * @param statName the statname
     * @return this stat
     * @throws IllegalArgumentException if this statName does not represent this stat
     */
    @Override
    public @NotNull IBuildableStat copyFromStatname(@NotNull String statName) {
        DungeonNativeStat other = fromString(statName);
        this.action = other.action;
        this.dungeonName = other.dungeonName;
        return this;
    }

    @Override
    public String getPrefix() {
        return PREFIX;
    }

    private boolean filterActionStat(Map.Entry<IStat, Double> entry) {
        final DungeonNativeStat stat = (DungeonNativeStat) entry.getKey();
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
        if (Strings.isNullOrEmpty(dungeonName)) {
            return this.getFilteredStat(statContainer, periodKey, this::filterActionStat);
        }
        return statContainer.getProperty(periodKey, this);
    }

    @Override
    public String getStatName() {
        return parser.asString(
                List.of(
                        PREFIX,
                        dungeonName
                ),
                List.of(
                        action.name()
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
    public boolean isSavable() {
        return !Strings.isNullOrEmpty(dungeonName);
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

    /**
     * Whether this stat contains this otherSTat
     *
     * @param otherStat
     * @return
     */
    @Override
    public boolean containsStat(IStat otherStat) {
        if (!(otherStat instanceof DungeonNativeStat other)) return false;
        if (!Strings.isNullOrEmpty(dungeonName) && !dungeonName.equals(other.dungeonName)) return false;
        return action.equals(other.action);
    }

    /**
     * <p>Get the generic stat that includes this stat.</p>
     * <p>{@link IStat#containsStat(IStat)} of the generic should be {@code true} for this stat</p>
     *
     * @return the generic stat
     */
    @Override
    public @NotNull IStat getGenericStat() {
        return DungeonNativeStat.builder().action(action).build();
    }

    public enum Action {
        ENTER,
        WIN,
        LOSS,
        BOSS_KILL,
    }
}
