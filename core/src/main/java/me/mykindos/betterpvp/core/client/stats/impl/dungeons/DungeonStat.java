package me.mykindos.betterpvp.core.client.stats.impl.dungeons;

import com.google.common.base.Preconditions;
import joptsimple.internal.Strings;
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
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
@Builder
@Getter
@EqualsAndHashCode
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor
public class DungeonStat implements IBuildableStat {
    public static final String PREFIX = "DUNGEON";

    private static StringBuilderParser<DungeonStatBuilder> parser = new StringBuilderParser<>(
            List.of(
                DungeonStat::parsePrefix,
                    DungeonStat::parseAction,
                    DungeonStat::parseName
            )
    );

    public static DungeonStat fromString(String string) {
        return parser.parse(DungeonStat.builder(), string).build();
    }

    private static DungeonStatBuilder parsePrefix(DungeonStatBuilder builder, String input) {
        Preconditions.checkArgument(input.equals(PREFIX));
        return builder;
    }

    private static DungeonStatBuilder parseAction(DungeonStatBuilder builder, String input) {
        return builder.action(Action.valueOf(input));
    }

    private static DungeonStatBuilder parseName(DungeonStatBuilder builder, String input) {
        return builder.dungeonName(input);
    }

    @NotNull
    private Action action;

    @Nullable
    private String dungeonName;

    /**
     * Copies the stat represented by this statName into this object
     *
     * @param statName the statname
     * @return this stat
     * @throws IllegalArgumentException if this statName does not represent this stat
     */
    @Override
    public IBuildableStat copyFromStatname(@NotNull String statName) {
        DungeonStat other = fromString(statName);
        this.action = other.action;
        this.dungeonName = other.dungeonName;
        return this;
    }

    @Override
    public String getPrefix() {
        return PREFIX;
    }

    private Double getActionStat(StatContainer statContainer, String period) {
        return statContainer.getStats().getStatsOfPeriod(period).entrySet().stream()
                .filter(entry ->
                        entry.getKey().startsWith(PREFIX + StringBuilderParser.INTRA_SEQUENCE_DELIMITER + action)
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
        if (Strings.isNullOrEmpty(dungeonName)) {
            return getActionStat(statContainer, period);
        }
        return statContainer.getProperty(period, getStatName());
    }

    @Override
    public String getStatName() {
        return parser.asString(
                List.of(
                        PREFIX,
                        action.name(),
                        dungeonName
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

    public enum Action {
        ENTER,
        WIN,
        LOSS,
        DEATH,
        //todo? dont know where to increment this from
        BOSS_KILL,
    }
}
