package me.mykindos.betterpvp.core.client.stats.impl;

import com.google.common.base.Preconditions;
import lombok.Builder;
import lombok.Getter;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.skill.ISkill;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Map;

@Builder
@Getter
public class ChampionsSkillStat implements IStat {
    public static String PREFIX = "CHAMPIONS_SKILL_";
    public static String ACTION_SEPARATOR_SUFFIX = "_";
    public static String LEVEL_SEPARATOR = "__";

    @NotNull
    private Action action;
    @Nullable
    private String skillName;
    @Builder.Default
    private int level = -1;


    public static ChampionsSkillStat fromString(String statName) {
        Preconditions.checkArgument(statName.startsWith(PREFIX), "statName must start with " + PREFIX);
        final ChampionsSkillStatBuilder builder = ChampionsSkillStat.builder();

        final int endOfAction = statName.indexOf(ACTION_SEPARATOR_SUFFIX, PREFIX.length());

        if (endOfAction == -1) {
            return builder
                    .action(Action.valueOf(statName.substring(PREFIX.length())))
                    .build();
        }

        final Action action = Action.valueOf(statName.substring(PREFIX.length(), endOfAction));
        builder.action(action);

        final int extraIndex = statName.lastIndexOf(LEVEL_SEPARATOR);
        final String skillName = statName.substring(endOfAction, extraIndex != -1 ? extraIndex : statName.length());

        builder.skillName(getNormalName(skillName));
        if (extraIndex == -1) {
            return builder.build();
        }

        final int level = Integer.parseInt(statName.substring(extraIndex + LEVEL_SEPARATOR.length()));

        return builder
                .level(level)
                .build();
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
        if (skillName == null) {
            return getActionComposite(statContainer, period);
        }
        if (level == -1) {
            return getSkillComposite(statContainer, period);
        }
        return statContainer.getProperty(period, getStatName());
    }

    private Double getActionComposite(StatContainer statContainer, String period) {
        return statContainer.getStats().getStatsOfPeriod(period).entrySet().stream()
                .filter(entry ->
                        entry.getKey().startsWith(PREFIX + action.name())
                ).mapToDouble(Map.Entry::getValue)
                .sum();
    }

    private Double getSkillComposite(StatContainer statContainer, String period) {
        return statContainer.getStats().getStatsOfPeriod(period).entrySet().stream()
                .filter(entry ->
                        entry.getKey().startsWith(PREFIX + action.name() + ACTION_SEPARATOR_SUFFIX + transformSkillName())
                ).mapToDouble(Map.Entry::getValue)
                .sum();
    }

    public String getBaseStat() {
        StringBuilder builder = new StringBuilder(PREFIX);
        builder.append(action);
        //CHAMPIONS_SKILL_ACTION
        if (skillName == null) {
            return builder.toString();
        }
        builder.append(ACTION_SEPARATOR_SUFFIX)
                .append(transformSkillName());
        //CHAMPIONS_SKILL_ACTION_SKILL_NAME
        return builder.toString();
    }

    @Override
    public String getStatName() {
        StringBuilder builder = new StringBuilder(PREFIX);
        builder.append(action);
        //CHAMPIONS_SKILL_ACTION
        if (skillName == null) {
            return builder.toString();
        }
        builder.append(ACTION_SEPARATOR_SUFFIX)
                .append(transformSkillName());
        //CHAMPIONS_SKILL_ACTION_SKILL_NAME
        if (level == -1) {
            return builder.toString();
        }
        builder.append(LEVEL_SEPARATOR);
        builder.append(level);

        //CHAMPIONS_SKILL_ACTION_SKILL_NAME__LEVEL
        return builder.toString();
    }

    /**
     * Whether this stat is directly savable to the database
     *
     * @return {@code true} if it is, {@code false} otherwise
     */
    @Override
    public boolean isSavable() {
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
        return getStatName().equals(statName);
    }

    private String transformSkillName() {
        //Skill Name -> SKILL_NAME
        if (skillName == null) return "";
        return skillName.replace(' ', '_').toUpperCase();
    }


    private static String getNormalName(String transformedNamed) {
        //SKILL_NAME -> Skill Name
        return String.join(" ",Arrays.stream(transformedNamed.toLowerCase().replace("_", " ").split(" "))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
                .toList());
    }

    public enum Action {
        USE,
        EQUIP
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
