package me.mykindos.betterpvp.core.client.stats.impl;

import com.google.common.base.Preconditions;
import lombok.Builder;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.components.champions.IChampionsSkill;
import me.mykindos.betterpvp.core.framework.manager.Manager;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

@Builder
public class ChampionsSkillStat implements IStat {
    public static String PREFIX = "CHAMPIONS_SKILL_";
    public static String ACTION_SEPARATOR_SUFFIX = "_";
    public static String LEVEL_SEPARATOR = "__";

    @NotNull
    private Action action;
    @Nullable
    private IChampionsSkill skill;
    @Builder.Default
    private int level = -1;


    public static ChampionsSkillStat fromString(String statName, Manager<IChampionsSkill> manager) {
        Preconditions.checkArgument(statName.startsWith(PREFIX), "statName must start with " + PREFIX);
        final ChampionsSkillStatBuilder builder = ChampionsSkillStat.builder();




        final int extraIndex = statName.lastIndexOf(LEVEL_SEPARATOR);
        final String typeName = statName.substring(prefix.length(), extraIndex != -1 ? extraIndex : statName.length());

        final Statistic stat = Statistic.valueOf(typeName);

        builder.statistic(stat);

        if (isMaterialStatistic(stat)) {

            final Material mat = Material.getMaterial(statName.substring(prefix.length() + typeName.length() + qualifierSeparator.length()));
            if (mat != null) {
                builder.material(mat);
            }
        }

        if (isEntityStatistic(stat)) {
            final EntityType ent = EntityType.fromName(statName.substring(prefix.length() + typeName.length() + qualifierSeparator.length()));
            if (ent != null) {
                builder.entityType(ent);
            }

        }

        return builder.build();
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
        if (skill == null) {
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
        statContainer.getStats().getStatsOfPeriod(period).entrySet().stream()
                .filter(entry ->
                        entry.getKey().startsWith(PREFIX + action.name() + ACTION_SEPARATOR_SUFFIX + transformSkillName())
                ).mapToDouble(Map.Entry::getValue)
                .sum();
    }

    @Override
    public String getStatName() {
        StringBuilder builder = new StringBuilder(PREFIX);
        builder.append(action);
        if (skill == null) {
            return builder.toString();
        }
        builder.append("_")
                .append(transformSkillName());
        if (level == -1) {
            return builder.toString();
        }
        builder.append(LEVEL_SEPARATOR);
        builder.append(level);

        return builder.toString();
    }

    /**
     * Whether this stat is directly savable to the database
     *
     * @return {@code true} if it is, {@code false} otherwise
     */
    @Override
    public boolean isSavable() {
        return level != -1 && skill != null;
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
        if (skill == null) return "";
        return skill.getName().replace(' ', '_').toUpperCase();
    }

    public enum Action {
        USE,
        EQUIP
    }
}
