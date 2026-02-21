package me.mykindos.betterpvp.core.client.stats.impl.champions;

import com.google.common.base.Preconditions;
import joptsimple.internal.Strings;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.CustomLog;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.StatFilterType;
import me.mykindos.betterpvp.core.client.stats.impl.IBuildableStat;
import me.mykindos.betterpvp.core.client.stats.impl.IStat;
import me.mykindos.betterpvp.core.client.stats.impl.utility.StatValueType;
import me.mykindos.betterpvp.core.server.Period;
import me.mykindos.betterpvp.core.skill.ISkill;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.util.Map;
import java.util.Objects;

@Builder
@Getter
@CustomLog
@ToString
@EqualsAndHashCode
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor
public class ChampionsSkillStat implements IBuildableStat {
    public static final String TYPE = "CHAMPIONS_SKILL";

    @NotNull
    private Action action;
    @Nullable
    private String skillName;
    @Builder.Default
    private int level = -1;


    public static ChampionsSkillStat fromData(String statType, JSONObject data) {
        ChampionsSkillStat.ChampionsSkillStatBuilder builder = builder();
        Preconditions.checkArgument(statType.equals(TYPE));
        builder.action(Action.valueOf(data.getString("action")));
        builder.skillName(data.optString("skillName"));
        builder.level(data.getInt("level"));

        return builder.build();
    }


    @Override
    public Long getStat(StatContainer statContainer, StatFilterType type, @Nullable Period object) {
        if (skillName == null) {
            return getFilteredStat(statContainer, type, object, this::filterAction);
        }
        if (level == -1) {
            return getFilteredStat(statContainer, type, object, this::filterSkill);
        }
        return statContainer.getProperty(type, object, this);
    }

    /**
     * What type of stat this is, a LONG (default), DOUBLE, OR DURATION
     *
     * @return the type of stat
     */
    @Override
    public @NotNull StatValueType getStatValueType() {
        return action == Action.TIME_PLAYED ? StatValueType.DURATION : StatValueType.LONG;
    }

    private boolean filterAction(Map.Entry<IStat, Long> entry) {
        ChampionsSkillStat other = (ChampionsSkillStat) entry.getKey();
        return action.equals(other.action);
    }
    private boolean filterSkill(Map.Entry<IStat, Long> entry) {
        ChampionsSkillStat other = (ChampionsSkillStat) entry.getKey();
        return action.equals(other.action) && Objects.requireNonNull(skillName).equalsIgnoreCase(other.skillName);
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
        return new JSONObject()
                .put("action", action.name())
                .putOpt("skillName", skillName)
                .put("level", level);
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
        final StringBuilder stringBuilder = new StringBuilder(UtilFormat.cleanString(action.name()));
        if (!Strings.isNullOrEmpty(skillName)) {
            stringBuilder.append(" ")
                    .append(skillName);
        }
        if (level != -1) {
            stringBuilder.append(" ")
                    .append(level);
        }

        return stringBuilder.toString();
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
        return IBuildableStat.super.getQualifiedName();
    }

    /**
     * Whether this stat is directly savable to the database
     *
     * @return {@code true} if it is, {@code false} otherwise
     */
    @Override
    public boolean isSavable() {
        if (action == Action.EQUIP) {
            return skillName != null;
        }
        return level != -1 && skillName != null;
    }

    /**
     * Whether this stat contains this otherSTat
     *
     * @param otherStat
     * @return
     */
    @Override
    public boolean containsStat(IStat otherStat) {
        if (!(otherStat instanceof ChampionsSkillStat other)) return false;
        if (action != other.action) return false;
        if (!Strings.isNullOrEmpty(skillName) && !skillName.equals(other.skillName)) return false;
        if (level != -1 && level != other.level) return false;
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
        return ChampionsSkillStat.builder()
                .action(action)
                .skillName(skillName)
                .build();
    }

    /**
     * Copies the stat represented by this statName into this object
     *
     * @param statType the statname
     * @param data
     * @return this stat
     * @throws IllegalArgumentException if this statName does not represent this stat
     */
    @Override
    public @NotNull IBuildableStat copyFromStatData(@NotNull String statType, JSONObject data) {
        ChampionsSkillStat other = fromData(statType, data);
        this.action = other.action;
        this.skillName = other.skillName;
        this.level = other.level;
        return this;
    }
    
    public enum Action {
        USE,
        EQUIP,
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
