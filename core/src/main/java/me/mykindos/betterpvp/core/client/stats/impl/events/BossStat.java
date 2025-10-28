package me.mykindos.betterpvp.core.client.stats.impl.events;

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
import me.mykindos.betterpvp.core.client.stats.impl.IStat;
import me.mykindos.betterpvp.core.client.stats.impl.StringBuilderParser;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

@Builder
@Getter
@EqualsAndHashCode
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor
public class BossStat implements IBuildableStat {
    public static final String PREFIX = "EVENT_BOSS";

    private static StringBuilderParser<BossStatBuilder> parser = new StringBuilderParser<>(
            List.of(
                    BossStat::parsePrefix,
                    BossStat::parseAction,
                    BossStat::parseName
            )
    );

    public static BossStat fromString(String string) {
        return parser.parse(BossStat.builder(), string).build();
    }

    private static BossStatBuilder parsePrefix(BossStatBuilder builder, String input) {
        Preconditions.checkArgument(input.equals(PREFIX));
        return builder;
    }

    private static BossStatBuilder parseAction(BossStatBuilder builder, String input) {
        return builder.action(Action.valueOf(input));
    }

    private static BossStatBuilder parseName(BossStatBuilder builder, String input) {
        return builder.bossName(input);
    }

    @NotNull
    private Action action;

    @Nullable
    private String bossName;

    /**
     * Copies the stat represented by this statName into this object
     *
     * @param statName the statname
     * @return this stat
     * @throws IllegalArgumentException if this statName does not represent this stat
     */
    @Override
    public @NotNull IBuildableStat copyFromStatname(@NotNull String statName) {
        BossStat other = fromString(statName);
        this.action = other.action;
        this.bossName = other.bossName;
        return this;
    }

    @Override
    public String getPrefix() {
        return PREFIX;
    }

    private Double getActionStat(StatContainer statContainer, String period) {
        return statContainer.getStats().getStatsOfPeriod(period).entrySet().stream()
                .filter(entry ->
                        entry.getKey().getStatName().startsWith(PREFIX + StringBuilderParser.DEFAULT_INTRA_SEQUENCE_DELIMITER + action)
                ).mapToDouble(Map.Entry::getValue)
                .sum();
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
        if (Strings.isNullOrEmpty(bossName)) {
            return getActionStat(statContainer, periodKey);
        }
        return statContainer.getProperty(periodKey, this);
    }

    @Override
    public String getStatName() {
        return parser.asString(
                List.of(
                        PREFIX,
                        action.name(),
                        bossName
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
     * Get the qualified name of the stat, if one exists.
     * Should usually end with the {@link IStat#getSimpleName()}
     * <p>
     * i.e. Domination Time Played, Capture the Flag CTF_Oakvale Flags Captured
     *
     * @return the qualified name
     */
    @Override
    public String getQualifiedName() {
        StringBuilder stringBuilder = new StringBuilder();
        if (!com.google.common.base.Strings.isNullOrEmpty(bossName)) {
            stringBuilder.append(bossName);
            stringBuilder.append(" ");
        }
        return stringBuilder.append(getSimpleName()).toString();
    }

    /**
     * Whether this stat is directly savable to the database
     *
     * @return {@code true} if it is, {@code false} otherwise
     */
    @Override
    public boolean isSavable() {
        return !Strings.isNullOrEmpty(bossName);
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
    public boolean containsStat(IStat otherStat) {
        if (!(otherStat instanceof BossStat other)) return false;
        if (!action.equals(other.action)) return false;
        if (!Strings.isNullOrEmpty(bossName) && !bossName.equals(other.bossName)) return false;
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
        return BossStat.builder().action(action).build();
    }

    public enum Action {
        KILL,
    }
}
