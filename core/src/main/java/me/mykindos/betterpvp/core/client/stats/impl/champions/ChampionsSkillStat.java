package me.mykindos.betterpvp.core.client.stats.impl.champions;

import com.google.common.base.Preconditions;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.CustomLog;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.impl.IBuildableStat;
import me.mykindos.betterpvp.core.client.stats.impl.StringBuilderParser;
import me.mykindos.betterpvp.core.skill.ISkill;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

@Builder
@Getter
@CustomLog
@ToString
@EqualsAndHashCode
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor
public class ChampionsSkillStat implements IBuildableStat {
    public static final String PREFIX = "CHAMPIONS_SKILL";
    private static StringBuilderParser<ChampionsSkillStatBuilder> parser = new StringBuilderParser<>(
            List.of(
                    ChampionsSkillStat::parsePrefix,
                    ChampionsSkillStat::parseAction,
                    ChampionsSkillStat::parseSkillName,
                    ChampionsSkillStat::parseLevel
            )
    );

    @NotNull
    private Action action;
    @Nullable
    private String skillName;
    @Builder.Default
    private int level = -1;


    public static ChampionsSkillStat fromString(String statName) {
        return parser.parse(ChampionsSkillStat.builder(), statName).build();
    }

    private static ChampionsSkillStatBuilder parsePrefix(ChampionsSkillStatBuilder builder, String input) {
        Preconditions.checkArgument(input.equals(PREFIX));
        return builder;
    }

    private static ChampionsSkillStatBuilder parseAction(ChampionsSkillStatBuilder builder, String input) {
        return builder.action(Action.valueOf(input));
    }

    private static ChampionsSkillStatBuilder parseSkillName(ChampionsSkillStatBuilder builder, String input) {
        return builder.skillName(input);
    }

    private static ChampionsSkillStatBuilder parseLevel(ChampionsSkillStatBuilder builder, String input) {
        return builder.level(Integer.parseInt(input));
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
        if (skillName == null) {
            return getActionComposite(statContainer, periodKey);
        }
        if (level == -1) {
            return getSkillComposite(statContainer, periodKey);
        }
        return statContainer.getProperty(periodKey, getStatName());
    }

    private Double getActionComposite(StatContainer statContainer, String period) {
        return statContainer.getStats().getStatsOfPeriod(period).entrySet().stream()
                .filter(entry ->
                        entry.getKey().startsWith(PREFIX + StringBuilderParser.DEFAULT_INTRA_SEQUENCE_DELIMITER + action.name())
                ).mapToDouble(Map.Entry::getValue)
                .sum();
    }

    private Double getSkillComposite(StatContainer statContainer, String period) {
        return statContainer.getStats().getStatsOfPeriod(period).entrySet().stream()
                .filter(entry ->
                        entry.getKey().startsWith(PREFIX + StringBuilderParser.DEFAULT_INTRA_SEQUENCE_DELIMITER + action.name() + StringBuilderParser.DEFAULT_INTRA_SEQUENCE_DELIMITER + skillName)
                ).mapToDouble(Map.Entry::getValue)
                .sum();
    }

    @Override
    public String getStatName() {
        return parser.asString(List.of(
                PREFIX,
                action.name(),
                //null values are allowed
                skillName,
                level == -1 ? "" : String.valueOf(level)
        ));
    }

    /**
     * Whether this stat is directly savable to the database
     *
     * @return {@code true} if it is, {@code false} otherwise
     */
    @Override
    public boolean isSavable() {
        if (action == Action.EQUIP) {
            //todo this might not be true anymore
            return skillName != null;
        }
        return level != -1 && skillName != null;
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
     * Copies the stat represented by this statName into this object
     *
     * @param statName the statname
     * @return this stat
     * @throws IllegalArgumentException if this statName does not represent this stat
     */
    @Override
    public IBuildableStat copyFromStatname(@NotNull String statName) {
        ChampionsSkillStat other = fromString(statName);
        this.action = other.action;
        this.skillName = other.skillName;
        this.level = other.level;
        return this;
    }

    @Override
    public String getPrefix() {
        return PREFIX;
    }
    
    public enum Action {
        USE,
        EQUIP,
        //todo formatter
        TIME_PLAYED,
        KILL,
        DEATH,
        ASSIST
    }

    public static ChampionsSkillStatBuilder builder() {
        return new ChampionsSkillStatBuilder();
    }

    public static class ChampionsSkillStatBuilder {
        private Action action;
        private String skillName;
        private int level = -1;

        public ChampionsSkillStatBuilder skill(ISkill skill) {
            this.skillName = skill.getName();
            return this;
        }
    }

}
